(ns alda.parser.parse-events
  (:require [clojure.core.async       :refer (>!!)]
            [clojure.string           :as    str]
            [alda.lisp.events         :as    event]
            [alda.lisp.model.duration :as    dur]
            [alda.lisp.model.pitch    :as    pitch]
            [alda.parser.tokenize     :refer (token-names)]))

(defn initial-parser-state
  []
  {:state :parsing
   :stack []})

(defn parser
  [events-ch]
  (-> (initial-parser-state)
      (assoc :events-ch events-ch)))

(defn current-event
  [{:keys [stack] :as parser}]
  (-> stack peek))

(defn current-event-type
  [{:keys [stack] :as parser}]
  (-> stack peek :type))

(defn current-event-content
  [{:keys [stack] :as parser}]
  (-> stack peek :content))

(defn previous-event-type
  [{:keys [stack] :as parser}]
  (when-not (empty? stack)
    (-> stack pop peek :type)))

(defn previous-event-content
  [{:keys [stack] :as parser}]
  (when-not (empty? stack)
    (-> stack pop peek :content)))

(defn token-is
  [token-type token]
  (and (sequential? token) (= token-type (first token))))

(defn token-type
  [token]
  (when (sequential? token) (first token)))

(defn token-position
  [[_ [line column] _ :as token]]
  [line column])

(defn token-content
  [[_ _ content :as token]]
  content)

(defn validate-variable-event
  [var-name [line column]]
  (if (re-find #"\+|\-" (name var-name))
    (throw (Exception.
             (format (str "Invalid variable name '%s' at line %s, column %s: "
                          "a variable name may not contain '+' or '-'.")
                     (name var-name)
                     line
                     column)))))

(declare alda-event-with-metadata)

(defmulti alda-event :type)

(defmethod alda-event :default
  [{:keys [type] :as event}]
  (throw (Exception. (format "Unrecognized event: %s" type))))

(defmethod alda-event :at-marker
  [{:keys [content]}]
  (event/at-marker content))

(defmethod alda-event :barline
  [{:keys [content]}]
  (event/barline))

;; NB: I don't think we can include the position in the resulting object,
;; because we're evaluating an arbitrary clojure expression and the result might
;; be nil or some other object that doesn't implement IMeta.
(defmethod alda-event :clj-expr
  [{:keys [content]}]
  (require '[alda.lisp :refer :all])
  (let [value (load-string content)]
    (if (nil? value)
      :nil ; can't put nil on a channel
      value)))

(defmethod alda-event :cram
  [{:keys [content]}]
  (apply event/cram (map alda-event-with-metadata content)))

(defmethod alda-event :duration
  [{:keys [content]}]
  (-> (apply dur/duration (for [{:keys [type] :as event} content
                                :when (not= :tie type)]
                            (alda-event-with-metadata event)))
      (merge (when (= :tie (:type (last content)))
               {:slur? true}))))

(defmethod alda-event :event-seq
  [{:keys [content]}]
  (mapv alda-event-with-metadata content))

(defmethod alda-event :get-variable
  [{:keys [content position] :as event}]
  (validate-variable-event content position)
  (event/get-variable (keyword content)))

(defmethod alda-event :instrument-call
  [{:keys [content]}]
  (let [nickname-error (Exception. (str "Can't have more than one nickname in "
                                        "an instrument call."))
        instruments (reduce (fn [acc {:keys [type content]}]
                              (case type
                                :name (update acc :names (fnil conj []) content)
                                :nickname (if (:nickname acc)
                                            (throw nickname-error)
                                            (assoc acc :nickname content))))
                            {}
                            content)]
    (event/part instruments)))

(defmethod alda-event :marker
  [{:keys [content]}]
  (event/marker content))

(defmethod alda-event :note
  [{:keys [content chord?] :as event}]
  (let [[letter & more] content
        accidentals    (-> (for [{:keys [type content]} more
                                 :when (= :accidentals type)]
                             (map {\+ :sharp \- :flat \_ :natural} content))
                           flatten)
        pitch          (apply pitch/pitch (keyword (:content letter)) accidentals)
        duration       (-> (for [{:keys [type] :as event} more
                                 :when (= :duration type)]
                             (alda-event-with-metadata event))
                           first) ; there should be at most one
        slur           (when (or (some #(= :tie (:type %)) more)
                                 (:slur? duration))
                         :slur)]
    (-> (event/note pitch duration slur)
        (merge (when chord? {:chord? true})))))

(defmethod alda-event :note-length
  [{:keys [content]}]
  (let [[_ number _ dots]  (re-matches #"(\d+(\.\d+)?)(\.*)" content)
        [_ seconds]      (re-matches #"(\d+)s" content)
        [_ milliseconds] (re-matches #"(\d+)ms" content)]
    (cond
      number
      (dur/note-length (Float/parseFloat number) {:dots (count dots)})

      seconds
      (dur/ms (* 1000 (Integer/parseInt seconds)))

      milliseconds
      (dur/ms (Integer/parseInt milliseconds))

      :else
      (throw (Exception. (format "Invalid note length: %s" content))))))

(defmethod alda-event :octave-change
  [{:keys [content]}]
  (let [value (cond
                (str/starts-with? content "o")
                (Integer/parseInt (subs content 1))

                (= "<" content)
                :down

                (= ">" content)
                :up)]
    (event/set-attribute :octave value)))

(defmethod alda-event :repeat
  [{:keys [content]}]
  (let [[times event] content]
    (vec (repeat times (alda-event-with-metadata event)))))

(defmethod alda-event :rest
  [{:keys [content chord?] :as event}]
  (let [duration (when (seq content)
                   (alda-event-with-metadata (first content)))]
    (-> (event/pause duration)
        (merge (when chord? {:chord? true})))))

(defmethod alda-event :set-variable
  [{:keys [content position] :as event}]
  (let [[var-name events] content]
    (validate-variable-event var-name position)
    (apply event/set-variable
           (keyword var-name)
           (map alda-event-with-metadata events))))

(defmethod alda-event :voice
  [{:keys [content] :as event}]
  (let [[_ vn]       (re-matches #"V(\d+)" content)
        voice-number (Integer/parseInt vn)]
    (event/voice voice-number)))

(defn alda-event-with-metadata
  [{:keys [position] :as event}]
  (let [event    (alda-event event)
        metadata (when (map? event)
                   (select-keys event [:position]))]
    (if (and (instance? clojure.lang.IObj event)
             (instance? clojure.lang.IMeta event))
      (with-meta event metadata)
      event)))

(defn emit!
  [{:keys [events-ch] :as parser} x]
  (>!! events-ch x)
  parser)

(defn emit-event!
  [parser event]
  (-> parser (emit! (alda-event-with-metadata event))))

(defn emit-error!
  [parser e-or-msg]
  (let [error (if (instance? Throwable e-or-msg)
                e-or-msg
                (Exception. e-or-msg))]
    (-> parser (emit! error) (assoc :state :error))))

(defn pop-and-emit-event!
  [{:keys [stack] :as parser}]
  (if-let [event (peek stack)]
    (-> parser (emit-event! event) (update :stack pop))
    parser))

(defn append-to-parent
  "Given a stack like [[:event-seq] [:note ...]],

   if no `value` arg is provided, appends the top of the stack to the event
   below: [[:event-seq [:note ...]]]

   if a `value` arg is provided, pops the stack and appends the custom value
   to the item below: [[:event-seq 'some-custom-value]]"
  [{:keys [stack] :as parser} & [value]]
  (let [value        (or value (peek stack))
        parent       (-> stack pop peek)
        parent+value (update parent :content conj value)]
    (-> parser (update :stack #(-> % pop pop (conj parent+value))))))

(defn append-to-current-event
  [{:keys [stack] :as parser} event]
  (let [current-event  (peek stack)
        appended-event (update current-event :content conj event)]
    (-> parser (update :stack #(-> % pop (conj appended-event))))))

(defn last-open-event
  [{:keys [stack] :as parser}]
  (->> stack
       (filter :open?)
       last))

(def repeatable?
  #{:clj-expr :note :rest :event-seq :get-variable :cram})

(defn unexpected-token-error
  [parser token]
  (let [error-msg (if (sequential? token)
                    (let [[token [line column] content] token]
                      (format "Unexpected %s at line %s, column %s."
                              (if (= :EOF token)
                                "EOF"
                                (get token-names token token))
                              line
                              column))
                    (format "Unexpected token: %s." token))]
    (-> parser (emit-error! error-msg))))

(declare push-set-variable)

(defn push-event
  [{:keys [stack] :as parser} event]
  (cond
    (empty? stack)
    (-> parser (update :stack conj event))

    (= :duration (:type event))
    (-> parser (update :stack conj event))

    (= :duration (current-event-type parser))
    (-> parser append-to-parent (push-event event))

    (= :repeat (:type event))
    (if (and (repeatable? (current-event-type parser))
             (not (:open? (current-event parser))))
      (let [repeats         (Integer/parseInt (:content event))
            event-to-repeat (peek stack)
            repeat-event    (assoc event :content [repeats event-to-repeat])]
        (-> parser
            (update :stack #(-> %
                                pop
                                (conj repeat-event)))))
      (-> parser (unexpected-token-error event)))

    ;; EOF can terminate a variable definition
    (and (= :EOF event) (= :set-variable (:type (last-open-event parser))))
    (-> parser push-set-variable (push-event event))

    ;; no other type of container can be open at the end of input
    (and (= :EOF event) (last-open-event parser))
    (-> parser (unexpected-token-error event))

    (last-open-event parser)
    (-> parser (update :stack conj event))

    :else
    (-> parser pop-and-emit-event! (push-event event))))

(defn push-event-when
  [parser token expected-token]
  (when (token-is expected-token token)
    (-> parser (push-event {:type     expected-token
                            :position (token-position token)
                            :content  (token-content token)}))))

(defn rename-current-event
  [{:keys [stack] :as parser} new-name]
  (let [current-event (-> stack peek (assoc :type new-name))]
    (-> parser (update :stack #(-> % pop (conj current-event))))))

(defn open-current-event
  [{:keys [stack] :as parser}]
  (let [current-event (-> stack peek (assoc :open? true))]
    (-> parser (update :stack #(-> % pop (conj current-event))))))

(defn push-set-variable
  [parser]
  {:pre [(= :set-variable (:type (last-open-event parser)))]}
  (let [{:keys [stack] :as parser}
        (if (= :duration (current-event-type parser))
          (-> parser append-to-parent)
          parser)

        var-events
        (->> stack
             reverse
             (take-while #(not= :set-variable (:type %)))
             reverse)

        {var-name :content}
        (->> stack
             reverse
             (drop-while #(not= :set-variable (:type %)))
             first)

        set-var-event
        {:type :set-variable
         :content [(keyword var-name) var-events]}]
    (-> parser
        (update :stack #(->> %
                             (drop-last (inc (count var-events)))
                             vec))
        (push-event set-var-event))))

(defn- push-container
  [container]
  (fn [parser]
    {:pre [(= container (:type (last-open-event parser)))]}
    (let [{:keys [stack] :as parser}
          (if (= :duration (current-event-type parser))
            (-> parser append-to-parent)
            parser)

          events
          (->> stack
               reverse
               (take-while #(not (and (= container (:type %))
                                      (:open? %))))
               reverse
               vec)]
      (-> parser
          (update :stack #(->> %
                               (drop-last (inc (count events)))
                               vec))
          (push-event {:type container :content events})))))

(def push-cram
  (push-container :cram))

(def push-event-seq
  (push-container :event-seq))

(defn push-instrument-call
  [{:keys [stack] :as parser}]
  {:pre [(= :instrument-call (:type (last-open-event parser)))]}
  (let [contents (->> stack
                      reverse
                      (take-while #(not (= :instrument-call (:type %))))
                      reverse)]
    (-> parser
        (update :stack #(->> %
                             (drop-last (inc (count contents)))
                             vec))
        (push-event {:type :instrument-call
                     :content contents}))))

(defn ensure-parsing
  "If the parser's state is not :parsing, short-circuits the parser so that the
   current state is passed through until the end.

   Otherwise returns nil so that parsing will continue."
  [{:keys [state] :as parser}]
  (when (not= :parsing state)
    parser))

(defn propagate-error
  "If there was an error in a previous stage of the parsing pipeline, propagate
   it through and stop parsing."
  [parser token]
  (when (instance? Throwable token)
    (-> parser (emit-error! token))))

(defn finish-parsing
  [{:keys [stack] :as parser} token]
  (when (token-is :EOF token)
    (-> parser (push-event :EOF) (assoc :state :done))))

(defn ignore-comment
  [parser token]
  (when (token-is :comment token)
    parser))

(declare read-token! start-parsing-note start-parsing-rest)

(defn parse-accidentals
  [parser token]
  (when (token-is :accidentals token)
    (if (= :note (current-event-type parser))
      (-> parser (append-to-current-event {:type     :accidentals
                                           :position (token-position token)
                                           :content  (token-content token)}))
      (-> parser (unexpected-token-error token)))))

(defn parse-at-marker
  [parser token]
  (-> parser (push-event-when token :at-marker)))

(defn parse-barline
  [parser token]
  (when (token-is :barline token)
    (let [barline {:type     :barline
                   :position (token-position token)}]
      (if (= :duration (current-event-type parser))
        (-> parser (append-to-current-event barline))
        (-> parser (push-event barline))))))

(defn parse-colon
  [parser token]
  (when (token-is :colon token)
    (if (= :instrument-call (:type (last-open-event parser)))
      (-> parser push-instrument-call)
      (-> parser (unexpected-token-error token)))))

(defn parse-clj-expr
  [parser token]
  (-> parser (push-event-when token :clj-expr)))

(defn parse-marker
  [parser token]
  (-> parser (push-event-when token :marker)))

(defn parse-name
  [parser token]
  (-> parser (push-event-when token :name)))

(defn parse-nickname
  [parser token]
  (when (token-is :nickname token)
    (if (= :instrument-call (:type (last-open-event parser)))
      (-> parser (push-event-when token :nickname))
      (-> parser (unexpected-token-error token)))))

(defn parse-note-length
  [parser token]
  (when (token-is :note-length token)
    (let [note-length {:type     :note-length
                       :position (token-position token)
                       :content  (token-content token)}]
      (condp contains? (current-event-type parser)
        #{:duration}
        (-> parser (append-to-current-event note-length))

        #{:note :rest :cram}
        (-> parser (push-event {:type     :duration
                                :position (token-position token)
                                :content  [note-length]}))

        ;; else
        (-> parser (unexpected-token-error token))))))

(defn parse-octave-change
  [parser token]
  (-> parser (push-event-when token :octave-change)))

(defn parse-repeat
  [{:keys [stack] :as parser} token]
  (-> parser (push-event-when token :repeat)))

(defn parse-voice
  [parser token]
  (-> parser (push-event-when token :voice)))

(defn parse-tie
  [parser token]
  (when (token-is :tie token)
    (if (#{:duration :note} (current-event-type parser))
      (-> parser (append-to-current-event {:type     :tie
                                           :position (token-position token)}))
      (-> parser (unexpected-token-error token)))))

(defn start-parsing-instrument-call
  [parser]
  (let [first-name (current-event parser)]
    (-> parser
        (update :stack pop)
        (push-event {:type :instrument-call})
        open-current-event
        (push-event first-name))))

(defn continue-parsing-instrument-call
  [parser token]
  (when (= :instrument-call (:type (last-open-event parser)))
    (cond
      (token-is :slash token)
      parser ; ignore

      (token-is :name token)
      (-> parser (push-event-when token :name))

      (token-is :nickname token)
      (-> parser (push-event-when token :nickname))

      (token-is :colon token)
      (-> parser push-instrument-call)

      :else
      (-> parser (unexpected-token-error token)))))

(defn start-parsing-chord
  [parser token]
  (when (and (token-is :slash token)
             (not= :instrument-call (last-open-event parser)))
    (-> parser (assoc :chord? true))))

(defn continue-parsing-chord
  [p t]
  (when (:chord? parser)
    (or
      (parse-octave-change p t)
      (parse-clj-expr p t)
      (start-parsing-note p t)
      (start-parsing-rest p t)
      (unexpected-token-error p t))))

(defn start-parsing-cram
  [parser token]
  (when (token-is :cram-open token)
    (-> parser (push-event {:type     :cram
                            :position (token-position token)})
               open-current-event)))

(defn finish-parsing-cram
  [parser token]
  (when (token-is :cram-close token)
    (-> parser push-cram)))

(defn start-parsing-event-seq
  [parser token]
  (when (token-is :event-seq-open token)
    (-> parser (push-event {:type     :event-seq
                            :position (token-position token)})
               open-current-event)))

(defn finish-parsing-event-seq
  [parser token]
  (when (token-is :event-seq-close token)
    (-> parser push-event-seq)))

(defn start-parsing-note
  [{:keys [chord?] :as parser} token]
  (when (token-is :note token)
    (-> parser
        (push-event (merge {:type     :note
                            :position (token-position token)
                            :content  [{:type     :pitch
                                        :position (token-position token)
                                        :content  (token-content token)}]}
                           (when chord? {:chord? true})))
        (dissoc :chord?))))

(defn start-parsing-rest
  [{:keys [chord?] :as parser} token]
  (when (token-is :rest token)
    (-> parser
        (push-event (merge {:type     :rest
                            :position (token-position token)}
                           (when chord? {:chord? true})))
        (dissoc :chord?))))

(defn start-parsing-set-variable
  [parser token]
  (when (token-is :equals token)
    (if (= :name (current-event-type parser))
      (-> parser (-> (rename-current-event :set-variable) open-current-event))
      (-> parser (unexpected-token-error token)))))

(defn handle-newline
  [parser token]
  (when (token-is :newline token)
    (if (= :set-variable (:type (last-open-event parser)))
      (-> parser push-set-variable)
      parser)))

(defn disambiguate-name
  [p t]
  (when (and (= :name (current-event-type p))
             (not= :instrument-call (:type (last-open-event p))))
    (case (token-type t)
      :equals   (start-parsing-set-variable p t)
      :slash    (start-parsing-instrument-call p)
      :nickname (-> p start-parsing-instrument-call (read-token! t))
      :colon    (-> p start-parsing-instrument-call (parse-colon t))
      (-> p
          (rename-current-event :get-variable)
          (read-token! t)))))

(defn read-token!
  "Reads one token `t` and updates parser `p`.

   Puts events on (:events-ch p) as they are parsed."
  [p t]
  (try
    (or (ensure-parsing p)
        (propagate-error p t)
        (ignore-comment p t)
        (handle-newline p t)
        (disambiguate-name p t)
        (continue-parsing-instrument-call p t)
        (continue-parsing-chord p t)
        (parse-accidentals p t)
        (parse-at-marker p t)
        (parse-barline p t)
        (parse-colon p t)
        (parse-clj-expr p t)
        (parse-marker p t)
        (parse-name p t)
        (parse-nickname p t)
        (parse-note-length p t)
        (parse-octave-change p t)
        (parse-repeat p t)
        (parse-voice p t)
        (parse-tie p t)
        (start-parsing-note p t)
        (start-parsing-rest p t)
        (start-parsing-chord p t)
        (start-parsing-cram p t)
        (start-parsing-event-seq p t)
        (finish-parsing-cram p t)
        (finish-parsing-event-seq p t)
        (finish-parsing p t)
        (unexpected-token-error p t))
    (catch Throwable e
      (emit-error! p e))))
