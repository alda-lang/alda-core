(ns alda.lisp.repeats-test
  (:require [clojure.test      :refer :all]
            [alda.lisp         :refer :all]))

(deftest repeats-test
  (testing "alternate endings/numbered repeats"
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
                        (alda.lisp/note (alda.lisp/pitch :e))]]]))))

  (testing "alternate endings range errors"
    (is (thrown? AssertionError
          (alda.lisp/times 3
            [[[0 2] (alda.lisp/note (alda.lisp/pitch :c))]
             [[1 3] (alda.lisp/note (alda.lisp/pitch :d))]])))

    (is (thrown? AssertionError
          (alda.lisp/times 3
            [[[1 2] (alda.lisp/note (alda.lisp/pitch :c))]
             [[2 4] (alda.lisp/note (alda.lisp/pitch :d))]])))))
