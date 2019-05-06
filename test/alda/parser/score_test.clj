(ns alda.parser.score-test
  (:require [clojure.test :refer :all]
            [alda.parser :refer (parse-input)]))

(deftest score-tests
  (is (= [(alda.lisp/part {:names ["theremin"]})
          (alda.lisp/note (alda.lisp/pitch :c))
          (alda.lisp/note (alda.lisp/pitch :d))
          (alda.lisp/note (alda.lisp/pitch :e))]
         (parse-input "theremin: c d e" :output :events)))
  (is (= [(alda.lisp/part {:names ["trumpet" "trombone" "tuba"]
                           :nickname "brass"})
          (alda.lisp/note
            (alda.lisp/pitch :f :sharp)
            (alda.lisp/duration (alda.lisp/note-length 1)))]
         (parse-input "trumpet/trombone/tuba \"brass\": f+1"
                      :output :events)))
  (is (= [(alda.lisp/part {:names ["guitar"]})
          (alda.lisp/note (alda.lisp/pitch :e))
          (alda.lisp/part {:names ["bass"]})
          (alda.lisp/note (alda.lisp/pitch :e))]
         (parse-input "guitar: e
                       bass: e"
                      :output :events)))
  (testing "tabs are treated as whitespace"
    (is (= [(alda.lisp/part {:names ["piano"]})
            (alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/note (alda.lisp/pitch :e))
            (alda.lisp/note (alda.lisp/pitch :g))]
           (parse-input "piano:\tc e g"
                        :output :events)))))
