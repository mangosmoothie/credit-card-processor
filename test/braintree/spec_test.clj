(ns braintree.spec-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [braintree.spec :as spec]))

(deftest luhn-10?-test
  (is (= true (spec/luhn-10? "5454")))
  (is (= true (spec/luhn-10? "54545454")))
  (is (= true (spec/luhn-10? "5454545454545454")))
  (is (= true (spec/luhn-10? "4111111111111111")))
  ;; interesting cases - but we don't validate input in luhn-10?
  (is (= true (spec/luhn-10? "invalid")))
  (is (= true (spec/luhn-10? "545454545454545454545454")))
  (is (= true (spec/luhn-10? "")))

  (is (= false (spec/luhn-10? "1234567890123456")))
  )

(deftest cc-number?-test
  (is (= true (spec/cc-number? "5454")))
  (is (= true (spec/cc-number? "54545454")))
  (is (= true (spec/cc-number? "5454545454545454")))
  (is (= true (spec/cc-number? "4111111111111111")))
  ;; cc-number? catches these
  (is (= false (spec/cc-number? "invalid")))
  (is (= false (spec/cc-number? "")))
  (is (= false (spec/cc-number? "1234567890123456")))
  (is (= false (spec/cc-number? "545454545454545454545454"))))
