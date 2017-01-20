(ns alda.parser.parse-events
  (:require [clojure.core.async   :refer (>!!)]
            [alda.lisp.events     :as    evts]
            [alda.parser.tokenize :refer (token-names)]))

(defn initial-parser-state
  []
  {:state   :parsing
   :stack []
   })

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

(defn token-content
  [[_ _ content :as token]]
  content)

(defmulti emit-event! (fn [parser event] (:type event)))

(defmethod emit-event! :default
  [{:keys [events-ch] :as parser} event]
  (>!! events-ch event)
  parser)

(defmethod emit-event! :event-seq
  [parser {:keys [content] :as event}]
  (doseq [event content] (emit-event! parser event))
  parser)

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

(defn open-event?
  [event-type]
  (fn [{:keys [stack] :as parser}]
    (->> stack
         (filter (fn [{:keys [type open?]}] (and (= event-type type) open?)))
         last)))

(def open-event-seq? (open-event? :event-seq))
(def open-set-variable? (open-event? :set-variable))

(defn last-open-event
  [{:keys [stack] :as parser}]
  (->> stack
       (filter :open?)
       last))

(defn push-event
  [{:keys [stack] :as parser} {:keys [type] :as event}]
  (cond
    (last-open-event parser)
    (-> parser (update :stack conj event))

    :else
    (-> parser pop-and-emit-event! (update :stack conj event))))

(defn rename-current-event
  [{:keys [stack] :as parser} new-name]
  (let [current-event (-> stack peek (assoc :type new-name))]
    (-> parser (update :stack #(-> % pop (conj current-event))))))

(defn open-current-event
  [{:keys [stack] :as parser}]
  (let [current-event (-> stack peek (assoc :open? true))]
    (-> parser (update :stack #(-> % pop (conj current-event))))))

(defn push-set-variable
  [{:keys [stack] :as parser}]
  {:pre [(= :set-variable (:type (last-open-event parser)))]}
  (let [var-events          (->> stack
                                 reverse
                                 (take-while #(not= :set-variable (:type %)))
                                 reverse)
        {var-name :content} (->> stack
                                 reverse
                                 (drop-while #(not= :set-variable (:type %)))
                                 first)
        set-var-event       {:type :set-variable
                             :content [var-name var-events]}]
    (-> parser
        (update :stack #(->> %
                             (drop-last (inc (count var-events)))
                             vec))
        (push-event set-var-event))))

(defn push-event-seq
  [{:keys [stack] :as parser}]
  {:pre [(= :event-seq (:type (last-open-event parser)))]}
  (let [seq-events (->> stack
                        reverse
                        (take-while #(not (and (= :event-seq (:type %))
                                               (:open? %))))
                        reverse)]
    (-> parser
        (update :stack #(->> %
                             (drop-last (inc (count seq-events)))
                             vec))
        (push-event {:type :event-seq
                     :content seq-events}))))

(defn ensure-parsing
  "If the parser's state is not :parsing, short-circuits the parser so that the
   current state is passed through until the end.

   Otherwise returns nil so that parsing will continue."
  [{:keys [state] :as parser}]
  (when (not= :parsing state)
    parser))

(defn finish-parsing
  [parser character]
  (when (= :EOF character)
    (-> parser (assoc :state :done))))

(defn caught-error
  [parser e]
  (-> parser (emit-event! [:error e]) (assoc :state :error)))

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
    (-> parser (emit-event! [:error error-msg]) (assoc :state :error))))

(defn ignore-comment
  [parser token]
  (when (token-is :comment token)
    parser))

(declare parse-note-length parse-tie parse-barline read-token!)

(defn parse-accidentals
  [parser token]
  (when (token-is :accidentals token)
    (if (= :note (current-event-type parser))
      (-> parser (append-to-current-event {:type :accidentals
                                           :content (token-content token)}))
      (-> parser (unexpected-token-error token)))))

(defn parse-barline
  [parser token]
  (when (token-is :barline token)
    (if (= :note (current-event-type parser))
      (-> parser (append-to-current-event {:type :barline}))
      (-> parser (push-event {:type :barline})))))

(defn parse-clj-expr
  [parser token]
  (when (token-is :clj-expr token)
    (let [clj-expr (token-content token)]
      (-> parser (push-event {:type :clj-expr
                              :content (token-content token)})))))

(defn parse-name
  [parser token]
  (when (token-is :name token)
    (-> parser (push-event {:type :name
                            :content (token-content token)}))))

(defn parse-note-length
  [parser token]
  (when (token-is :note-length token)
    (if (#{:note :rest} (current-event-type parser))
      (-> parser (append-to-current-event {:type :note-length
                                           :content (token-content token)}))
      (-> parser (unexpected-token-error token)))))

(defn parse-octave-change
  [parser token]
  (when (token-is :octave-change token)
    (-> parser (push-event {:type :octave-change
                            :content (token-content token)}))))

(def ^:private repeatable?
  ; TODO: include all the things that can be repeated
  #{:clj-expr :note :rest :chord :event-seq :get-variable})

(defn parse-repeat
  [{:keys [stack] :as parser} token]
  (when (token-is :repeat token)
    (if (and (repeatable? (current-event-type parser))
             (not (:open? (current-event parser))))
      (let [repeats (Integer/parseInt (token-content token))
            events-to-repeat (peek stack)]
        (-> parser
            (update :stack pop)
            (push-event {:type :repeat
                         :content [repeats events-to-repeat]})))
      (-> parser (unexpected-token-error token)))))

(defn parse-tie
  [parser token]
  (when (token-is :tie token)
    (if (#{:note :rest} (current-event-type parser))
      (-> parser (append-to-current-event {:type :tie}))
      (-> parser (unexpected-token-error token)))))

(defn start-parsing-names
  [parser token]
  (when (token-is :slash token)
    (-> parser
        (rename-current-event :names))))

(defn start-parsing-note
  [parser token]
  (when (token-is :note token)
    (-> parser (push-event {:type :note
                            :content [{:type :pitch
                                       :content (token-content token)}]}))))

(defn start-parsing-rest
  [parser token]
  (when (token-is :rest token)
    (-> parser (push-event {:type :rest}))))

(defn start-parsing-set-variable
  [parser token]
  (when (token-is :equals token)
    (if (= :name (current-event-type parser))
      (-> parser (-> (rename-current-event :set-variable) open-current-event))
      (-> parser (unexpected-token-error token)))))

(defn start-parsing-event-seq
  [parser token]
  (when (token-is :event-seq-open token)
    (-> parser (push-event {:type :event-seq}) open-current-event)))

(defn finish-parsing-event-seq
  [parser token]
  (when (token-is :event-seq-close token)
    (-> parser push-event-seq)))

(defn handle-newline
  [parser token]
  (when (token-is :newline token)
    (let [open-event (last-open-event parser)]
      (if (= :set-variable (:type open-event))
        (-> parser push-set-variable)
        parser))))

(defn disambiguate-name
  [p t]
  (when (= :name (current-event-type p))
    (case (token-type t)
      :equals (start-parsing-set-variable p t)
      :slash  (start-parsing-names p t)
      (-> p
          (rename-current-event :get-variable)
          (read-token! t)))))

(defn read-token!
  "Reads one token `t` and updates parser `p`.

   Puts events on (:events-ch p) as they are parsed."
  [p t]
  (try
    (or (ensure-parsing p)
        (ignore-comment p t)
        (handle-newline p t)
        (disambiguate-name p t)
        (parse-accidentals p t)
        (parse-barline p t)
        (parse-clj-expr p t)
        (parse-name p t)
        (parse-note-length p t)
        (parse-octave-change p t)
        (parse-repeat p t)
        (parse-tie p t)
        (start-parsing-note p t)
        (start-parsing-rest p t)
        (start-parsing-event-seq p t)
        (finish-parsing-event-seq p t)
        (finish-parsing p t)
        (unexpected-token-error p t))
    (catch Throwable e
      (caught-error p e))))
