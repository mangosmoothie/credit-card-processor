(ns mangosmoothie.spec
  (:require
   [clojure.set]
   [clojure.spec.alpha :as s]))

(defn luhn-10? [^String cc-number]
  (let [nums (map #(Character/digit % 10) cc-number)
        sum (reduce + (map #(+ (quot % 10) (mod % 10))
                           (map * (reverse nums) (cycle [1 2]))))]
    (zero? (mod sum 10))))

(defn cc-number? [^String cc-number]
  (and (string? cc-number)
       (not (clojure.string/blank? cc-number))
       (every? #(Character/isDigit %) cc-number)
       (> 20 (.length cc-number))
       (luhn-10? cc-number)))

(defn balance-within-limit? [{:account/keys [balance limit]}]
  (<= balance limit))

(defn non-empty? [^String s]
  (not (clojure.string/blank? s)))

(def create-command? #{:command/add})
(def update-command? #{:command/charge :command/credit})
(def command? (clojure.set/union create-command? update-command?))
(def global-max 1000000001) ;; one-billion

(s/def ::natural-nums-limited (s/int-in 0 global-max))
(s/def ::integers-limited (s/int-in (* -1 global-max) global-max))
(s/def :account.command/amount ::natural-nums-limited)
(s/def :account/balance ::integers-limited)
(s/def :account/cc-number (s/with-gen cc-number?
                            #(s/gen #{"5454" "54545454"})))
(s/def :account/limit ::natural-nums-limited)
(s/def :account/name (s/with-gen (s/and string? non-empty?)
                       #(s/gen #{"Tom" "Lisa" "Quincy"})))
(s/def :command/type command?)

(s/def :account/account-fields
  (s/keys :req [:account/balance
                :account/cc-number
                :account/name
                :account/limit]))

(s/def :account/account
  (s/and
   :account/account-fields
   balance-within-limit?))

(s/def :account/accounts
  (s/map-of :account/name :account/account))

;; cc-number is not validated for account creation since we want to
;; include 'error' accounts in the report - therefore invalid cc numbers
;; can be part of a valid account creation request
(s/def :account/create
  (s/cat :command/type create-command?
         :account/name :account/name
         :account/cc-number string?
         :account/limit :account/limit))

(s/def :account/update
  (s/cat :command/type update-command?
         :account/name :account/name
         :account.command/amount :account.command/amount))

(s/def :command/command
  (s/or
   :account/create :account/create
   :account/update :account/update))

(defn invalid? [a] (s/invalid? a))

(defn account-valid? [account]
  (s/valid? :account/account account))

(defn parse-command
  "Simple parser that takes a vector of strings `xs` and relies on
  the fact that all command inputs start with a `command-type` and end
  with a number, `dollars`. These are the only values that need to be
  parsed to something else, other values can be left as strings.

  As per the challenge instuctions, this is not a parsing exercise so
  any invalid numeric data not fitting the pattern `\\$\\d+` will cause
  a `NumberFormatException` or `StringIndexOutOfBoundsException` to be
  thrown. An empty vector will cause a `NullPointerException` as well."
  [xs]
  (let [command-type (s/conform
                      :command/type
                      (keyword "command" (.toLowerCase (first xs))))
        dollars (Long/parseLong (subs (last xs) 1))]
    (-> xs vec (assoc 0 command-type) pop (conj dollars))))

(defn make-command [xs]
  (let [parsed (parse-command xs)
        command (s/conform :command/command parsed)]
    (if (s/invalid? command)
      command
      (val command))))
