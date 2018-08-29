(ns alda.parser.repeats-test
  (:require [clojure.test :refer :all]
            [alda.parser  :refer (parse-input)]))

(deftest repeat-tests
  (testing "repeated events"
    (is (= [(alda.lisp/times 4
              [(alda.lisp/note (alda.lisp/pitch :c))
               (alda.lisp/note (alda.lisp/pitch :d))
               (alda.lisp/note (alda.lisp/pitch :e))])]
           (parse-input "[c d e] *4" :output :events)))
    (is (= [(alda.lisp/times 5
              [(alda.lisp/note (alda.lisp/pitch :c))
               (alda.lisp/octave :up)])]
           (parse-input "[ c > ]*5" :output :events)))
    (is (= [(alda.lisp/times 5
              [(alda.lisp/note (alda.lisp/pitch :c))
               (alda.lisp/octave :up)])]
           (parse-input "[ c > ] * 5" :output :events)))
    (is (= [(alda.lisp/times 7
              (alda.lisp/note
                (alda.lisp/pitch :c)
                (alda.lisp/duration (alda.lisp/note-length 8))))]
           (parse-input "c8*7" :output :events)))
    (is (= [(alda.lisp/times 7
              (alda.lisp/note
                (alda.lisp/pitch :c)
                (alda.lisp/duration (alda.lisp/note-length 8))))]
           (parse-input "c8 *7" :output :events)))
    (is (= [(alda.lisp/times 7
              (alda.lisp/note
                (alda.lisp/pitch :c)
                (alda.lisp/duration (alda.lisp/note-length 8))))]
           (parse-input "c8 * 7" :output :events))))

  (testing "alternate endings/numbered repeats (lisp)"
    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))]]
           (alda.lisp/times 1
              [[1] (alda.lisp/note (alda.lisp/pitch :c))])))

    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/note (alda.lisp/pitch :d))]
            [(alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/note (alda.lisp/pitch :e))]]
           (alda.lisp/times 2
              [(alda.lisp/note (alda.lisp/pitch :c))
               [[1] (alda.lisp/note (alda.lisp/pitch :d))]
               [[2] (alda.lisp/note (alda.lisp/pitch :e))]])))

    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))]
            [(alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/note (alda.lisp/pitch :d))
            (alda.lisp/note (alda.lisp/pitch :e))]
            [(alda.lisp/note (alda.lisp/pitch :d))
             (alda.lisp/note (alda.lisp/pitch :e))]
            [(alda.lisp/note (alda.lisp/pitch :c))]]
           (alda.lisp/times 4
              [[[1 2 4] (alda.lisp/note (alda.lisp/pitch :c))]
               [[2 3]  [(alda.lisp/note (alda.lisp/pitch :d))
                        (alda.lisp/note (alda.lisp/pitch :e))]]])))))

  (testing "alternate endings/numbered repeats (parser)"
    (is (= (alda.lisp/times 1
              [[1] (alda.lisp/note (alda.lisp/pitch :c))])
           (parse-input "[c'1]*1" :output :events)))

    (is (= (alda.lisp/times 2
              [(alda.lisp/note (alda.lisp/pitch :c))
               [[1] (alda.lisp/note (alda.lisp/pitch :d))]
               [[2] (alda.lisp/note (alda.lisp/pitch :e))]])
           (parse-input "[c d'1 e'2]*2" :output :events)))

    (is (= (alda.lisp/times 4
              [[[1 2 4] (alda.lisp/note (alda.lisp/pitch :c))]
               [[2 3]  [(alda.lisp/note (alda.lisp/pitch :d))
                        (alda.lisp/note (alda.lisp/pitch :e))]]])
           (parse-input "[c'1-2,4 [d e]'2-3]*4" :output :events))))


