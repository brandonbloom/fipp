(ns bbloom.fipp.edn
  "Provides a pretty document serializer and pprint fn for Clojure/EDN forms.
  See bbloom.fipp.clojure for pretty printing Clojure code."
  (:require [bbloom.fipp.printer :as printer :refer (defprinter)]))

(defmulti pretty class)

(defmethod pretty :default [x]
  [:text (pr-str x)])

(defmethod pretty clojure.lang.IPersistentVector [v]
  [:group "[" [:nest 1 (interpose :line (map pretty v))] "]"])

(defmethod pretty clojure.lang.ISeq [s]
  [:group "(" [:nest 1 (interpose :line (map pretty s))] ")"])

(defmethod pretty clojure.lang.IPersistentMap [m]
  (let [kvps (map (fn [[k v]]
                    [:span (pretty k) " " (pretty v)])
                  m)]
    [:group "{" [:nest 1 (interpose [:span "," :line] kvps)]  "}"]))

(defmethod pretty clojure.lang.IPersistentSet [s]
  [:group "#{" [:nest 2 (interpose :line (map pretty s))] "}"])

;clojure.lang.PersistentQueue pprint-pqueue)
;clojure.lang.IDeref pprint-ideref)

(defprinter pprint pretty
  {:width 70})

(comment

  (->
    ;(list 1 2 3 4 [:a :b :c :d] 5 6 7 8 9)
    {:foo 1 :bar \c :baz "str"}
    (pprint {:width 5}))

)
