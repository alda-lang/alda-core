(ns alda.parser.repeats-test
  (:require [clojure.test :refer :all]
            [alda.lisp    :as    a]
            [alda.parser  :refer (parse-input)]))

(deftest repeat-tests
  (testing "repeated events"
    (is (= [(a/times 4
              [(a/note (a/pitch :c))
               (a/note (a/pitch :d))
               (a/note (a/pitch :e))])]
           (parse-input "[c d e] *4" :output :events)))
    (is (= [(a/times 5
              [(a/note (a/pitch :c))
               (a/octave :up)])]
           (parse-input "[ c > ]*5" :output :events)))
    (is (= [(a/times 5
              [(a/note (a/pitch :c))
               (a/octave :up)])]
           (parse-input "[ c > ] * 5" :output :events)))
    (is (= [(a/times 7
              (a/note
                (a/pitch :c)
                (a/duration (a/note-length 8))))]
           (parse-input "c8*7" :output :events)))
    (is (= [(a/times 7
              (a/note
                (a/pitch :c)
                (a/duration (a/note-length 8))))]
           (parse-input "c8 *7" :output :events)))
    (is (= [(a/times 7
              (a/note
                (a/pitch :c)
                (a/duration (a/note-length 8))))]
           (parse-input "c8 * 7" :output :events))))

  (testing "alternate endings/numbered repeats"
    (is (= [(a/times 3
              [[[1 3] (a/note (a/pitch :c))]])]
           (parse-input "[c'1,3]*3" :output :events)))

    (is (= [(a/times 2
              [(a/note (a/pitch :c))
               [[1] (a/note (a/pitch :d))]
               [[2] (a/note (a/pitch :e))]])]
           (parse-input "[c d'1 e'2]*2" :output :events)))

    (is (= [(a/times 4
              [[[1 2 4] (a/note (a/pitch :c))]
               [[2 3]  [(a/note (a/pitch :d))
                        (a/note (a/pitch :e))]]])]
           (parse-input "[c'1-2,4 [d e]'2-3]*4" :output :events)))

    (is (= [(a/times 4
              [[[1 3] (a/cram
                        (a/note (a/pitch :c))
                        (a/note (a/pitch :d))
                        (a/note (a/pitch :e))
                        (a/duration (a/note-length 2)))]
               [[2 3 4] [(a/note (a/pitch :f))
                         (a/pause (a/duration (a/note-length 8)))
                         (a/octave :up)
                         (a/note (a/pitch :g))]]])]
           (parse-input "[{c d e}2'1,3 [f r8 > g]'2-4]*4" :output :events)))

    (is (= [(a/times 1
              [(a/note (a/pitch :c))
               [[1] (a/note (a/pitch :d))]])]
           (parse-input "[[c] [d]'1]*1" :output :events))))

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
