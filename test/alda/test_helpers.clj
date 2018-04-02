(ns alda.test-helpers
  (:require [clojure.string           :as    str]
            [alda.lisp.model.duration :refer (calculate-duration)]
            [alda.lisp.model.pitch    :refer (midi->hz determine-midi-note)]
            [alda.parser              :refer (parse-input)]))

(defn get-instrument
  "Returns the first instrument in :instruments whose id starts with inst-name."
  [{:keys [instruments] :as score} inst-name]
  (first (for [[id instrument] instruments
               :when (str/starts-with? id (str inst-name \-))]
           instrument)))

(defn dur->ms
  "Given a duration map, a tempo, and (optionally) a time-scaling value,
   returns the calculated duration in milliseconds.

   The duration map can be created via the `duration` function, which takes any
   number of note-length / millisecond components.

   e.g.
   (dur->ms (duration (note-length 8)) 120) => 250"
  [{:keys [beats ms]} tempo & [time-scaling]]
  (calculate-duration beats tempo (or time-scaling 1) ms))

(defn calculate-pitch
  [letter accidentals octave key-sig & [{:keys [ref-pitch transpose]
                                         :or {ref-pitch 440 transpose 0}}]]
  (let [midi-note (determine-midi-note {:letter letter
                                        :accidentals accidentals}
                                       octave
                                       key-sig
                                       transpose)]
    (midi->hz ref-pitch midi-note)))

(defn parse-events
  [input]
  (parse-input input :output :events))

(defn parse-events-or-error
  [input]
  (parse-input input :output :events-or-error))
