(ns fipp.clojure)

(defmulti pretty class)

(defmethod pretty :default [x]
  [:text (str x)])

(defmethod pretty clojure.lang.IPersistentVector [v]
  [:group "[" [:nest 1 (interpose :line (map pretty v)) ] "]"])


(comment

  (require '[fipp.core :as fipp])
  (require '[fipp.transduce :as t])

  (->>
    (vec (range 3))
    pretty
    fipp/serialize
    ;fipp/annotate-rights
    ;fipp/annotate-begins
    ;fipp/format-nodes
    ;(t/each print)
    (t/each prn)
    )

)
