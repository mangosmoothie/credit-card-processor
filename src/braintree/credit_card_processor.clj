(ns braintree.credit-card-processor
  (:gen-class)
  (:require
   [braintree.account] ;; for command dispatch
   [braintree.command-interface :as command]
   [braintree.spec :as spec]))

(defn make-report-line [{acc-name :account/name :as account}]
  (let [report-value (if (spec/account-valid? account)
                       (str "$" (:account/balance account))
                       "error")]
    (format "%s: %s" acc-name report-value)))

(defn make-final-report [accounts]
  (->> (keys accounts)
       sort
       (map accounts)
       (map make-report-line)
       (clojure.string/join "\n")))

(defn process-lines
  "Process a sequence of 'commands' to generate an aggregate (report).

  Invalid commands, those that fail to conform to the `:command/command`
  spec, are simply removed / ignored.
  "
  [lines]
  (transduce
   (comp
    (map #(clojure.string/split % #" "))
    (map spec/make-command)
    (remove spec/invalid?))
   (fn
     ([accounts command] (command/execute accounts command))
     ([accounts] (make-final-report accounts)))
   {}
   lines))

(defn -main
  "
  If arguments are present, treat first arg as file path and read
  line by line. Ignore other arguments.

  If no arguments supplied, read line by line from stdin.
  "
  [& args]
  (println
   (if (seq args)
     (with-open [reader (clojure.java.io/reader (first args))]
       (process-lines (line-seq reader)))
     (process-lines (line-seq (java.io.BufferedReader. *in*))))))
