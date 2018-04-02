(ns alda.parser.chords-test
  (:require [clojure.test :refer :all]
            [alda.lisp]
            [alda.parser  :refer (parse-input)]))

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

