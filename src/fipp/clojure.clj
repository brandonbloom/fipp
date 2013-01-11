(ns fipp.clojure)

;;TODO This is all "EDN" ie simple-dispatch, but also need code-dispatch

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

(comment

  (require '[fipp.core :as fipp])
  (require '[fipp.transduce :as t])

  (binding [fipp/*width* 10]
    (->>
      ;(list 1 2 3 4 [:a :b :c :d] 5 6 7 8 9)
      {:foo 1 :bar \c :baz "str"}
      pretty
      fipp/serialize
      fipp/annotate-rights
      fipp/annotate-begins
      fipp/format-nodes
      (t/each print)
      ;(t/each prn)
      ))

)
