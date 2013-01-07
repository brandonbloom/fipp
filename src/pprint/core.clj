(ns pprint.core
  (:require [clojure.string :as s]))


(defn flatten-seqs [x]
  (filter (complement seq?)
          (rest (tree-seq seq? identity x))))

(defmulti desugar-node first)

(defn desugar [doc]
  (cond
    (seq? doc) (->> doc (map desugar) flatten-seqs)
    (string? doc) doc
    (vector? doc) (desugar-node doc)
    :else (throw (Exception. "Unexpected doc node"))))

(defmethod desugar-node :text [[_ & strings]]
  (->> strings
       flatten-seqs
       (map #(interpose :newline (s/split % #"\n" -1)))
       (apply concat)))

(defmethod desugar-node :span [[_ & children]]
  (desugar children))

(defmethod desugar-node :group [[_ & children]]
  [:group (desugar children)])

(defmethod desugar-node :nest [[_ indent & children]]
  [:nest indent (desugar children)])


(comment

  (flatten-seqs (repeat 3 (range 5)))

  (desugar [:text "ab"])
  (desugar [:text "a\nb"])
  (desugar [:text "a" "x\ny" "b"])

  (desugar [:span [:span "a" "b"] "c" [:span "xyz" [:span "123"]]])
  (desugar [:span [:text "a" "b"]])

  (desugar [:group "a" [:span "b" "c"] "d"])

  (desugar [:group "a" [:nest 2 "b" "c"] "d"])

)
