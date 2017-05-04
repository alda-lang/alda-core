(ns alda.parser.clj-exprs-test
  (:require [clojure.test :refer :all]
            [alda.lisp    :refer :all]
            [alda.parser  :refer (parse-input)]))

(deftest attribute-tests
  (testing "volume change"
    (is (= (parse-input "(volume 50)" :output :events) [(volume 50)])))
  (testing "tempo change"
    (is (= (parse-input "(tempo 100)" :output :events) [(tempo 100)])))
  (testing "quantization change"
    (is (= (parse-input "(quant 75)" :output :events) [(quant 75)])))
  (testing "panning change"
    (is (= (parse-input "(panning 0)" :output :events) [(panning 0)]))))

(deftest multiple-attribute-change-tests
  (testing "attribute changes"
    (is (= (parse-input "(list (vol 50) (tempo 100))" :output :events)
           [(list (vol 50) (tempo 100))]))
    (is (= (parse-input "(list (quant! 50) (tempo 90))" :output :events)
           [(list (quant! 50) (tempo 90))])))
  (testing "global attribute changes"
    (is (= (parse-input "(tempo! 126)" :output :events)
           [(tempo! 126)]))
    (is (= (parse-input "(list (tempo! 130) (quant! 80))" :output :events)
           [(list (tempo! 130) (quant! 80))]))))

(deftest comma-and-semicolon-tests
  (testing "commas/semicolons can exist in strings"
    (is (= (parse-input "(println \"hi; hi, hi\")" :output :events)
           [(println "hi; hi, hi")])))
  (testing "commas inside [brackets] and {braces} won't break things"
    (is (= (parse-input "(prn [1,2,3])" :output :events)
           [(prn [1 2 3])]))
    (is (= (parse-input "(prn {:a 1, :b 2})" :output :events)
           [(prn {:a 1 :b 2})])))
  (testing "comma/semicolon character literals are OK too"
    (is (= (parse-input "(println \\, \\;)" :output :events)
           [(println \, \;)]))))

(deftest paren-tests
  (testing "parens inside of a string are NOT a clj-expr"
    (is (= (parse-input "(prn \"a string (with parens)\")" :output :events)
           [(prn "a string (with parens)")]))
    (is (= (parse-input "(prn \"a string with just a closing paren)\")" :output :events)
           [(prn "a string with just a closing paren)")])))
  (testing "paren character literals don't break things"
    (is (= (parse-input "(prn \\()" :output :events)
           [(prn \()]))
    (is (= (parse-input "(prn \\))" :output :events)
           [(prn \))]))
    (is (= (parse-input "(prn \\( (+ 1 1) \\))" :output :events)
           [(prn \( (+ 1 1) \))]))))

(deftest vector-tests
  (testing "vectors are a thing"
    (is (= (parse-input "(prn [1 2 3 \\a :b \"c\"])" :output :events)
           [(prn [1 2 3 \a :b "c"])])))
  (testing "vectors can have commas in them"
    (is (= (parse-input "(prn [1, 2, 3])" :output :events)
           [(prn [1 2 3])]))))

(deftest map-tests
  (testing "maps are a thing"
    (is (= (parse-input "(prn {:a 1 :b 2 :c 3})" :output :events)
           [(prn {:a 1 :b 2 :c 3})])))
  (testing "maps can have commas in them"
    (is (= (parse-input "(prn {:a 1, :b 2, :c 3})" :output :events)
           [(prn {:a 1 :b 2 :c 3})]))))

(deftest set-tests
  (testing "sets are a thing"
    (is (= (parse-input "(prn #{1 2 3})" :output :events)
           [(prn #{1 2 3})])))
  (testing "sets can have commas in them"
    (is (= (parse-input "(prn #{1, 2, 3})" :output :events)
           [(prn #{1 2 3})]))))

(deftest nesting-things
  (testing "things can be nested and it won't break shit"
    (is (= (parse-input "(prn [1 2 [3 4] 5])" :output :events)
           [(prn [1 2 [3 4] 5])]))
    (is (= (parse-input "(prn #{1 2 #{3 4} 5})" :output :events)
           [(prn #{1 2 #{3 4} 5})]))
    (is (= (parse-input "(prn (+ 1 [2 {3 #{4 5}}]))" :output :events)
           [(prn (+ 1 [2 {3 #{4 5}}]))]))))

