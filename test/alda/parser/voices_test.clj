(ns alda.parser.voices-test
  (:require [clojure.test :refer :all]
            [alda.lisp]
            [alda.parser  :refer (parse-input)]))

(deftest voice-tests
  (testing "voices"
    (is (= [(alda.lisp/part {:names ["piano"]})
            (alda.lisp/voice 1)
            (alda.lisp/note (alda.lisp/pitch :a))
            (alda.lisp/note (alda.lisp/pitch :b))
            (alda.lisp/note (alda.lisp/pitch :c))]
           (parse-input "piano: V1: a b c" :output :events)))
    (is (= [(alda.lisp/part {:names ["piano"]})
            (alda.lisp/voice 1)
            (alda.lisp/note (alda.lisp/pitch :a))
            (alda.lisp/note (alda.lisp/pitch :b))
            (alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/voice 2)
            (alda.lisp/note (alda.lisp/pitch :d))
            (alda.lisp/note (alda.lisp/pitch :e))
            (alda.lisp/note (alda.lisp/pitch :f))]
           (parse-input "piano:
                         V1: a b c
                         V2: d e f"
                        :output :events)))
    (is (= [(alda.lisp/part {:names ["piano"]})
            (alda.lisp/voice 1)
            (alda.lisp/note (alda.lisp/pitch :a))
            (alda.lisp/note (alda.lisp/pitch :b))
            (alda.lisp/note (alda.lisp/pitch :c))
            (alda.lisp/barline)
            (alda.lisp/voice 2)
            (alda.lisp/note (alda.lisp/pitch :d))
            (alda.lisp/note (alda.lisp/pitch :e))
            (alda.lisp/note (alda.lisp/pitch :f))]
           (parse-input "piano:
                         V1: a b c | V2: d e f"
                        :output :events)))
    (is (= [(alda.lisp/part {:names ["piano"]})
            (alda.lisp/voice 1)
            (alda.lisp/times 8
              [(alda.lisp/note (alda.lisp/pitch :a))
               (alda.lisp/note (alda.lisp/pitch :b))
               (alda.lisp/note (alda.lisp/pitch :c))])
            (alda.lisp/voice 2)
            (alda.lisp/times 8
              [(alda.lisp/note (alda.lisp/pitch :d))
               (alda.lisp/note (alda.lisp/pitch :e))
               (alda.lisp/note (alda.lisp/pitch :f))])]
           (parse-input "piano:
                         V1: [a b c] *8
                         V2: [d e f] *8"
                        :output :events)))))

