(ns mangosmoothie.credit-card-processor-test
  (:require
   [mangosmoothie.credit-card-processor :as ccp]
   [clojure.test :refer [deftest is]]))

(def test-data
  ["Add Tom 4111111111111111 $1000"
   "Add Lisa 5454545454545454 $3000"
   "Add Quincy 1234567890123456 $2000"
   "Charge Tom $500"
   "Charge Tom $800"
   "Charge Lisa $7"
   "Credit Lisa $100"
   "Credit Quincy $200"])

(deftest end-to-end-test
  (is (= "Lisa: $-93\nQuincy: error\nTom: $500"
         (ccp/process-lines test-data))))
