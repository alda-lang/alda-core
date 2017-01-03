(ns alda.import.midi-test
  (:require [clojure.test      :refer :all]
            [alda.test-helpers :refer (get-instrument)]
            [alda.import.midi  :refer :all]))

(deftest midi-tests
  (testing "we can chain function calls together"
    (is (= "some test data" (import-midi "./examples/midi/twotone.mid")))
  )
)
