(ns braintree.command-interface)

(defmulti execute
  "functional polymorphism - dispatch on command type"
  (fn [_ {command :command/type}] command))
