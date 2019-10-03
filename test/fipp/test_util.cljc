(ns fipp.test-util
  (:require [clojure.string :as str]))

(defn clean [s]
  (-> s
    str/trim
    ;; Force CLJS to JVM's class name behavior.
    (str/replace "fipp.edn-test/" "fipp.edn_test.")
    ;; Use dummy addresses and gensyms.
    (str/replace #"\"0x[a-f0-9]+\"" "\"0xDEADBEEF\"")
    (str/replace #"reify__[0-9]+" "reify__123")))
