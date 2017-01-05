(ns alda.import.midi
  (:import [javax.sound.midi MidiSystem]
           [java.io File])
)

(comment
  "This module accepts raw bytecode from a midi file, and converts it to alda syntax
   which is then printed to STDOUT.")

(defn- find-existing
  "Given some criteria, find the first matching hash in a collection of hashes" ; I feel like clojure should be able to do this for me?
  [collection criteria]
  (some #(if (= criteria (select-keys % (keys criteria))) %) collection)
)

(defn- note-on
  "We've just heard a new note beginning, update state partials to include it"
  [state event]
  (assoc state :partials (conj (get state :partials) (hash-map
    :channel    (-> event .getMessage .getChannel)
    :pitch      (-> event .getMessage .getData1)
    :volume     (-> event .getMessage .getData2)
    :start      (-> event .getTick)
    :instrument (get state :instrument)
    :duration   nil))) ; duration will be set when the note completes
)

(defn- note-off
  "We've just heard a note complete; remove the note from partials, add a duration, and include it as a completed note"
  [state event]
  (let [partial (find-existing (get state :partials) {
          :pitch    (-> event .getMessage .getData1)
          :channel  (-> event .getMessage .getChannel)
          :duration nil })]
    (if (nil? partial)
      state                                                                ; couldn't find corresponding note; return state as-is
      (assoc state :partials (remove #(= % partial) (get state :partials)) ; remove partial note
                   :notes    (conj (get state :notes)                      ; and re-add the completed note
                               (assoc partial :duration (- (.getTick event) (get partial :start)))))))
)

(defn- program-change
  "We've just switched instruments in this channel; update state to reflect this"
  [state event]
  (assoc state :instrument (-> event .getMessage .getData1))
)

(defn- is-defined?
  ; This is a bit ugly: We get an array of Events, some of which respond to
  ; getCommand() and some don't... we need to filter out the ones which don't
  ; otherwise the world blows up. Seeing if 'getCommand' was reflected on the Java
  ; class was the only way I could get to work, but there's probly a better way.
  "See if a java class responds to a method"
  [message method]
  (some #(= (.getName %) method) (-> message .getClass .getMethods))
)

(defn process-note-event
  [state event]
  (let [message (.getMessage event)]
    (case (if (is-defined? message "getCommand") (.getCommand message))
      144 (note-on state event)
      128 (note-off state event)
      192 (program-change state event)
      state)) ; this isn't an event we care about; return the original state
)

(defn- notes-from-track
  "Get a final state by reading through a series of note events"
  [track]
  (get
    (reduce process-note-event {}
      (map #(.get track %) (range (.size track)))) ; fetch all events from the given track
        :notes ())
)

(defn import-midi
  "Imports a .mid or .midi file specified by path, and prints it to STDOUT"
  [path]
  (flatten
    (map notes-from-track
      (-> (new File path) MidiSystem/getSequence .getTracks)))
)
