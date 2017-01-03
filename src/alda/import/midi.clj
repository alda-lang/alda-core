(ns alda.import.midi
  (:import [javax.sound.midi MidiSystem ShortMessage]
           [java.io File])
)

(comment
  "This module accepts raw bytecode from a midi file, and converts it to alda syntax
   which is then printed to STDOUT.")

(defn- note-on
  "We've just heard a new note beginning, update state to include it"
  [state event]
  (assoc state :notes (concat (get state :notes) [(hash-map
    :channel    (-> event .getMessage .getChannel)
    :pitch      (-> event .getMessage .getData1)
    :volume     (-> event .getMessage .getData2)
    :start      (-> event .getTick)
    :instrument (get state :instrument))]))
)

(defn- note-off
  "We've just heard a note complete; update state to include its duration"
  [state event]
  state ; TODO
)

(defn- program-change
  "We've just switched instruments in this channel; update state to reflect this"
  [state event]
  (assoc state :instrument (-> event .getMessage .getData1))
)

(defn process-note-event
  [state event]
  (if (= (type (.getMessage event)) ShortMessage)
    (case (-> event .getMessage .getCommand)
      144 (note-on state event)
      128 (note-off state event)
      192 (program-change state event)
      state)
    state) ; this isn't an event we care about; return the original state
)

(defn- notes-from-track
  "Get a final state by reading through a series of note events"
  [track]
  (get
    (reduce process-note-event {}
      (map #(.get track %)
        (range 0 (.size track)))) :notes [])
)

(defn import-midi
  "Imports a .mid or .midi file specified by path, and prints it to STDOUT"
  [path]
  (flatten
    (map notes-from-track
      (-> (new File path) MidiSystem/getSequence .getTracks)))
)
