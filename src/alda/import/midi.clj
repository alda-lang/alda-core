(ns alda.import.midi
  (:import [javax.sound.midi MidiSystem]
           [java.io File]))

(comment
  "This module accepts raw bytecode from a midi file, and converts it to alda syntax
   which is then printed to STDOUT.")

(defn- find-existing
  "Given some criteria, find the first matching hash in a collection of hashes"
  [collection criteria]
  (first (filter #(= criteria (select-keys % (keys criteria))) collection)))

(defn- note-on
  "We've just heard a new note beginning, update state partials to include it"
  [{:keys [instrument] :as state} event]
  (update state :partials conj {
    :channel    (-> event .getMessage .getChannel)
    :pitch      (-> event .getMessage .getData1)
    :volume     (-> event .getMessage .getData2)
    :start      (-> event .getTick)
    :instrument instrument
    ; duration will be set when the note completes
    :duration   nil}))

(defn- note-off
  "We've just heard a note complete; remove the note from partials, add a duration, and include it as a completed note"
  [{:keys [partials] :as state} event]
  (if-let [{:keys [start] :as partial-note}
           (find-existing partials
                          {:pitch (-> event .getMessage .getData1)
                           :channel (-> event .getMessage .getChannel)
                           :duration nil})]
    ; (or (println "note: " partial-note) (println "end: " (.getTick event)) (println "start: " start) (println "dur: " (- (.getTick event) start)))
    (let [completed-note (assoc partial-note :duration (- (.getTick event) start))]
      (-> state
        (update :partials (partial remove #{partial-note}))
        (update :notes conj completed-note)))
    state))

(defn- program-change
  "We've just switched instruments in this channel; update state to reflect this"
  [state event]
  (assoc state :instrument (-> event .getMessage .getData1)))

(defn- is-defined?
  "See if a java class responds to a method"
  [message method]
  (some #(= (.getName %) method) (-> message .getClass .getMethods)))

(defn process-note-event
  [state event]
  (let [message (.getMessage event)]
    (case (if (is-defined? message "getCommand") (.getCommand message))
      ; a note_on event also signifies that the previous note should be turned off
      144 (-> state (note-off event) (note-on event))
      128 (note-off state event)
      192 (program-change state event)
      state))) ; this isn't an event we care about; return the original state

(defn- notes-from-track
  "Get a final state by reading through a series of note events"
  [track]
  (let [events (map #(.get track %) (range (.size track)))]
    (get (reduce process-note-event {} events) :notes ())))

(defn import-midi
  "Imports a .mid or .midi file specified by path, and prints it to STDOUT"
  [path]
  (mapcat notes-from-track (-> (File. path) MidiSystem/getSequence .getTracks)))
