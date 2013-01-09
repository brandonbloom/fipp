(ns pprint.core
  "Linear, bounded, functional pretty-printing.
  See Doitse Swierstra and Olaf Chitil
  Journal of Functional Programming, 19(1):1-16, January 2009."
  (:require [clojure.string :as s]
            [clojure.data.finger-tree :refer (double-list)]))

(defmulti serialize-node first)

(defn serialize [doc]
  (cond
    (seq? doc) (mapcat serialize doc)
    (string? doc) [doc]
    (vector? doc) (serialize-node doc)
    (keyword? doc) (serialize-node [doc])
    :else (throw (Exception. "Unexpected class for doc node"))))

(defmethod serialize-node :text [[_ & text]]
  [(apply str text)])

(defmethod serialize-node :span [[_ & children]]
  (serialize children))

(defmethod serialize-node :line [[_ inline]]
  (let [inline (or inline " ")]
    (assert (string? inline))
    [[:line inline]]))

(defmethod serialize-node :group [[_ & children]]
  (concat [:begin] (serialize children) [:end]))

(defmethod serialize-node :nest [[_ n & children]]
  (assert (integer? n))
  (concat [[:indent n]] (serialize children) [[:outdent n]]))

;(defmethod serialize-node :align [[_ indent & children]]
;  (assert (integer? indent))
;  TODO align

;TODO normalize -- i think that this can be done on the serialize

(def empty-deque (double-list))



(comment

  (defn dbg [x]
    (pprint x)
    x)

  (pretty [:text "word"])

  (pretty [:span "cool" :line "dog"])

  (pretty [:group "omg" :line [:text "wtf" "is" "up?"] [:nest 2 "dog?"]])

)
