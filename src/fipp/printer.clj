(ns fipp.printer
  "See: Oleg Kiselyov, Simon Peyton-Jones, and Amr Sabry
  Lazy v. Yield: Incremental, Linear Pretty-printing"
  (:require [clojure.string :as s]
            [clojure.data.finger-tree :refer (double-list consl ft-concat)]
            [clojure.core.async :refer (chan go <! >! <!! close!)]))


;;; Some double-list (deque / 2-3 finger-tree) utils

(def empty-deque (double-list))

(def conjl (fnil consl empty-deque))
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

(defmethod serialize-node :text [[_ & text]]
  [{:op :text, :text (apply str text)}])

(defmethod serialize-node :pass [[_ & text]]
  [{:op :pass, :text (apply str text)}])

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

(defn annotate-rights [in out]
  (go
    (loop [position 0]
      (when-let [node (<! in)]
        (condp = (:op node)
          :text
            (let [position* (+ position (count (:text node)))]
              (>! out (assoc node :right position*))
              (recur position*))
          :line
            (let [position* (+ position (count (:inline node)))]
              (>! out (assoc node :right position*))
              (recur position*))
          (do
            (>! out (assoc node :right position))
            (recur position)))))
    (close! out)))


;;; Annotate right-side of groups on their :begin nodes.  This includes the
;;; pruning algorithm which will annotate some :begin nodes as being :too-far
;;; to the right without calculating their exact sizes.

;;TODO get rid of this dynamic var
(def ^:dynamic *width* 70)

(defn update-right [deque f & args]
  (conjr (pop deque) (apply f (peek deque) args)))

(defn annotate-begins [in out]
  (go
    (loop [{:keys [position buffers] :as state}
           {:position 0 :buffers empty-deque}]
      (when-let [{:keys [op right] :as node} (<! in)]
        (if (empty? buffers)
          (if (= op :begin)
            ;; Buffer groups
            (let [position* (+ right *width*)
                  buffer {:position position* :nodes empty-deque}]
              (recur {:position position* :buffers (double-list buffer)}))
            ;; Emit unbuffered
            (do
              (>! out node)
              (recur state)))
          (if (= op :end)
            ;; Pop buffer
            (let [buffer (peek buffers)
                  buffers* (pop buffers)
                  begin {:op :begin :right right}
                  nodes (conjlr begin (:nodes buffer) node)]
              (if (empty? buffers*)
                (do
                  (doseq [node nodes]
                    (>! out node))
                  (recur {:position 0 :buffers empty-deque}))
                (let [buffers** (update-right buffers* update-in [:nodes]
                                              ft-concat nodes)]
                  (recur (assoc state :buffers buffers**)))))
            ;; Pruning lookahead
            (recur
              (loop [position* position
                     buffers* (if (= op :begin)
                                (conjr buffers {:position (+ right *width*)
                                                :nodes empty-deque})
                                (update-right buffers update-in [:nodes]
                                              conjr node))]
                (if (and (<= right position*) (<= (count buffers*) *width*))
                  ;; Not too far
                  {:position position* :buffers buffers*}
                  ;; Too far
                  (let [buffer (first buffers*)
                        buffers** (next buffers*)]
                    ;; Emit buffered
                    (>! out {:op :begin, :right :too-far})
                    (doseq [node (:nodes buffer)]
                      (>! out node))
                    (if (empty? buffers**)
                      ;; Root buffered group
                      {:position 0 :buffers empty-deque}
                      ;; Interior group
                      (let [position** (:position (first buffers**))]
                        (recur position** buffers**)))))))))))
    (close! out)))


;;; Format the annotated document stream.

(defn format-nodes [in out]
  (go
    (loop [{:keys [fits length tab-stops column] :as state}
           {:fits 0
            :length *width*
            :tab-stops (list 0) ; Technically, this stack uses unbounded space...
            :column 0}]
      (when-let [{:keys [op right] :as node} (<! in)]
        (let [indent (peek tab-stops)]
          (condp = op
            :text
              (let [text (:text node)]
                (if (zero? column)
                  (do
                    (>! out (apply str (repeat indent \space)))
                    (>! out text)
                    (recur (assoc state :column (+ indent (count text)))))
                  (do
                    (>! out text)
                    (recur (update-in state [:column] + (count text))))))
            :pass
              (do
                (>! out (:text node))
                (recur state))
            :line
              (if (zero? fits)
                (do
                  (>! out "\n")
                  (recur (assoc state :length (- (+ right *width*) indent)
                                      :column 0)))
                (let [inline (:inline node)]
                  (>! out inline)
                  (recur (update-in state [:column] + (count inline)))))
            :break
              (do
                (>! out "\n")
                (recur (assoc state :length (- (+ right *width*) indent)
                                    :column 0)))
            :nest
              (recur (update-in state [:tab-stops] conj (+ indent (:offset node))))
            :align
              (recur (update-in state [:tab-stops] conj (+ column (:offset node))))
            :outdent
              (recur (update-in state [:tab-stops] pop))
            :begin
              (let [fits* (cond
                            (pos? fits) (inc fits)
                            (= right :too-far) 0
                            (<= right length) 1
                            :else 0)]
                (recur (assoc state :fits fits*)))
            :end
              (let [fits* (max 0 (dec fits))]
                (recur (assoc state :fits fits*)))
            (throw-op node)))))
    (close! out)))


(defn pprint-document [document options]
  (binding [*width* (:width options)]
    (let [c1 (chan)
          c2 (chan)
          c3 (chan)
          c4 (chan)]
      (go
        (doseq [x (serialize document)]
          (>! c1 x))
        (close! c1))
      (annotate-rights c1 c2)
      (annotate-begins c2 c3)
      (format-nodes c3 c4)
      (loop []
        (when-let [x (<!! c4)]
          (print x)
          (recur)))
      (println))))

(defmacro defprinter [name document-fn defaults]
  `(defn ~name
     ([~'document] (~name ~'document ~defaults))
     ([~'document ~'options]
       (pprint-document (~document-fn ~'document) ~'options))))


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

  (binding [*width* 3]
    (->> doc3
         serialize
         ;(map-dbg "node: ")
         annotate-rights
         annotate-begins
         format-nodes
         ;clojure.pprint/pprint
         (t/each print)
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
