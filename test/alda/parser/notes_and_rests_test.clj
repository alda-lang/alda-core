(ns alda.parser.notes-and-rests-test
  (:require [clojure.test :refer :all]
            [alda.lisp]
            [alda.parser  :refer (parse-input)]))

(deftest note-and-rest-tests
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
           (parse-input "[c d c r ]" :output :events)))
    ;; Regression test for https://github.com/alda-lang/alda-core/issues/52
    ;; which involved incorrectly parsing a rest `r` followed by a newline
    ;; within a variable definition.
    (is (= [(alda.lisp/set-variable
              :foo
              (alda.lisp/note
                (alda.lisp/pitch :c)
                (alda.lisp/duration
                  (alda.lisp/note-length 8)))
              (alda.lisp/note (alda.lisp/pitch :d))
              (alda.lisp/note (alda.lisp/pitch :c))
              (alda.lisp/pause))
            (alda.lisp/part {:names ["piano"]})
            (repeat 2 (alda.lisp/get-variable :foo))]
           (parse-input "foo = c8 d c r\npiano: foo*2" :output :events)))))

