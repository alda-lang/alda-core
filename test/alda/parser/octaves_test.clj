(ns alda.parser.octaves-test
  (:require [clojure.test :refer :all]
            [alda.parser  :refer (parse-input)]))

(deftest octave-tests
  (testing "octave change"
    (is (= [(alda.lisp/octave :up)]
           (parse-input ">" :output :events)))
    (is (= [(alda.lisp/octave :down)]
           (parse-input "<" :output :events)))
    (is (= [(alda.lisp/octave 5)]
           (parse-input "o5" :output :events))))
  (testing "multiple octave changes back to back without spaces"
    (is (= [(alda.lisp/octave :up)
            (alda.lisp/octave :up)
            (alda.lisp/octave :up)]
           (parse-input ">>>" :output :events)))
    (is (= [(alda.lisp/octave :down)
            (alda.lisp/octave :down)
            (alda.lisp/octave :down)]
           (parse-input "<<<" :output :events)))
    (is (= [(alda.lisp/octave :up)
            (alda.lisp/octave :down)
            (alda.lisp/octave :up)]
           (parse-input "><>" :output :events))))
  (testing "octave changes back to back with notes"
    (is (= [(alda.lisp/octave :up)
            (alda.lisp/note (alda.lisp/pitch :c))]
           (parse-input ">c" :output :events)))
    (is (= [(alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/octave :down)]
           (parse-input "c<" :output :events)))
    (is (= [(alda.lisp/octave :up)
            (alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/octave :down)]
           (parse-input ">c<" :output :events)))))

