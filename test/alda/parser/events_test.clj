(ns alda.parser.events-test
  (:require [clojure.test :refer :all]
            [alda.parser  :refer (parse-input)]))

(deftest note-tests
  (testing "notes"
    (is (= (parse-input "c" :output :events)
           [(alda.lisp/note (alda.lisp/pitch :c))]))
    (is (= (parse-input "c4" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/note-length 4)))]))
    (is (= (parse-input "c+" :output :events)
           [(alda.lisp/note (alda.lisp/pitch :c :sharp))]))
    (is (= (parse-input "b-" :output :events)
           [(alda.lisp/note (alda.lisp/pitch :b :flat))])))
  (testing "rests"
    (is (= (parse-input "r" :output :events)
           [(alda.lisp/pause)])
        (= (parse-input "r1" :output :events)
           [(alda.lisp/pause (alda.lisp/duration (alda.lisp/note-length 1)))]))))

(deftest chord-tests
  (testing "chords"
    (is (= (parse-input "c/e/g" :output :events)
           [(alda.lisp/chord
              (alda.lisp/note (alda.lisp/pitch :c))
              (alda.lisp/note (alda.lisp/pitch :e))
              (alda.lisp/note (alda.lisp/pitch :g)))]))
    (is (= (parse-input "c1/>e2/g4/r8" :output :events)
           [(alda.lisp/chord
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
                (alda.lisp/duration (alda.lisp/note-length 8))))]))
    (is (= (parse-input "b>/d/f2." :output :events)
           [(alda.lisp/chord
              (alda.lisp/note (alda.lisp/pitch :b))
              (alda.lisp/octave :up)
              (alda.lisp/note (alda.lisp/pitch :d))
              (alda.lisp/note
                (alda.lisp/pitch :f)
                (alda.lisp/duration (alda.lisp/note-length 2 {:dots 1}))))]))))

(deftest voice-tests
  (testing "voices"
    (is (= (parse-input "piano: V1: a b c" :output :events)
           [(alda.lisp/part {:names ["piano"]}
              (alda.lisp/voice 1
                (alda.lisp/note (alda.lisp/pitch :a))
                (alda.lisp/note (alda.lisp/pitch :b))
                (alda.lisp/note (alda.lisp/pitch :c))))]))
    (is (= (parse-input "piano:
                         V1: a b c
                         V2: d e f"
                        :output :events)
           [(alda.lisp/part {:names ["piano"]}
              (alda.lisp/voice 1
                (alda.lisp/note (alda.lisp/pitch :a))
                (alda.lisp/note (alda.lisp/pitch :b))
                (alda.lisp/note (alda.lisp/pitch :c)))
              (alda.lisp/voice 2
                (alda.lisp/note (alda.lisp/pitch :d))
                (alda.lisp/note (alda.lisp/pitch :e))
                (alda.lisp/note (alda.lisp/pitch :f))))]))
    (is (= (parse-input "piano:
                         V1: a b c | V2: d e f"
                        :output :events)
           [(alda.lisp/part {:names ["piano"]}
              (alda.lisp/voice 1
                (alda.lisp/note (alda.lisp/pitch :a))
                (alda.lisp/note (alda.lisp/pitch :b))
                (alda.lisp/note (alda.lisp/pitch :c))
                (alda.lisp/barline))
              (alda.lisp/voice 2
                (alda.lisp/note (alda.lisp/pitch :d))
                (alda.lisp/note (alda.lisp/pitch :e))
                (alda.lisp/note (alda.lisp/pitch :f))))]))
    (is (= (parse-input "piano:
                         V1: [a b c] *8
                         V2: [d e f] *8"
                        :output :events)
           [(alda.lisp/part {:names ["piano"]}
              (alda.lisp/voice 1
                (alda.lisp/times 8
                  [(alda.lisp/note (alda.lisp/pitch :a))
                   (alda.lisp/note (alda.lisp/pitch :b))
                   (alda.lisp/note (alda.lisp/pitch :c))]))
              (alda.lisp/voice 2
                (alda.lisp/times 8
                  [(alda.lisp/note (alda.lisp/pitch :d))
                   (alda.lisp/note (alda.lisp/pitch :e))
                   (alda.lisp/note (alda.lisp/pitch :f))])))]))))

(deftest marker-tests
  (testing "markers"
    (is (= (parse-input "%chorus" :output :events)
           [(alda.lisp/marker "chorus")]))
    (is (= (parse-input "@verse-1" :output :events)
           [(alda.lisp/at-marker "verse-1")]))))

(deftest cram-tests
  (testing "crams"
    (is (= (parse-input "{c d e}" :output :events)
           [(alda.lisp/cram
              (alda.lisp/note (alda.lisp/pitch :c))
              (alda.lisp/note (alda.lisp/pitch :d))
              (alda.lisp/note (alda.lisp/pitch :e)))]))
    (is (= (parse-input "{c d e}2" :output :events)
           [(alda.lisp/cram
              (alda.lisp/note (alda.lisp/pitch :c))
              (alda.lisp/note (alda.lisp/pitch :d))
              (alda.lisp/note (alda.lisp/pitch :e))
              (alda.lisp/duration (alda.lisp/note-length 2)))]))))
