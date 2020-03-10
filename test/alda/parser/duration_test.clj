(ns alda.parser.duration-test
  (:require [clojure.test :refer :all]
            [alda.lisp]
            [alda.parser  :refer (parse-input)]))

(deftest duration-tests
  (testing "duration"
    (is (= (parse-input "c2" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/note-length 2)))]))
    (is (= (parse-input "c0.5" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/note-length 0.5)))]))))

(deftest dot-tests
  (testing "dots"
    (is (= (parse-input "c2.." :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/note-length 2 {:dots 2})))]))
    (is (= (parse-input "c0.5.." :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/note-length 0.5 {:dots 2})))]))))

(deftest millisecond-duration-tests
  (testing "duration in milliseconds"
    (is (= (parse-input "c450ms" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/ms 450)))]))))

(deftest second-duration-tests
  (testing "duration in seconds"
    (is (= (parse-input "c2s" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/ms 2000)))]))))

(deftest tie-and-slur-tests
  (testing "ties"
    (is (= (parse-input "c1~2~4" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/note-length 1)
                                  (alda.lisp/note-length 2)
                                  (alda.lisp/note-length 4)))]))
    (is (= (parse-input "c1.5~2.5~4" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/note-length 1.5)
                                  (alda.lisp/note-length 2.5)
                                  (alda.lisp/note-length 4)))]))
    (is (= (parse-input "c500ms~350ms" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/ms 500)
                                  (alda.lisp/ms 350)))]))
    (is (= (parse-input "c5s~4~350ms" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/ms 5000)
                                  (alda.lisp/note-length 4)
                                  (alda.lisp/ms 350)))])))
  (testing "slurs"
    (are
      [input]
      (= (parse-input input :output :events)
         [(alda.lisp/note
            (alda.lisp/pitch :c)
            (alda.lisp/duration (alda.lisp/note-length 4))
            :slur)])
      "c4~"
      "c4~\n"
      "c4~|"
      "c4~|\n"
      "c4~ |"
      "c4~ |\n")
    (is (= (parse-input "c420ms~" :output :events)
           [(alda.lisp/note
              (alda.lisp/pitch :c)
              (alda.lisp/duration (alda.lisp/ms 420))
              :slur)]))))

