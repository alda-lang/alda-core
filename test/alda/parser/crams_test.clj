(ns alda.parser.crams-test
  (:require [clojure.test :refer :all]
            [alda.lisp]
            [alda.parser  :refer (parse-input)]))

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
