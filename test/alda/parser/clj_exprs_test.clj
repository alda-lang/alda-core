(ns alda.parser.clj-exprs-test
  (:require [clojure.test :refer :all]
            [alda.lisp    :refer :all]
            [alda.parser  :refer (parse-input)]))

(deftest attribute-tests
  (testing "volume change"
    (is (= [(volume 50)] (parse-input "(volume 50)" :output :events))))
  (testing "tempo change"
    (is (= [(tempo 100)] (parse-input "(tempo 100)" :output :events))))
  (testing "quantization change"
    (is (= [(quant 75)] (parse-input "(quant 75)" :output :events))))
  (testing "panning change"
    (is (= [(panning 0)] (parse-input "(panning 0)" :output :events)))))

(deftest multiple-attribute-change-tests
  (testing "attribute changes"
    (is (= [(list (vol 50) (tempo 100))]
           (parse-input "(list (vol 50) (tempo 100))" :output :events)))
    (is (= [(list (quant! 50) (tempo 90))]
           (parse-input "(list (quant! 50) (tempo 90))" :output :events))))
  (testing "global attribute changes"
    (is (= [(tempo! 126)]
           (parse-input "(tempo! 126)" :output :events)))
    (is (= [(list (tempo! 130) (quant! 80))]
           (parse-input "(list (tempo! 130) (quant! 80))" :output :events)))))

(deftest comma-and-semicolon-tests
  (testing "commas/semicolons can exist in strings"
    (is (= [:nil]
           (parse-input "(println \"hi; hi, hi\")" :output :events))))
  (testing "commas inside [brackets] and {braces} won't break things"
    (is (= [:nil]
           (parse-input "(prn [1,2,3])" :output :events)))
    (is (= [:nil]
           (parse-input "(prn {:a 1, :b 2})" :output :events))))
  (testing "comma/semicolon character literals are OK too"
    (is (= [:nil]
           (parse-input "(println \\, \\;)" :output :events)))))

(deftest paren-tests
  (testing "parens inside of a string are NOT a clj-expr"
    (is (= [:nil]
           (parse-input "(prn \"a string (with parens)\")" :output :events)))
    (is (= [:nil]
           (parse-input "(prn \"a string with just a closing paren)\")" :output :events))))
  (testing "paren character literals don't break things"
    (is (= [:nil]
           (parse-input "(prn \\()" :output :events)))
    (is (= [:nil]
           (parse-input "(prn \\))" :output :events)))
    (is (= [:nil]
           (parse-input "(prn \\( (+ 1 1) \\))" :output :events)))))

(deftest vector-tests
  (testing "vectors are a thing"
    (is (= [:nil]
           (parse-input "(prn [1 2 3 \\a :b \"c\"])" :output :events))))
  (testing "vectors can have commas in them"
    (is (= [:nil]
           (parse-input "(prn [1, 2, 3])" :output :events)))))

(deftest map-tests
  (testing "maps are a thing"
    (is (= [:nil]
           (parse-input "(prn {:a 1 :b 2 :c 3})" :output :events))))
  (testing "maps can have commas in them"
    (is (= [:nil]
           (parse-input "(prn {:a 1, :b 2, :c 3})" :output :events)))))

(deftest set-tests
  (testing "sets are a thing"
    (is (= [:nil]
           (parse-input "(prn #{1 2 3})" :output :events))))
  (testing "sets can have commas in them"
    (is (= [:nil]
           (parse-input "(prn #{1, 2, 3})" :output :events)))))

(deftest nesting-things
  (testing "things can be nested and it won't break shit"
    (is (= [:nil]
           (parse-input "(prn [1 2 [3 4] 5])" :output :events)))
    (is (= [:nil]
           (parse-input "(prn #{1 2 #{3 4} 5})" :output :events)))
    (is (= [:nil]
           (parse-input "(prn (list 1 [2 {3 #{4 5}}]))" :output :events)))))

