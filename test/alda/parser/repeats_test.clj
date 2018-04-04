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

  (testing "alternate endings/numbered repeats"
    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))]]
           (alda.lisp/times 1
              (alda.lisp/note (alda.lisp/pitch :c))
              [[1]])))

    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/note (alda.lisp/pitch :d))]
            [(alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/note (alda.lisp/pitch :e))]]
           (alda.lisp/times 2
              [(alda.lisp/note (alda.lisp/pitch :c))
               (alda.lisp/note (alda.lisp/pitch :d))
               (alda.lisp/note (alda.lisp/pitch :e))]
              [[1 2] [1] [2]]))

    (is (= [[(alda.lisp/note (alda.lisp/pitch :c))]
            [(alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/note (alda.lisp/pitch :d))
            (alda.lisp/note (alda.lisp/pitch :e))]
            [(alda.lisp/note (alda.lisp/pitch :d))
             (alda.lisp/note (alda.lisp/pitch :e))]
            [(alda.lisp/note (alda.lisp/pitch :c))]]
           (alda.lisp/times 4
              [(alda.lisp/note (alda.lisp/pitch :c))
               [(alda.lisp/note (alda.lisp/pitch :d))
                (alda.lisp/note (alda.lisp/pitch :e))]]
              [[1 2 4] [2 3]])
           ;(parse-input "[c [d e]'2] *4" :output :events)
           )))))


