(ns alda.import.midi
  (:import [javax.sound.midi MidiSystem]
           [java.io File])
)

(def note-on 144)
(def note-off 128)
(defstruct note :channel :pitch :volume :start :duration)

(comment
  "This module accepts raw bytecode from a midi file, and converts it to alda syntax
   which is then printed to STDOUT.")

(defn- begin-note
  "Constructs the beginning of a vote given a NOTE_ON event"
  [note-on-event]
  (struct note
    (-> note-on-event .getMessage .getChannel)
    (-> note-on-event .getMessage .getData1)
    (-> note-on-event .getMessage .getData2)
    (-> note-on-event .getTick))
)

(defn- finish-note
  "Adds the duration to an existing note given a NOTE_OFF event.
   We do this by subtracting the note-off timestamp from the note-off timestamp"
  [note-off-event half-note]
  (assoc half-note :duration
    (- (.getTick note-off-event) (get half-note :start)))
)

(defn- note-from-event
  "Takes a new event from the Track and convert it to a Note struct"
  [event]
  (let [message (.getMessage event)]
    (cond
      (not= (type message) com.sun.media.sound.FastShortMessage) nil
      (= (.getCommand message) note-on)  (begin-note event)
      (= (.getCommand message) note-off) (finish-note event (begin-note event))
    ))
)

(defn- notes-from-track
  "Read a track and get a series of note data from it"
  [track]
  (remove nil?
    (for [tick (range 0 (.size track))]
      (note-from-event (.get track tick))))
)

(defn import-midi
  "Imports a .mid or .midi file specified by path, and prints it to STDOUT"
  [path]
  (remove empty?
    (map notes-from-track
      (.getTracks
        (MidiSystem/getSequence
          (new File path)))))
)
