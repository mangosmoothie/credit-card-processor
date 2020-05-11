(ns braintree.account
  (:require
   [braintree.command-interface :as command]
   [braintree.spec :as spec]))

(defn command-executor
  "Executes account updates - injects account validation

  Updates an account with `command` (credit or charge) in the accounts
  backing store in a 'transaction'. Account validation occurrs at point
  of execution and will bail the transaction with no-op if account is
  invalid. This check occurs before the `command` is applied and
  also on the result of `command` before updating the accounts store.

  This could easily transition to multithreaded or distributed model
  by adjusting this executor; for example, move `update` to `swap!` for
  use with the Clojure concurrency primitive `atom`
  "
  [accounts
   {account-name :account/name
    amount :account.command/amount}
   action]
  (let [account (get accounts account-name)]
    (if (spec/account-valid? account)
      (let [updated-account (action account amount)]
        (if (spec/account-valid? updated-account)
          (assoc accounts account-name updated-account)
          accounts))
      accounts)))

(defn charge-account [account amount]
  (update account :account/balance + amount))

(defn credit-account [account amount]
  (update account :account/balance - amount))

(def account-defaults
  "all accounts start with balance set to 0"
  {:account/balance 0})

(defn make-account
  "Create account by pulling 'account' keys from command and merging
  with account defaults: i.e. initializer"
  [command]
  (->> command
       (filter (fn [[k _]] (= "account" (namespace k))))
       (into {})))

(defmethod command/execute :command/charge [accounts command]
  (command-executor accounts command charge-account))

(defmethod command/execute :command/credit [accounts command]
  (command-executor accounts command credit-account))

(defmethod command/execute :command/add
  [accounts {account-name :account/name :as command}]
  (assoc accounts account-name
         (merge
          (make-account command)
          account-defaults)))
