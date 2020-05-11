(ns braintree.account-test
  (:require
   [braintree.account :as account]
   [braintree.spec :as spec]
   [clojure.pprint :refer [pprint]]
   [clojure.set]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :refer [deftest is testing]]))

;; these specs describe the shape and set the bounds for select properties
;; of the multi-dimensional paramaters we will use for generative testing
;; and input/output validation. Can also use these specs for instrumentation,
;; parameter validation, stubbing, & mocking
(s/def :account.test/update-command-params
  (s/cat :account :account/account-fields :amount :account.command/amount))

(defn same-keys? [spec-data]
  (apply = (map #(into #{} (keys %))
                [(-> spec-data :ret)
                 (-> spec-data :args :account)])))

(s/fdef account/charge-account
  :args :account.test/update-command-params
  :ret map?
  :fn same-keys?)

(s/fdef account/credit-account
  :args :account.test/update-command-params
  :ret map?
  :fn same-keys?)

;; dont think we need to promote this to production spec yet
(s/def :account.test/action
  (s/with-gen
    (s/fspec :args (s/cat :account :account/account
                          :amount :account.command/amount)
             :ret map?
             :fn same-keys?)
    #(s/gen #{account/charge-account account/credit-account})))

(s/def :account.test/command
  (s/keys :req [:account/name
                :account.command/amount
                :command/type]))

(defn all-accounts-valid? [accounts]
  (every? #(s/valid? :account/account %) (vals accounts)))

(s/fdef account/command-executor
  :args (s/cat :accounts :account/accounts
               :command :account.test/command
               :action :account.test/action)
  :ret :account/accounts
  :fn #(all-accounts-valid? (-> % :ret)))

;; wrap generative tests with printer and basic validator
(defn print-test-summary [test-run]
  (pprint (stest/summarize-results test-run))
  (mapv #(pprint (:clojure.spec.test.check/ret %)) test-run))

(defn check' [test-run]
  (is (nil? (-> test-run first :failure))
      (print-test-summary test-run)))

(deftest charge-account-tests
  (testing "basic charge-account tests"
    (letfn [(test-charge-account [start-balance charge-amount]
              (:account/balance
               (account/charge-account
                {:account/balance start-balance
                 :account/limit Long/MAX_VALUE}
                charge-amount)))]
      (is (= 100 (test-charge-account 0 100)))
      (is (= -100 (test-charge-account 100 -200)))
      (is (= 200 (test-charge-account -100 300)))
      (is (= 350 (test-charge-account 325 25)))))
  (testing "charge-account limit tests"
    (letfn [(test-charge-account [start-balance limit charge-amount]
              (:account/balance
               (account/charge-account
                {:account/balance start-balance
                 :account/limit limit}
                charge-amount)))]
      (is (= 101 (test-charge-account 100 100 1)))
      (is (= -99 (test-charge-account -100 -100 1)))
      (is (= -99 (test-charge-account -100 -10 1)))
      (is (= 100 (test-charge-account 0 0 100)))
      (is (= 400 (test-charge-account 200 100 200)))))
  (testing "running spec based generative tests"
    (check' (stest/check `account/charge-account))
    ))

(deftest credit-account-tests
  (testing "basic credit-account tests"
    (letfn [(test-credit-account [start-balance credit-amount]
              (:account/balance
               (account/credit-account
                {:account/balance start-balance} credit-amount)))]
      (is (= 100 (test-credit-account 200 100)))
      (is (= -100 (test-credit-account 200 300)))
      (is (= 200 (test-credit-account 100 -100)))))
  (testing "running spec based generative tests"
    (check' (stest/check `account/credit-account))))

(deftest make-account-test
  (testing "can make account"
    (is (clojure.set/subset?
         #{:account/name :account/foo :account/limit}
         (into #{} (keys (account/make-account {:account/name 0
                                                :account/foo 0
                                                :foo/bar 0
                                                :account/limit 0})))))))

(deftest command-executor-test
  (testing "basic command-executor tests"
    (let [valid-account {:account/cc-number "5454"
                         :account/name "test"
                         :account/limit 100
                         :account/balance 0}]
      (is (= true (-> (account/command-executor
                       {"test" valid-account}
                       {:account/name "test"}
                       (fn [a _](assoc a :updated true)))
                      vals
                      first
                      :updated)))
      (is (= nil (-> (account/command-executor
                       {"test" (assoc valid-account
                                      :account/cc-number "invalid")}
                       {:account/name "test"}
                       (fn [a _](assoc a :updated true)))
                      vals
                      first
                      :updated)))))
  (testing "generative tests"
    (check' (stest/check `account/command-executor))))
