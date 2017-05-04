(ns alda.parser.variables-test
  (:require [clojure.test :refer :all]
            [alda.parser  :refer (parse-input)]))

(deftest variable-name-tests
  (testing "variable names"
    (testing "must start with two letters"
      (is (= [(alda.lisp/get-variable :aa)]
             (parse-input "aa" :output :events)))
      (is (= [(alda.lisp/get-variable :aaa)]
             (parse-input "aaa" :output :events)))
      (is (= [(alda.lisp/get-variable :HI)]
             (parse-input "HI" :output :events)))
      (is (thrown? Exception (parse-input "x" :output :events)))
      (is (thrown? Exception (parse-input "y2" :output :events)))
      (is (thrown? Exception (parse-input "1234kittens" :output :events)))
      (is (thrown? Exception (parse-input "r2d2" :output :events)))
      (is (thrown? Exception (parse-input "i_like_underscores" :output :events))))
    (testing "can't contain pluses or minuses"
      (is (thrown? Exception (parse-input "jar-jar-binks" :output :events)))
      (is (thrown? Exception (parse-input "han+leia" :output :events)))
      (is (thrown? Exception (parse-input "ionlyprograminc++" :output :events))))
    (testing "can contain digits"
      (is (= [(alda.lisp/get-variable :celloPart2)]
             (parse-input "celloPart2" :output :events)))
      (is (= [(alda.lisp/get-variable :xy42)]
             (parse-input "xy42" :output :events)))
      (is (= [(alda.lisp/get-variable :my20cats)]
             (parse-input "my20cats" :output :events))))
    (testing "can contain underscores"
      (is (= [(alda.lisp/get-variable :apple_cider)]
             (parse-input "apple_cider" :output :events)))
      (is (= [(alda.lisp/get-variable :underscores__are___great____)]
             (parse-input "underscores__are___great____" :output :events))))))

(deftest variable-get-tests
  (testing "variable getting"
    (is (= [(alda.lisp/score
              (alda.lisp/part {:names ["flute"]}
                (alda.lisp/note (alda.lisp/pitch :c))
                (alda.lisp/get-variable :flan)
                (alda.lisp/note (alda.lisp/pitch :f))))]
           (parse-input "flute: c flan f" :output :events)))
    (is (= [(alda.lisp/score
              (alda.lisp/part {:names ["clarinet"]}
                (alda.lisp/get-variable :pudding123)))]
           (parse-input "clarinet: pudding123" :output :events)))))

(deftest variable-set-tests
  (testing "variable setting"
    (testing "within an instrument part"
      (is (= [alda.lisp/score
              (alda.lisp/part {:names ["harpsichord"]}
                              (alda.lisp/set-variable :custard_
                                                      (alda.lisp/note (alda.lisp/pitch :c))
                                                      (alda.lisp/note (alda.lisp/pitch :d))
                                                      (alda.lisp/chord
                                                        (alda.lisp/note (alda.lisp/pitch :e))
                                                        (alda.lisp/note (alda.lisp/pitch :g)))))]
             (parse-input "harpsichord:\n\ncustard_ = c d e/g" :output :events)))
      (is (= [(alda.lisp/score
                (alda.lisp/part {:names ["glockenspiel"]}
                  (alda.lisp/set-variable :sorbet
                    (alda.lisp/note (alda.lisp/pitch :c))
                    (alda.lisp/note (alda.lisp/pitch :d))
                    (alda.lisp/chord
                      (alda.lisp/note (alda.lisp/pitch :e))
                      (alda.lisp/note (alda.lisp/pitch :g))))
                  (alda.lisp/note (alda.lisp/pitch :c))))]
             (parse-input "glockenspiel:\n\nsorbet=c d e/g\nc" :output :events))))
    (testing "at the top of a score"
      (is (= [(alda.lisp/score
                (alda.lisp/set-variable :GELATO
                  (alda.lisp/note (alda.lisp/pitch :d))
                  (alda.lisp/note (alda.lisp/pitch :e)))
                (alda.lisp/part {:names ["clavinet"]}
                  (alda.lisp/chord
                    (alda.lisp/note (alda.lisp/pitch :c))
                    (alda.lisp/note (alda.lisp/pitch :f)))))]
             (parse-input "GELATO=d e\n\nclavinet: c/f" :output :events))))))
