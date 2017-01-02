(ns alda.import.midi
  (:import [java.lang Math])
)

(comment
  "This module accepts raw bytecode from a midi file, and converts it to alda syntax
   which is then printed to STDOUT.")

(defn call-java
  "Calls a java function; I'm just doing this to get my feet wet."
  []
  (java.lang.Math/abs -3)
)

(defn slurp-midi
  "read a midi file and convert it to an array of bytes"
  []
  nil
)

(defn extract-midi-notes
  "converts an array of bytes to a series of note on pairs"
  []
  nil
)

(defn midi-note-to-alda
  "Takes a note on / note off pair and converts it to an alda note"
  []
  nil
)
