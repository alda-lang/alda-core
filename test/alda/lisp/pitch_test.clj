(ns alda.lisp.pitch-test
  (:require [clojure.test      :refer :all]
            [alda.test-helpers :refer (get-instrument
                                       calculate-pitch)]
            [alda.lisp         :refer :all]))

(deftest pitch-tests
  (testing "pitch converts a note in a given octave to frequency in Hz"
    (is (== 440 (calculate-pitch :a [] 4 {})))
    (is (== 880 (calculate-pitch :a [] 5 {})))
    (is (< 261 (calculate-pitch :c  [] 4 {}) 262)))
  (testing "flats and sharps"
    (is (> (calculate-pitch :c [:sharp] 4 {})
           (calculate-pitch :c [] 4 {})))
    (is (> (calculate-pitch :c [] 5 {})
           (calculate-pitch :c [:sharp] 4 {})))
    (is (< (calculate-pitch :b [:flat] 4 {})
           (calculate-pitch :b [] 4 {})))
    (is (== (calculate-pitch :c [:sharp] 4 {})
            (calculate-pitch :d [:flat] 4 {})))
    (is (== (calculate-pitch :c [:sharp :sharp] 4 {})
            (calculate-pitch :d [] 4 {})))
    (is (== (calculate-pitch :f [:flat] 4 {})
            (calculate-pitch :e [] 4 {})))
    (is (== (calculate-pitch :a [:flat :flat] 4 {})
            (calculate-pitch :g [] 4 {})))
    (is (== (calculate-pitch :c [:sharp :flat :flat :sharp] 4 {})
            (calculate-pitch :c [] 4 {})))))

(deftest key-tests
  (testing "you can set and get a key signature"
    (let [s     (score
                  (part "piano"
                    (key-signature {:b [:flat] :e [:flat]})))
          piano (get-instrument s "piano")]
      (is (= {:b [:flat] :e [:flat]}
             (:key-signature piano))))
    (let [s     (score
                  (part "piano"
                    (key-sig "f+ c+ g+")))
          piano (get-instrument s "piano")]
      (is (= {:f [:sharp] :c [:sharp] :g [:sharp]}
             (:key-signature piano))))
    (let [s     (score
                  (part "piano"
                    (key-sig [:a :flat :major])))
          piano (get-instrument s "piano")]
      (is (= {:b [:flat] :e [:flat] :a [:flat] :d [:flat]}
             (:key-signature piano))))
    (let [s     (score
                  (part "piano"
                    (key-sig [:e :minor])))
          piano (get-instrument s "piano")]
      (is (= {:f [:sharp]}
             (:key-signature piano))))
    (let [s     (score
                 (part "piano"
                   (key-sig [:c :lydian])))
         piano (get-instrument s "piano")]
     (is (= {:f [:sharp]}
            (:key-signature piano))))
    (let [s     (score
                  (part "piano"
                    (key-sig [:d :mixolydian])))
          piano (get-instrument s "piano")]
      (is (= {:f [:sharp]}
             (:key-signature piano))))
    (let [s     (score
                (part "piano"
                  (key-sig [:e :dorian])))
        piano (get-instrument s "piano")]
    (is (= {:f [:sharp], :c [:sharp]}
           (:key-signature piano))))
    (let [s     (score
                 (part "piano"
                   (key-sig [:f :phrygian])))
         piano (get-instrument s "piano")]
     (is (= {:b [:flat] :e [:flat] :a [:flat] :d [:flat] :g [:flat]}
            (:key-signature piano))))
    (let [s     (score
                 (part "piano"
                   (key-sig [:g :locrian])))
         piano (get-instrument s "piano")]
     (is (= {:b [:flat] :e [:flat] :a [:flat] :d [:flat] }
            (:key-signature piano))))
    (let [s     (score
                 (part "piano"
                   (key-sig [:a :ionian])))
         piano (get-instrument s "piano")]
     (is (= {:f [:sharp], :c [:sharp] :g [:sharp]}
            (:key-signature piano))))
    (let [s     (score
                 (part "piano"
                   (key-sig [:b :aeolian])))
         piano (get-instrument s "piano")]
     (is (= {:f [:sharp], :c [:sharp]}
            (:key-signature piano)))))

  (testing "the pitch of a note is affected by the key signature"
    (is (= (calculate-pitch :b [] 4 {:b [:flat]})
           (calculate-pitch :b [:flat] 4 {})))
    (is (= (calculate-pitch :b [:natural] 4 {:b [:flat]})
           (calculate-pitch :b [] 4 {})))
    (let [s         (score
                      (part "piano"
                        (key-signature "f+")))
          piano     (get-instrument s "piano")
          f-sharp-4 (calculate-pitch :f [] 4 (:key-signature piano))]
      (is (= f-sharp-4 (calculate-pitch :f [:sharp] 4 {})))))

  (testing "ionian should be the same as major"
    (let [s     (score
                 (part "piano"
                   (key-sig [:c :ionian])))
          t     (score
                  (part "piano"
                    (key-sig [:c :major])))
         piano  (get-instrument s "piano")
         piano2 (get-instrument t "piano")]
     (is (= (:key-signature piano2)
            (:key-signature piano)
            {}))))

  (testing "aeolian should be the same as minor"
    (let [s     (score
                 (part "piano"
                   (key-sig [:c :aeolian])))
          t     (score
                  (part "piano"
                    (key-sig [:c :minor])))
         piano  (get-instrument s "piano")
         piano2 (get-instrument t "piano")]
     (is (= (:key-signature piano2)
            (:key-signature piano)
            {:b [:flat] :e [:flat] :a [:flat]})))))
