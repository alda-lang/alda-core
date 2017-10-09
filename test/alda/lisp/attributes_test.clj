(ns alda.lisp.attributes-test
  (:require [clojure.test      :refer :all]
            [alda.test-helpers :refer (get-instrument)]
            [alda.lisp         :refer :all]))

(deftest octave-tests
  (testing "octaves"
    (let [s     (score (part "piano"))
          piano (get-instrument s "piano")]
      (is (= (:octave piano) 4))

      (let [s     (continue s
                    (octave 2))
            piano (get-instrument s "piano")]
        (is (= (:octave piano) 2)))

      (let [s     (continue s
                    (octave :down))
            piano (get-instrument s "piano")]
        (is (= (:octave piano) 3)))

      (let [s     (continue s
                    (octave :up))
            piano (get-instrument s "piano")]
        (is (= (:octave piano) 5)))

      (let [s     (continue s
                    (octave :up)
                    (octave :up)
                    (octave :up)
                    (octave :down))
            piano (get-instrument s "piano")]
        (is (= (:octave piano) 6)))

      (let [s     (continue s
                    (set-attribute :octave 1))
            piano (get-instrument s "piano")]
        (is (= (:octave piano) 1))))))

(deftest volume-tests
  (testing "volume"
    (let [s     (score (part "piano"))
          piano (get-instrument s "piano")]
      (is (== (:volume piano) 1.0))

      (let [s     (continue s
                    (volume 50))
            piano (get-instrument s "piano")]
        (is (== (:volume piano) 0.5)))

      (let [s     (continue s
                    (volume 75))
            piano (get-instrument s "piano")]
        (is (== (:volume piano) 0.75)))

      (let [s     (continue s
                    (set-attribute :volume 81))
            piano (get-instrument s "piano")]
        (is (== (:volume piano) 0.81))))))

(deftest panning-tests
  (testing "panning"
    (let [s     (score (part "piano"))
          piano (get-instrument s "piano")]
      (is (== (:panning piano) 0.5))

      (let [s     (continue s
                    (panning 25))
            piano (get-instrument s "piano")]
        (is (== (:panning piano) 0.25)))

      (let [s     (continue s
                    (panning 75))
            piano (get-instrument s "piano")]
        (is (== (:panning piano) 0.75)))

      (let [s     (continue s
                    (set-attribute :panning 81))
            piano (get-instrument s "piano")]
        (is (== (:panning piano) 0.81))))))

(deftest quantization-tests
  (testing "quantization"
    (let [s     (score (part "piano"))
          piano (get-instrument s "piano")]
      (is (== (:quantization piano) 0.9))

      (let [s     (continue s
                    (quant 50))
            piano (get-instrument s "piano")]
        (is (== (:quantization piano) 0.50)))

      (let [s     (continue s
                    (quantize 75))
            piano (get-instrument s "piano")]
        (is (== (:quantization piano) 0.75)))

      (let [s     (continue s
                    (quantization 9001))
            piano (get-instrument s "piano")]
        (is (== (:quantization piano) 90.01)))

      (let [s     (continue s
                    (set-attribute :quant 81))
            piano (get-instrument s "piano")]
        (is (== (:quantization piano) 0.81)))

      (let [s     (continue s
                    (set-attribute :quantize 82))
            piano (get-instrument s "piano")]
        (is (== (:quantization piano) 0.82)))

      (let [s     (continue s
                    (set-attribute :quantization 83))
            piano (get-instrument s "piano")]
        (is (== (:quantization piano) 0.83))))))

(deftest note-length-tests
  (testing "note-length"
    (let [s     (score (part "piano"))
          piano (get-instrument s "piano")]
      ; default note length is a quarter note (1 beat)
      (is (== (:beats (:duration piano)) 1))

      (let [s     (continue s
                    (set-duration (note-length 2 {:dots 2})))
            piano (get-instrument s "piano")]
        (is (== (:beats (:duration piano)) 3.5)))

      (let [s     (continue s
                    (set-note-length 1))
            piano (get-instrument s "piano")]
        (is (== (:beats (:duration piano)) 4)))

      (let [s     (continue s
                    (set-note-length "2.."))
            piano (get-instrument s "piano")]
        (is (== (:beats (:duration piano)) 3.5)))

      (let [s     (continue s
                    (note (pitch :c)
                          (duration (note-length 1) (note-length 1))))
            piano (get-instrument s "piano")]
        (is (== (:beats (:duration piano)) 8))))))

(deftest tempo-tests
  (testing "tempo"
    ;; test different representations of tempo
    (is (== (:val (tempo 60))
            (:val (tempo 2 30))
            (:val (tempo "4." 40))
            (:val (tempo "8.~16~4" 30))))

    (let [s     (score (part "piano"))
          piano (get-instrument s "piano")]
      ;; default tempo is 120
      (is (== (:tempo piano) 120))

      (let [s     (continue s
                    (tempo 60))
            piano (get-instrument s "piano")]
        (is (== (:tempo piano) 60)))))

  (testing "metric-modulation")
    (let [s     (score (part "piano"))
          piano (get-instrument s "piano")]

      ;; ratio of (half : dotted quarter) = 4/3
      ;; (* 120 = 160)
      (let [s     (continue s
                    (metric-modulation "4." 2))
            piano (get-instrument s "piano")]
        (is (== (:tempo piano) 160)))

      (let [s     (continue s
                    (tempo 60)
                    (metric-modulation 4 8))
            piano (get-instrument s "piano")]
        (is (== (:tempo piano) 30)))))

