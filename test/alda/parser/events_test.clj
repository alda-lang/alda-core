(ns alda.parser.events-test
  (:require [clojure.test :refer :all]
            [alda.lisp]
            [alda.parser  :refer (parse-input)]))

(deftest note-tests
  (testing "notes"
    (is (= [(alda.lisp/note (alda.lisp/pitch :c))]
           (parse-input "c" :output :events)))
    (is (= [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/note-length 4)))]
           (parse-input "c4" :output :events)))
    (is (= [(alda.lisp/note (alda.lisp/pitch :c :sharp))]
           (parse-input "c+" :output :events)))
    (is (= [(alda.lisp/note (alda.lisp/pitch :b :flat))]
           (parse-input "b-" :output :events)))
    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))
             (alda.lisp/note (alda.lisp/pitch :d))
             (alda.lisp/note (alda.lisp/pitch :c))]]
           (parse-input "[c d c]" :output :events)))
    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))
             (alda.lisp/note (alda.lisp/pitch :d))
             (alda.lisp/note (alda.lisp/pitch :c))]]
           (parse-input "[c d c ]" :output :events))))
  (testing "rests"
    (is (= [(alda.lisp/pause)]
           (parse-input "r" :output :events))
        (= [(alda.lisp/pause (alda.lisp/duration (alda.lisp/note-length 1)))]
           (parse-input "r1" :output :events)))
    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))
             (alda.lisp/note (alda.lisp/pitch :d))
             (alda.lisp/note (alda.lisp/pitch :c))
             (alda.lisp/pause)]]
           (parse-input "[c d c r]" :output :events)))
    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))
             (alda.lisp/note (alda.lisp/pitch :d))
             (alda.lisp/note (alda.lisp/pitch :c))
             (alda.lisp/pause)]]
           (parse-input "[c d c r ]" :output :events)))))

(deftest chord-tests
  (testing "chords"
    (is (= [(alda.lisp/chord
              (alda.lisp/note (alda.lisp/pitch :c))
              (alda.lisp/note (alda.lisp/pitch :e))
              (alda.lisp/note (alda.lisp/pitch :g)))]
           (parse-input "c/e/g" :output :events)))
    (is (= [(alda.lisp/chord
              (alda.lisp/note
                (alda.lisp/pitch :c)
                (alda.lisp/duration (alda.lisp/note-length 1)))
              (alda.lisp/octave :up)
              (alda.lisp/note
                (alda.lisp/pitch :e)
                (alda.lisp/duration (alda.lisp/note-length 2)))
              (alda.lisp/note
                (alda.lisp/pitch :g)
                (alda.lisp/duration (alda.lisp/note-length 4)))
              (alda.lisp/pause
                (alda.lisp/duration (alda.lisp/note-length 8))))]
           (parse-input "c1/>e2/g4/r8" :output :events)))
    (is (= [(alda.lisp/chord
              (alda.lisp/note (alda.lisp/pitch :b))
              (alda.lisp/octave :up)
              (alda.lisp/note (alda.lisp/pitch :d))
              (alda.lisp/note
                (alda.lisp/pitch :f)
                (alda.lisp/duration (alda.lisp/note-length 2 {:dots 1}))))]
           (parse-input "b>/d/f2." :output :events)))))

(deftest voice-tests
  (testing "voices"
    (is (= [(alda.lisp/part {:names ["piano"]})
            (alda.lisp/voice 1)
            (alda.lisp/note (alda.lisp/pitch :a))
            (alda.lisp/note (alda.lisp/pitch :b))
            (alda.lisp/note (alda.lisp/pitch :c))]
           (parse-input "piano: V1: a b c" :output :events)))
    (is (= [(alda.lisp/part {:names ["piano"]})
            (alda.lisp/voice 1)
            (alda.lisp/note (alda.lisp/pitch :a))
            (alda.lisp/note (alda.lisp/pitch :b))
            (alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/voice 2)
            (alda.lisp/note (alda.lisp/pitch :d))
            (alda.lisp/note (alda.lisp/pitch :e))
            (alda.lisp/note (alda.lisp/pitch :f))]
           (parse-input "piano:
                         V1: a b c
                         V2: d e f"
                        :output :events)))
    (is (= [(alda.lisp/part {:names ["piano"]})
            (alda.lisp/voice 1)
            (alda.lisp/note (alda.lisp/pitch :a))
            (alda.lisp/note (alda.lisp/pitch :b))
            (alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/barline)
            (alda.lisp/voice 2)
            (alda.lisp/note (alda.lisp/pitch :d))
            (alda.lisp/note (alda.lisp/pitch :e))
            (alda.lisp/note (alda.lisp/pitch :f))]
           (parse-input "piano:
                         V1: a b c | V2: d e f"
                        :output :events)))
    (is (= [(alda.lisp/part {:names ["piano"]})
            (alda.lisp/voice 1)
            (alda.lisp/times 8
              [(alda.lisp/note (alda.lisp/pitch :a))
               (alda.lisp/note (alda.lisp/pitch :b))
               (alda.lisp/note (alda.lisp/pitch :c))])
            (alda.lisp/voice 2)
            (alda.lisp/times 8
              [(alda.lisp/note (alda.lisp/pitch :d))
               (alda.lisp/note (alda.lisp/pitch :e))
               (alda.lisp/note (alda.lisp/pitch :f))])]
           (parse-input "piano:
                         V1: [a b c] *8
                         V2: [d e f] *8"
                        :output :events)))))

(deftest marker-tests
  (testing "markers"
    (is (= [(alda.lisp/marker "chorus")]
           (parse-input "%chorus" :output :events)))
    (is (= [(alda.lisp/at-marker "verse-1")]
           (parse-input "@verse-1" :output :events)))))

(deftest cram-tests
  (testing "crams"
    (is (= [(alda.lisp/cram
              (alda.lisp/note (alda.lisp/pitch :c))
              (alda.lisp/note (alda.lisp/pitch :d))
              (alda.lisp/note (alda.lisp/pitch :e)))]
           (parse-input "{c d e}" :output :events)))
    (is (= [(alda.lisp/cram
              (alda.lisp/note (alda.lisp/pitch :c))
              (alda.lisp/note (alda.lisp/pitch :d))
              (alda.lisp/note (alda.lisp/pitch :e))
              (alda.lisp/duration (alda.lisp/note-length 2)))]
           (parse-input "{c d e}2" :output :events)))))
