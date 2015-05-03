(ns fipp.ednize
  (:require [clojure.string :as s]))

(defprotocol IEdn
  "Perform a shallow conversion to an Edn data structure."
  (-edn [x]))

;;TODO contribute to clj & cljs
(defn boolean? [x]
  (or (true? x) (false? x)))

;;TODO contribute to clj & cljs
(defn pattern? [x]
  (instance? js/RegExp x))

;;XXX contribute to cljs
(defn tagged-literal [tag rep]
  [:TAGGED tag rep])

(defn record->tagged [x]
  (tagged-literal (s/split (-> x type pr-str) #"/" 2)
                  (into {} x)))
