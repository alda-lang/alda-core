(ns alda.import.midi-test
  (:require [clojure.test      :refer :all]
            [alda.test-helpers :refer (get-instrument)]
            [alda.import.midi  :refer :all]))

(deftest midi-tests
  (testing "we can call java from the clojure"
    (is (= 3 (call-java)))
  )
)
