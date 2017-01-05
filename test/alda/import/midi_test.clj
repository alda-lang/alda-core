(ns alda.import.midi-test
  (:require [clojure.test      :refer :all]
            [alda.test-helpers :refer (get-instrument)]
            [alda.import.midi  :refer :all])
  (:import [javax.sound.midi MidiEvent MidiMessage ShortMessage]
           [com.sun.media.sound FastShortMessage])
)

(defn- build-event
  "Build an event for testing"
  ([command]                     (build-event command 0))
  ([command channel]             (build-event command channel 0))
  ([command channel data1]       (build-event command channel data1 0))
  ([command channel data1 data2] (build-event command channel data1 data2 10))
  ([command channel data1 data2 seq]
    (new MidiEvent (new ShortMessage command channel data1 data2) seq))
)

(deftest midi-tests
  (testing "can process a short midi file"
    (let [result (import-midi "./examples/midi/twotone.mid")]
      ; we get a collection with two tones
      (is (= 2 (count result)))

      ; first note
      (is (= 0     (get (first result) :instrument)))
      (is (= 0     (get (first result) :start)))
      (is (= 0     (get (first result) :channel)))
      (is (= 72    (get (first result) :pitch)))
      (is (= 11520 (get (first result) :duration)))
      (is (= 40    (get (first result) :volume)))
      ; second note
      (is (= 0     (get (last result) :instrument)))
      (is (= 1440  (get (last result) :start)))
      (is (= 0     (get (last result) :channel)))
      (is (= 74    (get (last result) :pitch)))
      (is (= 7200  (get (last result) :duration)))
      (is (= 40    (get (last result) :volume)))
    )
  )

  ; this doesn't work yet, I think because this file has 0-duration NOTE_ON events in lieu of NOTE_OFF events
  ; (apparantly that's a thing that MIDI does sometimes)
  ; (testing "can process a big midi file"
  ;   (is (= "some test data" (import-midi "./examples/midi/pokemon.mid")))
  ; )

  (testing "note-on adds a new note partial"
    (let [event (build-event 144 9)
          state (process-note-event {} event)]
      (is (= 1 (count (get state :partials))))
      (is (= 9 (get (first (get state :partials)) :channel))))
  )

  (testing "note-off updates an existing note with the duration"
    (let [event (build-event 128 0 0 0 50)
          prev-state (process-note-event {} (build-event 144 0 0 0 10))
          state (process-note-event prev-state event)]
      (is (= 1 (count (get state :notes))))
      (is (= 40 (get (first (get state :notes)) :duration))))
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
