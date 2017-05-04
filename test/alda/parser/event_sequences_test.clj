(ns alda.parser.event-sequences-test
  (:require [clojure.test :refer :all]
            [alda.parser  :refer (parse-input)]))

(deftest event-sequence-tests
  (testing "event sequences"
    (is (= (parse-input "[]" :output :events) [[]]))
    (is (= (parse-input "[   ]" :output :events) [[]]))
    (is (= (parse-input "[ c d e f c/e/g ]" :output :events)
           [[(alda.lisp/note (alda.lisp/pitch :c))
             (alda.lisp/note (alda.lisp/pitch :d))
             (alda.lisp/note (alda.lisp/pitch :e))
             (alda.lisp/note (alda.lisp/pitch :f))
             (alda.lisp/chord
               (alda.lisp/note (alda.lisp/pitch :c))
               (alda.lisp/note (alda.lisp/pitch :e))
               (alda.lisp/note (alda.lisp/pitch :g)))]]))
    (is (= (parse-input "[c d [e f] g]" :output :events)
           [[(alda.lisp/note (alda.lisp/pitch :c))
             (alda.lisp/note (alda.lisp/pitch :d))
             [(alda.lisp/note (alda.lisp/pitch :e))
              (alda.lisp/note (alda.lisp/pitch :f))]
             (alda.lisp/note (alda.lisp/pitch :g))]])))
  (testing "voices within event sequences parse successfully"
    (is (= (parse-input "[V1: e b d V2: a c f]" :output :events)
           [[(alda.lisp/voice
               1
               (alda.lisp/note (alda.lisp/pitch :e))
               (alda.lisp/note (alda.lisp/pitch :b))
               (alda.lisp/note (alda.lisp/pitch :d)))
             (alda.lisp/voice
               2
               (alda.lisp/note (alda.lisp/pitch :a))
               (alda.lisp/note (alda.lisp/pitch :c))
               (alda.lisp/note (alda.lisp/pitch :f)))]]))))
