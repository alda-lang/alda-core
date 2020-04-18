(ns alda.lisp.events
  "Convenience functions for generating Alda events.

   These functions comprise the event functions in the alda.lisp DSL."
  (:require [alda.lisp.model.attribute :refer (get-attr)]))

(defn part
  "Determines the current instrument instance(s) based on the `instrument-call`
   and evaluates the `events` within that context.

   `instrument-call` can either be a map containing :names and an optional
   :nickname (e.g. {:names ['piano' 'trumpet'] :nickname ['trumpiano']}) or a
   valid Alda instrument call string, e.g. 'piano/trumpet 'trumpiano''."
  [instrument-call & events]
  {:event-type      :part
   :instrument-call instrument-call
   :events          events})

(defn note
  "Causes every instrument in :current-instruments to play a note at its
   :current-offset for the specified duration.

   If no duration is specified, the note is played for the instrument's own
   internal duration, which will be the duration last specified on a note or
   rest in that instrument's part."
  ([pitch]
   (note pitch nil false))
  ([pitch x]
    ; x could be a duration or :slur
   (let [duration (when (map? x) x)
         slur?    (= x :slur)]
     (note pitch duration slur?)))
  ([{:keys [letter accidentals midi-note]} {:keys [beats ms slurred]} slur?]
   {:event-type  :note
    :letter      letter
    :accidentals accidentals
    :midi-note   midi-note
    :beats       beats
    :ms          ms
    :slur?       (or slur? slurred)}))

(defn pause
  "Causes every instrument in :current-instruments to rest (not play) for the
   specified duration.

   If no duration is specified, each instrument will rest for its own internal
   duration, which will be the duration last specified on a note or rest in
   that instrument's part."
  [& [{:keys [beats ms] :as dur}]]
  {:event-type :rest
   :beats      beats
   :ms         ms})

(defn chord
  "Causes every instrument in :current-instruments to play each note in the
   chord simultaneously at the instrument's :current-offset."
  [& events]
  {:event-type :chord
   :events     events})

(defn set-attribute
  "Public fn for setting attributes in a score.
   e.g. (set-attribute :tempo 100)"
  [attr val]
  {:event-type :attribute-change
   :attr       (:kw-name (get-attr attr))
   :val        val})

(defn set-attributes
  "Convenience fn for setting multiple attributes at once.
   e.g. (set-attributes :tempo 100 :volume 50)"
  [& attrs]
  (for [[attr val] (partition 2 attrs)]
    (set-attribute attr val)))

(defn global-attribute
  "Public fn for setting global attributes in a score.
   e.g. (global-attribute :tempo 100)"
  [attr val]
  {:event-type :global-attribute-change
   :attr       (:kw-name (get-attr attr))
   :val        val})

(defn global-attributes
  "Convenience fn for setting multiple global attributes at once.
   e.g. (global-attributes :tempo 100 :volume 50)"
  [& attrs]
  (for [[attr val] (partition 2 attrs)]
    (global-attribute attr val)))

(defn apply-global-attributes
  "For each instrument in :current-instruments, looks between the instrument's
   :last-offset and :current-offset and applies any attribute changes occurring
   within that window.

   Both global and per-instrument attributes are applied; in the case that a
   per-instrument attribute is applied at the exact same time as a global
   attribute, the per-instrument attribute takes precedence for that instrument."
  []
  {:event-type :apply-global-attributes})

(defn barline
  "Barlines, at least currently, do nothing when evaluated in alda.lisp."
  []
  {:event-type :barline})

(defn marker
  "Places a marker at the current absolute offset. Throws an exception if there
   are multiple instruments active at different offsets."
  [name]
  {:event-type :marker
   :name       name})

(defn at-marker
  "Set the marker at which events will be added."
  [name]
  {:event-type :at-marker
   :name       name})

(defn voice
  "One voice in a voice group."
  [voice-number & events]
  {:event-type :voice
   :number     voice-number
   :events     events})

(defn end-voices
  "By default, the score remains in 'voice mode' until it reaches an end-voices
   event. This is so that if an instrument part ends with a voice group, the
   same voices can be appended later if the part is resumed, e.g. when building
   a score gradually in the Alda REPL or in a Clojure process.

   The end-voices event is emitted by the parser when it parses 'V0:'."
  []
  {:event-type :end-voice-group})

(defn- times-select
  [n event]
  (let [pair?          (fn [x]
                         (and (sequential? x)
                              (let [y (first x)]
                                (and (sequential? y)
                                     (every? number? y)))))

        ; for each unpaired event, pair it with a repetition vector
        ; (assume that unpaired = repeat each time)
        ; + decrement all reps to match future indexing
        ; e.g. [[[1 3] note1] note2] -> [[[0 2] note1] [[0 1 2] note2]] (n = 3)
        wrap-unpaired  (fn [evnt-or-pair]
                         (if (pair? evnt-or-pair)
                           (let [[reps evnt] evnt-or-pair]
                             (vector (map dec reps) evnt))
                           (vector (range n) evnt-or-pair)))

        ; if event seq is associated with one rep vector, then pair that rep
        ; with each event in the sequence
        ; e.g. [[[2] [note2 note3]]] -> [[[2] note2] [[2] note3]]
        expand-seq     (fn [[reps evnt :as pair]]
                         {:pre [(not-any? #(or (neg? %) (>= % n)) reps)]}
                         (if (vector? evnt)
                           (map #(vector reps %) evnt)
                           (vector pair)))

        ; repeat and index each repetition;
        ; for a certain indexed repetition, remove all events where that index
        ; does not appear in the event's rep vector
        filter-reps    (fn [index reps+events]
                         (->> reps+events
                              (filter (fn [[rep _]]
                                        (some #(= index %) rep)))
                              (mapv second)))]

    (->> event
         (mapcat (comp expand-seq wrap-unpaired))
         (repeat n)
         (map-indexed filter-reps)
         vec)))

(defn times
  "Repeats an Alda event (or sequence of events) `n` times.
   Also implements the alternate endings function. Takes as input a sequence of
   events optionally paired with which repetitions they play in. If no
   repetition position is specified for a certain event, then that event will
   play during all repetitions.
   e.g. (times 4 [[[1 3] note1] [[2] [note2 note3]] note4]) results in
        [[note1 note4] [note2 note3 note4] [note1 note4] [note4]]"
  [n event]
  (cond
    ; numbered repeat
    ; TODO: more robust/flexible condition check to differentiate between the
    ;       input for simple vs. numbered repeat
    (some number? (flatten event))
    (times-select n event)

    ; simple repeat
    :else
    (vec (repeat n event))))

(defn cram
  "A cram expression evaluates the events it contains, time-scaled based on the
   inner tally of beats in the events and the outer durations of each current
   instrument."
  [& events]
  (let [[duration & events] (if (:duration? (last events))
                              (cons (last events) (butlast events))
                              (cons nil events))]
    {:event-type :cram
     :duration   duration
     :events     events}))

(defn set-variable
  "Defines any number of events as a variable so that they can be referenced by
   name."
  [var-name & events]
  {:event-type :set-variable
   :variable   var-name
   :events     events})

(defn get-variable
  "Returns any number of events previously defined as a variable."
  [var-name]
  {:event-type :get-variable
   :variable   var-name})
