(ns alda.import.midi-test
  (:require [clojure.test      :refer :all]
            [alda.test-helpers :refer (get-instrument)]
            [alda.import.midi  :refer :all])
  (:import [javax.sound.midi MidiEvent MidiMessage ShortMessage]
           [com.sun.media.sound FastShortMessage])
)

(defn- build-event
  "Build an event for testing"
  ([command]                     (build-event command 0       0     0     10))
  ([command channel]             (build-event command channel 0     0     10))
  ([command channel data1]       (build-event command channel data1 0     10))
  ([command channel data1 data2] (build-event command channel data1 data2 10))
  ([command channel data1 data2 seq]
    (new MidiEvent (new ShortMessage command channel data1 data2) seq))
)

(deftest midi-tests
  (testing "can process a midi file"
    (is (= "some test data" (import-midi "./examples/midi/twotone.mid")))
  )

  (testing "note-on adds a new note"
    (let [event (build-event 144)
          state (process-note-event {} event)]
      (is (= 1 (count (get state :notes)))))
  )

  (testing "note-off updates an existing note with the duration"
    (let [event (build-event 128)
          prev-state (process-note-event {} (build-event 144))
          state (process-note-event prev-state event)]
      (is (not= nil (get-in state [:notes 0 :duration]))))
  )

  (testing "change-program updates the instrument of the state"
    (let [event (build-event 192 0 15) ; include '15' as the data for the instrument to change to
          state (process-note-event {} event)]
      (is (= 15 (get state :instrument))))
  )

  (testing "other events don't change the state"
    (let [event (build-event 208)
          state (process-note-event {} event)]
      (is (= {} state)))
  )
)
