(ns alda.parser.variables-test
  (:require [clojure.test      :refer :all]
            [alda.lisp]
            [alda.test-helpers :refer (parse-events parse-events-or-error)]))

(deftest variable-name-tests
  (testing "variable names"
    (testing "must start with two letters"
      (is (= [(alda.lisp/get-variable :aa)] (parse-events "aa")))
      (is (= [(alda.lisp/get-variable :aaa)] (parse-events "aaa")))
      (is (= [(alda.lisp/get-variable :HI)] (parse-events "HI")))
      (is (thrown? Exception (parse-events-or-error "x")))
      (is (thrown? Exception (parse-events-or-error "y2")))
      (is (thrown? Exception (parse-events-or-error "1234kittens")))
      (is (thrown? Exception (parse-events-or-error "i_like_underscores"))))
    (testing "can't contain pluses or minuses"
      (is (thrown? Exception (parse-events-or-error "jar-jar-binks")))
      (is (thrown? Exception (parse-events-or-error "han+leia")))
      (is (thrown? Exception (parse-events-or-error "ionlyprograminc++"))))
    (testing "can contain digits"
      (is (= [(alda.lisp/get-variable :celloPart2)]
             (parse-events "celloPart2")))
      (is (= [(alda.lisp/get-variable :xy42)]
             (parse-events "xy42")))
      (is (= [(alda.lisp/get-variable :my20cats)]
             (parse-events "my20cats"))))
    (testing "can contain underscores"
      (is (= [(alda.lisp/get-variable :apple_cider)]
             (parse-events "apple_cider")))
      (is (= [(alda.lisp/get-variable :underscores__are___great____)]
             (parse-events "underscores__are___great____"))))))

(deftest variable-get-tests
  (testing "variable getting"
    (is (= [(alda.lisp/part {:names ["flute"]})
            (alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/get-variable :flan)
            (alda.lisp/note (alda.lisp/pitch :f))]
           (parse-events "flute: c flan f")))
    (is (= [(alda.lisp/part {:names ["clarinet"]})
            (alda.lisp/get-variable :pudding123)]
           (parse-events "clarinet: pudding123")))))

(deftest variable-set-tests
  (testing "variable setting"
    (testing "within an instrument part"
      (is (= [(alda.lisp/part {:names ["harpsichord"]})
              (alda.lisp/set-variable :custard_
                (alda.lisp/note (alda.lisp/pitch :c))
                (alda.lisp/note (alda.lisp/pitch :d))
                (alda.lisp/chord
                  (alda.lisp/note (alda.lisp/pitch :e))
                  (alda.lisp/note (alda.lisp/pitch :g))))]
             (parse-events "harpsichord:\n\ncustard_ = c d e/g")))
      (is (= [(alda.lisp/part {:names ["glockenspiel"]})
              (alda.lisp/set-variable :sorbet
                (alda.lisp/note (alda.lisp/pitch :c))
                (alda.lisp/note (alda.lisp/pitch :d))
                (alda.lisp/chord
                  (alda.lisp/note (alda.lisp/pitch :e))
                  (alda.lisp/note (alda.lisp/pitch :g))))
              (alda.lisp/note (alda.lisp/pitch :c))]
             (parse-events "glockenspiel:\n\nsorbet=c d e/g\nc"))))
    (testing "at the top of a score"
      (is (= [(alda.lisp/set-variable :GELATO
                (alda.lisp/note (alda.lisp/pitch :d))
                (alda.lisp/note (alda.lisp/pitch :e)))
              (alda.lisp/part {:names ["clavinet"]})
              (alda.lisp/chord
                (alda.lisp/note (alda.lisp/pitch :c))
                (alda.lisp/note (alda.lisp/pitch :f)))]
             (parse-events "GELATO=d e\n\nclavinet: c/f")))
      (is (= [(alda.lisp/set-variable :cheesecake
                (alda.lisp/cram
                  (alda.lisp/chord
                    (alda.lisp/note (alda.lisp/pitch :c))
                    (alda.lisp/note (alda.lisp/pitch :e)))
                  (alda.lisp/duration (alda.lisp/note-length 2))))]
             (parse-events "cheesecake = { c/e }2")))
      ;; Regression tests for https://github.com/alda-lang/alda-core/issues/64
      ;; NB: the trailing newline was essential to reproducing the issue!
      (testing "and ending with a variable reference"
        (is (= [(alda.lisp/set-variable :satb
                  (alda.lisp/voice 1)
                  (alda.lisp/get-variable :soprano)
                  (alda.lisp/voice 2)
                  (alda.lisp/get-variable :alto)
                  (alda.lisp/voice 3)
                  (alda.lisp/get-variable :tenor)
                  (alda.lisp/voice 4)
                  (alda.lisp/get-variable :bass))]
               (parse-events
                 "satb = V1: soprano V2: alto V3: tenor V4: bass\n")))
        (is (= [(alda.lisp/set-variable :foo
                  (alda.lisp/get-variable :bar))]
               (parse-events "foo = bar\n")))))))
