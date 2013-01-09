(ns pprint.core
  "See: Oleg Kiselyov, Simon Peyton-Jones, and Amr Sabry
  Lazy v. Yield: Incremental, Linear Pretty-printing"
  (:require [clojure.string :as s]
            [clojure.core.reducers :as r]
            [clojure.data.finger-tree :refer (double-list consl ft-concat)]
            [pprint.transduce :as t]))

;;; Serialize document into a stream

(defmulti serialize-node first)

(defn serialize [doc]
  (cond
    (seq? doc) (mapcat serialize doc)
    (string? doc) [{:op :text, :text doc}]
    (keyword? doc) (serialize-node [doc])
    (vector? doc) (serialize-node doc)
    :else (throw (Exception. "Unexpected class for doc node"))))

(defmethod serialize-node :text [[_ & text]]
  [{:op :text, :text (apply str text)}])

(defmethod serialize-node :span [[_ & children]]
  (serialize children))

(defmethod serialize-node :line [[_ inline]]
  (let [inline (or inline " ")]
    (assert (string? inline))
    [{:op :line, :inline inline}]))

(defmethod serialize-node :group [[_ & children]]
  (concat [{:op :begin}] (serialize children) [{:op :end}]))

;TODO serialize nest & align nodes

(comment

  (serialize "apple")
  (serialize [:text "apple" "ball"])
  (serialize [:span "apple" [:group "ball" :line "cat"]])
  (serialize [:span "apple" [:line ","] "ball"])

  (def doc1 [:group "A" :line [:group "B" :line "C"]])
  (serialize doc1)

)

;;; Normalize document

;TODO normalization

;;; Annotate right-side of non-begin nodes assuming hypothetical zero-width
;;; empty groups along a single-line formatting of the document. These values
;;; are used by subsequent passes to produce the final layout.

(defn throw-op [node]
  (throw (Exception. (str "Unexpected op on node: " node))))

(def annotate-rights
  (t/map-state
    (fn [position node]
      (case (:op node)
        :text
          (let [position* (+ position (count (:text node)))]
            [position* (assoc node :right position*)])
        :line
          (let [position* (+ position (count (:inline node)))]
            [position* (assoc node :right position*)])
        :begin
          [position node]
        :end
          [position (assoc node :right position)]
        (throw-op node)))
    0))

(comment

  (->> doc1 serialize annotate-rights (into []) clojure.pprint/pprint)

)

;;; Annotate right-side of groups on their :begin nodes.
;;; NOTE: This is the non-pruning version, which is unbounded.

(def empty-deque (double-list))

(defn update-top [stack f & args]
  (conj (pop stack) (apply f (peek stack) args)))

(def annotate-begins
  (t/mapcat-state
    (fn [stack node]
      (cond
        (= (:op node) :begin)
          [(conj stack empty-deque) nil]
        (= (:op node) :end)
          (let [right (:right node)
                begin {:op :begin, :right right}
                [buffer & stack*] stack
                buffer* (conj (consl buffer begin) node)]
            (if (empty? stack*)
              [nil buffer*]
              [(update-top stack* ft-concat buffer*) nil]))
        (empty? stack)
          [nil [node]]
        :else
          (let [buffer (peek stack)
                stack* (pop stack)]
            [(conj stack* (conj buffer node)) nil])
        ))
    nil))


(comment

  (defn dbg [x]
    (pprint x)
    x)

  (->> doc1
       serialize
       annotate-rights
       annotate-begins
       (into [])
       clojure.pprint/pprint
       )

       ;(into [])
)
