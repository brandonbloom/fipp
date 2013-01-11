(ns fipp.clojure)

(defmulti pretty class)

(defmethod pretty :default [x]
  [:text (str x)])

(defmethod pretty clojure.lang.IPersistentVector [v]
  ;TODO nest 1
  [:group "[" (interpose :line (map pretty v)) "]"])


(comment

  (require '[fipp.core :as fipp])
  (require '[fipp.transduce :as t])

  (->>
    (vec (range 30))
    pretty
    fipp/serialize
    fipp/annotate-rights
    fipp/annotate-begins
    fipp/format-nodes
    (t/each print))

)
