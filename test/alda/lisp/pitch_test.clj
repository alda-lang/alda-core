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

(deftest midi-note-tests
  (testing "midi-note specifies a pitch as a MIDI note number"
    (doseq [n [14 42 60 75]]
      (is (= n (determine-midi-note (midi-note n) 4 {} 0))))))

(deftest ref-pitch-tests
  (testing "reference pitch/tuning constant attribute"
    (is (== 430 (calculate-pitch :a [] 4 {} {:ref-pitch 430})))
    (is (> (calculate-pitch :c [] 4 {})
           (calculate-pitch :c [] 4 {} {:ref-pitch 430})))

    ; default A4 pitch is 440 Hz
    (let [s       (score
                    (part "piano"))
          piano   (get-instrument s "piano")]

      (is (== 440 (:reference-pitch piano)))

      (let [s     (continue s
                    (tuning-constant 430))
            piano (get-instrument s "piano")]
        (is (== 430 (:reference-pitch piano)))))))

(deftest transposition-tests
  (testing "transposition attribute"
    (is (== (calculate-pitch :a [] 4 {})
            (calculate-pitch :g [] 4 {} {:transpose 2})
            (calculate-pitch :c [] 5 {} {:transpose -3})))
    (is (== (calculate-pitch :c [] 4 {})
            (calculate-pitch :b [] 3 {} {:transpose 1})
            (calculate-pitch :c [:sharp] 4 {} {:transpose -1})
            (calculate-pitch :c [] 4 {:c [:sharp]} {:transpose -1})))

    (let [s       (score
                    (part "piano"))
          piano   (get-instrument s "piano")]
      (is (== 0 (:transposition piano)))

      (let [s     (continue s
                    (transpose 2))
            piano (get-instrument s "piano")]
        (is (== 2 (:transposition piano))))))
  (testing "transposing notes specified as MIDI note numbers"
    (doseq [n [14 42 60 75]]
      (is (= (+ n 5)
             (determine-midi-note (midi-note n) 4 {} 5))))))

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
     (is (= {:b [:flat] :e [:flat] :a [:flat] :d [:flat]}
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
