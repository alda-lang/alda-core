(ns alda.lisp.events.note-test
  (:require [alda.lisp.events.note :as note]
            [clojure.test :refer :all])
  (:import [alda.lisp.model.records AbsoluteOffset]))

(deftest event-updates-test
  (testing "MIDI note inside range parsing correctly"
    (let [score {:chord-mode false
                 :current-instruments #{"piano-jYJnW"}
                 :events #{}
                 :beats-tally nil
                 :instruments {"piano-jYJnW"
                               {:octave 3
                                :current-offset (AbsoluteOffset. 0)
                                :key-signature {}
                                :config {:type :midi :patch 1}
                                :transposition 0
                                :duration {:beats 1}
                                :volume 1.0
                                :tempo/role :master
                                :last-offset (AbsoluteOffset. -1)
                                :reference-pitch 440.0
                                :id "piano-jYJnW"
                                :quantization 0.9
                                :tempo 120
                                :panning 0.5
                                :current-marker :start
                                :time-scaling 1
                                :stock "midi-acoustic-grand-piano"
                                :track-volume 0.7874015748031497}}
                 :markers {:start 0}
                 :cram-level 0
                 :global-attributes {}
                 :tempo/values {0 120}
                 :nicknames {}
                 :beats-tally-default nil}
          event {:event-type :note :letter :c :accidentals []
                 :beats nil :ms nil :slur? nil}
          processed-event (note/event-updates score event)
          result-note (-> processed-event
                          first
                          :events
                          first
                          :midi-note)]
      (is (= 48 result-note))))
  #_(testing "MIDI note outside range should raise exception"
      (let [score {:chord-mode false
                   :current-instruments #{"piano-Fckbf"}
                   :events #{}
                   :beats-tally nil
                   :instruments {"piano-Fckbf"
                                 {:octave 10
                                  :current-offset (AbsoluteOffset. 0)
                                  :key-signature {}
                                  :config {:type :midi :patch 1}
                                  :transposition 0
                                  :duration {:beats 1}
                                  :volume 1.0
                                  :tempo/role :master
                                  :last-offset (AbsoluteOffset. -1)
                                  :reference-pitch 440.0
                                  :id "piano-Fckbf"
                                  :quantization 0.9
                                  :tempo 120
                                  :panning 0.5
                                  :current-marker :start
                                  :time-scaling 1
                                  :stock "midi-acoustic-grand-piano"
                                  :track-volume 0.7874015748031497}}
                   :markers {:start 0}
                   :cram-level 0
                   :global-attributes {}
                   :tempo/values {0 120}
                   :nicknames {}
                   :beats-tally-default nil}
            event {:event-type :note :letter :c :accidentals []
                   :beats nil :ms nil :slur? nil}]
        (is (thrown? Exception (note/event-updates score event))))))
