(ns pprint.core
  "See: Oleg Kiselyov, Simon Peyton-Jones, and Amr Sabry
  Lazy v. Yield: Incremental, Linear Pretty-printing"
  (:require [clojure.string :as s]
            [clojure.core.reducers :as r]
            [clojure.data.finger-tree :refer (double-list consl ft-concat)]
            [pprint.transduce :as t]))

;;; Some double-list (deque / 2-3 finger-tree) utils

(def empty-deque (double-list))
(def conjl (fnil consl empty-deque))
(def conjr (fnil conj empty-deque))


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
          [position (assoc node :right position)] ; Temporarily assume 0-width
        :end
          [position (assoc node :right position)]
        (throw-op node)))
    0))

(comment

  (->> doc1 serialize annotate-rights (into []) clojure.pprint/pprint)

)

;;; Annotate right-side of groups on their :begin nodes.
;;; NOTE: This is the non-pruning version, which is unbounded.
;;; TODO: Implement the pruning version!!

(def ^:dynamic *width* 3)

(defn update-right [deque f & args]
  (conjr (pop deque) (apply f (peek deque) args)))

(def annotate-begins
  (t/mapcat-state
    (fn [{:keys [position buffers] :as state} {:keys [op right] :as node}]
      (if (empty? buffers)
        (if (= op :begin)
          ;; Buffer groups
          (let [position* (+ position *width*)
                buffer {:position position* :nodes empty-deque}
                state* {:position position* :buffers (double-list buffer)}]
            [state* nil])
          ;; Emit unbuffered
          [state [node]])
        (if (= op :end)
          ;; Pop buffer
          (let [buffer (peek buffers)
                buffers* (pop buffers)
                begin {:op :begin :right position}
                nodes (:nodes buffer)
                nodes* (conjr (conjl nodes begin) node)]
            (if (empty? buffers*)
              [{:position 0 :buffers empty-deque} nodes*]
              (let [buffers** (update-in buffers* [:nodes] update-right ft-concat nodes*)]
                [(assoc state :buffers buffers**) nil])))
          ;; Pruning lookahead
          (loop [position* position
                 buffers* (if (= op :begin)
                            (let [position** (+ position* *width*)
                                  buffer {:position position** :buffers empty-deque}]
                              (conjr buffers buffer))
                            (update-right buffers update-in [:nodes] conjr node))
                 emit nil]
            (if (and (<= right position*) (<= (count buffers*) *width*))
              ;; Not too far
              [{:position position* :buffers buffers*} emit]
              ;; Too far
              (let [buffer (peek buffers*)
                    buffers** (pop buffers*)
                    position** (:position buffer)
                    begin {:op :begin, :right :too-far}
                    emit* (concat [begin] (:nodes buffer) emit)]
                (if (empty? buffers**)
                  ;; Root buffered group
                  [{:position 0 :buffers empty-deque} emit*]
                  ;; Interior group
                  (recur position** buffers** emit*)))))
          )))
    {:position 0 :buffers empty-deque}))

;;; Format the annotated document stream.




(comment

  (defn dbg [x]
    (println "DBG:")
    (clojure.pprint/pprint x)
    (println "----")
    x)

  (defn map-dbg [prefix coll]
    (r/map (fn [x]
             (print prefix)
             (prn x)
             x)
           coll))

  (do
    (->> doc1
         serialize
         annotate-rights
         (map-dbg "read: ")
         annotate-begins
         (map-dbg "generated: ")
         (into [])
         ;clojure.pprint/pprint
         )
    nil)

)
