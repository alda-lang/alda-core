(ns alda.parser.barlines-test
  (:require [clojure.test :refer :all]
            [alda.parser  :refer (parse-input)]))

(def alda-code-1
  "violin: c d | e f | g a")

(def alda-events-1
  [(alda.lisp/part {:names ["violin"]})
   (alda.lisp/note (alda.lisp/pitch :c))
   (alda.lisp/note (alda.lisp/pitch :d))
   (alda.lisp/barline)
   (alda.lisp/note (alda.lisp/pitch :e))
   (alda.lisp/note (alda.lisp/pitch :f))
   (alda.lisp/barline)
   (alda.lisp/note (alda.lisp/pitch :g))
   (alda.lisp/note (alda.lisp/pitch :a))])

(def alda-code-2
  "marimba: c1|~1|~1~|1|~1~|2.")

(def alda-events-2
  [(alda.lisp/part {:names ["marimba"]})
   (alda.lisp/note
     (alda.lisp/pitch :c)
     (alda.lisp/duration
       (alda.lisp/note-length 1)
       (alda.lisp/barline)
       (alda.lisp/note-length 1)
       (alda.lisp/barline)
       (alda.lisp/note-length 1)
       (alda.lisp/barline)
       (alda.lisp/note-length 1)
       (alda.lisp/barline)
       (alda.lisp/note-length 1)
       (alda.lisp/barline)
       (alda.lisp/note-length 2 {:dots 1})))])

(deftest barline-tests
  (testing "barlines are included in alda.lisp code (even though they don't do anything)"
    (is (= alda-events-1 (parse-input alda-code-1 :output :events))))
  (testing "notes can be tied over barlines"
    (is (= alda-events-2 (parse-input alda-code-2 :output :events)))))

