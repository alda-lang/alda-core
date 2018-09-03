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
    (is (= [(alda.lisp/times 3
              [[[1 3] (alda.lisp/note (alda.lisp/pitch :c))]])]
           (parse-input "[c'1,3]*3" :output :events)))

    (is (= [(alda.lisp/times 2
              [(alda.lisp/note (alda.lisp/pitch :c))
               [[1] (alda.lisp/note (alda.lisp/pitch :d))]
               [[2] (alda.lisp/note (alda.lisp/pitch :e))]])]
           (parse-input "[c d'1 e'2]*2" :output :events)))

    (is (= [(alda.lisp/times 4
              [[[1 2 4] (alda.lisp/note (alda.lisp/pitch :c))]
               [[2 3]  [(alda.lisp/note (alda.lisp/pitch :d))
                        (alda.lisp/note (alda.lisp/pitch :e))]]])]
           (parse-input "[c'1-2,4 [d e]'2-3]*4" :output :events)))

    (is (= [(alda.lisp/times 4
              [[[1 3] (alda.lisp/cram
                        (alda.lisp/note (alda.lisp/pitch :c))
                        (alda.lisp/note (alda.lisp/pitch :d))
                        (alda.lisp/note (alda.lisp/pitch :e))
                        (alda.lisp/duration (alda.lisp/note-length 2)))]
               [[2 3 4] [(alda.lisp/note (alda.lisp/pitch :f))
                       (alda.lisp/pause (alda.lisp/duration (alda.lisp/note-length 8)))
                       (alda.lisp/octave :up)
                       (alda.lisp/note (alda.lisp/pitch :g))]]])]
           (parse-input "[{c d e}2'1,3 [f r8 > g]'2-4]*4" :output :events))))

  (testing "alternate endings parse errors"
    (is (thrown-with-msg? Exception #"Unexpected"
          (parse-input "[c'1,4-]*4" :output :events-or-error)))

    (is (thrown-with-msg? Exception #"Unexpected"
          (parse-input "[c'1,-]*4" :output :events-or-error)))

    (is (thrown-with-msg? Exception #"Invalid range"
          (parse-input "[c'4-1 d]*4" :output :events-or-error)))

    (is (thrown-with-msg? Exception #"Invalid range"
          (parse-input "[c'1,2-3,4-1 d]*4" :output :events-or-error)))

    (is (thrown-with-msg? Exception #"Unexpected"
          (parse-input "c'1,2-4*4" :output :events-or-error)))

    (is (thrown-with-msg? Exception #"is not repeated"
          (parse-input "[c'1,2 d'2-3]" :output :events-or-error)))

    (is (thrown-with-msg? Exception #"is not repeated"
          (parse-input "[[c'1,2 d'1,3]*3 e'2-3]" :output :events-or-error)))

    (is (thrown-with-msg? Exception #"is not repeated"
          (parse-input "[[c'1,2 d'1,3] e'2-3]*3" :output :events-or-error)))))




