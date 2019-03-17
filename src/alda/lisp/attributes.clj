(ns alda.lisp.attributes
  (:require [alda.lisp.events          :refer (set-attribute
                                               global-attribute)]
            [alda.lisp.model.attribute :refer (*attribute-table*)]
            [alda.lisp.model.key       :refer (get-key-signature)]
            [alda.lisp.model.records   :refer (->AbsoluteOffset
                                               ->Attribute)]
            [alda.lisp.model.duration  :refer (note-length)]))

(comment
  "The :attributes key in an instrument functions like the :global-attributes
   key on the score. It is a map of offsets to the attributes updated for that
   instrument at that offset. The attribute changes for each offset are
   represented as a map of attribute keywords to values.")

(def ^:dynamic *initial-attr-vals* {:current-offset (->AbsoluteOffset 0)
                                    :last-offset    (->AbsoluteOffset -1)
                                    :current-marker :start
                                    :time-scaling   1})

(defmacro defattribute
  "Convenience macro for setting up attributes."
  [attr-name & things]
  (let [{:keys [aliases kw initial-val fn-name transform] :as opts}
        (if (string? (first things)) (rest things) things)
        aliases      (or aliases [])
        kw-name      (or kw (keyword attr-name))
        attr-aliases (vec (cons kw-name aliases))
        transform-fn (or transform #(constantly %))
        fn-name      (or fn-name attr-name)
        fn-names     (vec (cons fn-name (map (comp symbol name) aliases)))
        global-fns   (vec (map (comp symbol #(str % \!)) fn-names))
        attr         (gensym "attr")]
    (list* 'let [attr `(->Attribute ~kw-name ~transform-fn)]
      `(alter-var-root (var *initial-attr-vals*) assoc ~kw-name ~initial-val)
      `(doseq [alias# ~attr-aliases]
         (alter-var-root (var *attribute-table*) assoc alias# ~attr))
       (concat
         (for [fn-name fn-names]
           `(defn ~fn-name [x#]
              (set-attribute ~kw-name x#)))
         (for [global-fn-name global-fns]
           `(defn ~global-fn-name [x#]
              (global-attribute ~kw-name x#)))))))

(defn- percentage [x]
  {:pre [(<= 0 x 100)]}
  (constantly (/ x 100.0)))

(defn- unbound-percentage [x]
  {:pre [(<= 0 x)]}
  (constantly (/ x 100.0)))

;; Validation that the input is an integer value
(defn- pos-num [x]
  {:pre [(and (number? x)
              (pos? x))]}
  (constantly x))

;; Converts string/number representation of note length into number of beats
(defn- parse-note-length
  [length]
  {:pre [(or (string? length) (number? length))]}
  (if (string? length)
    (let [[_ number _ dots] (re-matches #"(\d+(\.\d+)?)(\.*)" length)
          note-lengths      (re-seq #"[^~]+" length)]
      (cond
        number
        (:value (note-length (Float/parseFloat number) {:dots (count dots)}))

        note-lengths
        (reduce + 0 (map parse-note-length note-lengths))

        :else
        (throw (Exception. (format "Invalid note length: %s" length)))))
    (:value (note-length length))))

(defattribute tempo
  "Current tempo. Used to calculate the duration of notes."
  :initial-val 120
  :fn-name     set-tempo
  :transform   (fn [val]
                 (cond
                   (map? val)
                   (fn [old]
                     (* (:ratio val) old))

                   :else
                   (pos-num val))))

(defn tempo
  "Multi-arity function that allows for an additional parameter to represent
   the tempo in terms of non-quarter notes (e.g.(tempo 2 60) <--> (tempo 120))."
  ([val]
   (tempo 4 val))

  ([note-length val]
   (set-attribute :tempo (if (map? val)
                           val
                           (* (parse-note-length note-length) val)))))

(defn tempo!
  "Global version of `tempo`."
  ([val]
   (tempo! 4 val))

  ([note-length val]
   (global-attribute :tempo (if (map? val)
                              val
                              (* (parse-note-length note-length) val)))))

(defn metric-modulation
  "Express tempo in terms of metric modulation, where the new note takes the
   same amount of time (one beat) as the old note.
   (e.g. (metric-modulation \"4.\" 2) means that the new length of a half note
   equals the length of a dotted quarter note in the previous measure)"
  ([old new]
   (tempo {:ratio (/ (parse-note-length new) (parse-note-length old))})))

(defn metric-modulation!
  "Global version of `metric-modulation`."
  ([old new]
   (tempo! {:ratio (/ (parse-note-length new) (parse-note-length old))})))

(defattribute duration
  "Default note duration in beats or milliseconds. (default: beats)"
  :initial-val {:beats 1}
  :fn-name set-duration
  :transform (fn [val]
               {:pre [(or
                       (map? val)
                       (and (number? val) (pos? val)))]}
               (constantly
                 (cond
                   (and (map? val) (= :milliseconds (:type val)))
                   {:ms (:value val)}

                   (and (map? val) (= :beats (:type val)))
                   {:beats (:value val)}

                   :else
                   {:beats val}))))

(defn set-note-length
  "Sets note duration in terms of alda note-lengths
   (e.g. quarter note = 4, dotted note = \"4.\")"
  [length]
  (set-duration (parse-note-length length)))

(defattribute octave
  "Current octave. Used to calculate the pitch of notes."
  :initial-val 4
  :transform (fn [val]
               {:pre [(or (integer? val)
                          (contains? #{:down :up} val))]}
               (case val
                :down dec
                :up inc
                (constantly val))))

(defattribute quantization
  "The percentage of a note that is heard.
   Used to put a little space between notes.

   e.g. with a quantization value of 90%, a note that would otherwise last
   500 ms will be quantized to last 450 ms. The resulting note event will
   have a duration of 450 ms, and the next event will be set to occur in 500 ms."
  :aliases [:quant :quantize]
  :initial-val 0.9
  :transform unbound-percentage)

(defattribute volume
  "Current volume. For MIDI purposes, the velocity of individual notes."
  :aliases [:vol]
  :initial-val 1.0
  :transform percentage)

(defattribute track-volume
  "More general volume for the track as a whole. Although this can be changed
   just as often as volume, to do so is not idiomatic. For MIDI purposes, this
   corresponds to the volume of a channel."
  :aliases [:track-vol]
  :initial-val (/ 100.0 127.0)
  :transform percentage)

(defattribute panning
  "Current panning."
  :aliases [:pan]
  :initial-val 0.5
  :transform percentage)

(defn- validate-str-key-sig
  "Validates the current key-sig. Checks for:

  1. No duplicate letters, ie: a- a+
  2. No letters out of range a-g

  If all tests pass, return true"
  [key-sig]
  ;; Get a version of key-sig with only characters
  (let [clean-str (apply str (filter #(Character/isLetter %) key-sig))]
    (and (not (re-find #"[^a-g]" clean-str))
         (= (count (distinct clean-str)) (count clean-str)))))

(defn- parse-key-signature
  "Transforms a key signature into a letter->accidentals map.

   If the key signature is already provided as a letter->accidentals map
   (e.g. {:f [:sharp] :c [:sharp] :g [:sharp]}), then it passes through this
   function unchanged.

   If the key signature is provided as a string, e.g. 'f+ c+ g+', then it is
   converted to a letter->accidentals map."
  [key-sig]
  {:pre [(or (not (string? key-sig))
             (validate-str-key-sig key-sig))]}

  (constantly
    (cond
      (map? key-sig)
      key-sig

      (string? key-sig)
      (into {}
        (map (fn [[_ _ letter accidentals]]
               [(keyword letter)
                (map {\- :flat \+ :sharp \_ :natural} accidentals)])
             (re-seq #"(([a-g])([+-_]*))" key-sig)))

      (sequential? key-sig)
      (let [[scale-type & more]    (reverse key-sig)
            [letter & accidentals] (reverse more)]
        (get-key-signature scale-type letter accidentals)))))

(defattribute key-signature
   "The key in which the current instrument is playing."
   :aliases [:key-sig]
   :initial-val {}
   :transform parse-key-signature)

(defattribute transposition
  "Increments or decrements each note by the desired number of half-steps."
  :aliases [:transpose]
  :initial-val 0
  :transform (fn [x]
               {:pre [(integer? x)]}
               (constantly x)))

(defattribute reference-pitch
  "The A4 pitch that instruments are tuned around."
  :aliases [:tuning-constant]
  :initial-val 440.0
  :transform pos-num)
