(ns alda.parser.markers-test
  (:require [clojure.test :refer :all]
            [alda.lisp]
            [alda.parser  :refer (parse-input)]))

(deftest marker-tests
  (testing "markers"
    (is (= [(alda.lisp/marker "chorus")]
           (parse-input "%chorus" :output :events)))
    (is (= [(alda.lisp/at-marker "verse-1")]
           (parse-input "@verse-1" :output :events)))))

