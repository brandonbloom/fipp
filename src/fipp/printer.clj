(ns fipp.printer
  "See: Oleg Kiselyov, Simon Peyton-Jones, and Amr Sabry
  Lazy v. Yield: Incremental, Linear Pretty-printing"
  (:require [clojure.string :as s]
            [clojure.core.reducers :as r]
            [clojure.data.finger-tree :as ftree :refer (double-list ft-concat)]
            [transduce.reducers :as t]))


;;; Some double-list (deque / 2-3 finger-tree) utils

(def empty-deque (double-list))

(def conjl (fnil ftree/conjl empty-deque))
(def conjr (fnil conj empty-deque))

(defn conjlr [l deque r]
  (conj (conjl deque l) r))


;;; Serialize document into a stream

(defmulti serialize-node first)

(defn serialize [doc]
  (cond
    (nil? doc) nil
    (seq? doc) (mapcat serialize doc)
    (string? doc) [{:op :text, :text doc}]
    (keyword? doc) (serialize-node [doc])
    (vector? doc) (serialize-node doc)
    :else (throw (Exception.
                   (str "Unexpected class for doc node: " (class doc))))))

;; Primitives
;; See doc/primitives.md for details.

(defmethod serialize-node :text [[_ & text]]
  [{:op :text, :text (apply str text)}])

(defmethod serialize-node :pass [[_ & text]]
  [{:op :pass, :text (apply str text)}])

(defmethod serialize-node :escaped [[_ text]]
  (assert (string? text))
  [{:op :escaped, :text text}])

(defmethod serialize-node :span [[_ & children]]
  (serialize children))

(defmethod serialize-node :line [[_ inline]]
  (let [inline (or inline " ")]
    (assert (string? inline))
    [{:op :line, :inline inline}]))

(defmethod serialize-node :break [& _]
  [{:op :break}])

(defmethod serialize-node :group [[_ & children]]
  (concat [{:op :begin}] (serialize children) [{:op :end}]))

(defmethod serialize-node :nest [[_ offset & children]]
  (concat [{:op :nest, :offset offset}]
          (serialize children)
          [{:op :outdent}]))

(defmethod serialize-node :align [[_ & args]]
  (let [[offset & children] (if (number? (first args))
                             args
                             (cons 0 args))]
    (concat [{:op :align, :offset offset}]
            (serialize children)
            [{:op :outdent}])))


;;; Annotate right-side of nodes assuming hypothetical single-line
;;; formatting of the document. Groups and indentation directives
;;; are temporarily assumed to be zero-width. These values are
;;; used by subsequent passes to produce the final layout.

(defn throw-op [node]
  (throw (Exception. (str "Unexpected op on node: " node))))

(def annotate-rights
  (t/map-state
    (fn [position node]
      (case (:op node)
        :text
          (let [position* (+ position (count (:text node)))]
            [position* (assoc node :right position*)])
        :escaped
          (let [position* (inc position)]
            [position* (assoc node :right position*)])
        :line
          (let [position* (+ position (count (:inline node)))]
            [position* (assoc node :right position*)])
        [position (assoc node :right position)]))
    0))


;;; Annotate right-side of groups on their :begin nodes.  This includes the
;;; pruning algorithm which will annotate some :begin nodes as being :too-far
;;; to the right without calculating their exact sizes.

(def ^:dynamic *options* {:width 70})

(defn update-right [deque f & args]
  (conjr (pop deque) (apply f (peek deque) args)))

(def annotate-begins
  (t/mapcat-state
    (fn [{:keys [position buffers] :as state}
         {:keys [op right] :as node}]
      (if (empty? buffers)
        (if (= op :begin)
          ;; Buffer groups
          (let [position* (+ right (:width *options*))
                buffer {:position position* :nodes empty-deque}
                state* {:position position* :buffers (double-list buffer)}]
            [state* nil])
          ;; Emit unbuffered
          [state [node]])
        (if (= op :end)
          ;; Pop buffer
          (let [buffer (peek buffers)
                buffers* (pop buffers)
                begin {:op :begin :right right}
                nodes (conjlr begin (:nodes buffer) node)]
            (if (empty? buffers*)
              [{:position 0 :buffers empty-deque} nodes]
              (let [buffers** (update-right buffers* update-in [:nodes]
                                            ft-concat nodes)]
                [(assoc state :buffers buffers**) nil])))
          ;; Pruning lookahead
          (let [width (:width *options*)]
            (loop [position* position
                   buffers* (if (= op :begin)
                              (conjr buffers {:position (+ right width)
                                              :nodes empty-deque})
                              (update-right buffers update-in [:nodes]
                                            conjr node))
                   emit nil]
              (if (and (<= right position*) (<= (count buffers*) width))
                ;; Not too far
                [{:position position* :buffers buffers*} emit]
                ;; Too far
                (let [buffer (first buffers*)
                      buffers** (next buffers*)
                      begin {:op :begin, :right :too-far}
                      emit* (concat emit [begin] (:nodes buffer))]
                  (if (empty? buffers**)
                    ;; Root buffered group
                    [{:position 0 :buffers empty-deque} emit*]
                    ;; Interior group
                    (let [position** (:position (first buffers**))]
                      (recur position** buffers** emit*)))))))
          )))
    {:position 0 :buffers empty-deque}))


;;; Format the annotated document stream.

(defn format-nodes [coll]
  (t/mapcat-state
    (fn [{:keys [fits length tab-stops column] :as state}
         {:keys [op right] :as node}]
      (let [indent (peek tab-stops)
            width (:width *options*)]
        (case op
          :text
            (let [text (:text node)]
              (if (zero? column)
                (let [emit [(apply str (repeat indent \space)) text]
                      state* (assoc state :column (+ indent (count text)))]
                  [state* emit])
                (let [state* (update-in state [:column] + (count text))]
                  [state* [text]])))
          :escaped
            (let [text (:text node)]
              (if (zero? column)
                (let [emit [(apply str (repeat indent \space)) text]
                      state* (assoc state :column (inc indent))]
                  [state* emit])
                (let [state* (update-in state [:column] inc)]
                  [state* [text]])))
          :pass
            [state [(:text node)]]
          :line
            (if (zero? fits)
              (let [state* (assoc state :length (- (+ right width) indent)
                                        :column 0)]
                [state* ["\n"]])
              (let [inline (:inline node)
                    state* (update-in state [:column] + (count inline))]
                [state* [inline]]))
          :break
            (let [state* (assoc state :length (- (+ right width) indent)
                                      :column 0)]
              [state* ["\n"]])
          :nest
            [(update-in state [:tab-stops] conj (+ indent (:offset node))) nil]
          :align
            [(update-in state [:tab-stops] conj (+ column (:offset node))) nil]
          :outdent
            [(update-in state [:tab-stops] pop) nil]
          :begin
            (let [fits* (cond
                          (pos? fits) (inc fits)
                          (= right :too-far) 0
                          (<= right length) 1
                          :else 0)]
              [(assoc state :fits fits*) nil])
          :end
            (let [fits* (max 0 (dec fits))]
              [(assoc state :fits fits*) nil])
          (throw-op node))))
    {:fits 0
     :length (:width *options*)
     :tab-stops '(0) ; Technically, this stack uses unbounded space...
     :column 0}
  coll))


(defn pprint-document [document options]
  (binding [*options* (merge *options* options)]
    (->> document
         serialize
         annotate-rights
         annotate-begins
         format-nodes
         (run! print)))
  (println))


(comment

  (defn dbg [x]
    (println "DBG:")
    (clojure.pprint/pprint x)
    (println "----")
    x)

  (serialize "apple")
  (serialize [:text "apple" "ball"])
  (serialize [:span "apple" [:group "ball" :line "cat"]])
  (serialize [:span "apple" [:line ","] "ball"])

  (def doc1 [:group "A" :line [:group "B" :line "C"]])
  (serialize doc1)

  (defn map-dbg [prefix coll]
    (r/map (fn [x]
             (print prefix)
             (prn x)
             x)
           coll))

  (def doc2 [:group "A" :line [:nest 2 "B" :line "C"] :line "D"])
  (def doc3 [:group "A" :line [:nest 2 "B-XYZ" [:align -3 :line "C"]] :line "D"])

  (binding [*options* {:width 3}]
    (->> doc3
         serialize
         ;(map-dbg "node: ")
         annotate-rights
         annotate-begins
         format-nodes
         ;clojure.pprint/pprint
         (run! print)
         ;(into [])
         )
    ;nil
    )

  ;; test of :pass op
  (do
    (pprint-document
      [:group "AB" :line "B" :line "C"]
      {:width 6}) 
    (println "--")
    (pprint-document
      [:group "<AB>" :line "B" :line "C"]
      {:width 6}) 
    (println "--")
    (pprint-document
      [:group [:pass "<"] "AB" [:pass ">"] :line "B" :line "C"]
      {:width 6}))

  (def ex1

[:group "["
    [:nest 2
        [:line ""] "0,"
        :line "1,"
        :line "2,"
        :line "3"
        [:line ""]]
    "]"]

   )

  (pprint-document ex1 {:width 20})
  (pprint-document ex1 {:width 6})

  (def ex2

[:span "["
    [:align
        [:group [:line ""]] "0,"
        [:group :line] "1,"
        [:group :line] "2,"
        [:group :line] "3"]
    "]"]

   )

  (pprint-document ex2 {:width 20})
  (pprint-document ex2 {:width 6})

)
