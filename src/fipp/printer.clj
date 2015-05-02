(ns fipp.printer
  "See: Oleg Kiselyov, Simon Peyton-Jones, and Amr Sabry
  Lazy v. Yield: Incremental, Linear Pretty-printing"
  (:require [clojure.string :as s]
            [clojure.data.finger-tree :as ftree
             :refer (double-list ft-concat)]))


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

(defn annotate-rights [rf]
  (let [pos (volatile! 0)]
    (fn
      ([] (rf))
      ([res] (rf res))
      ([res node]
       (let [delta (case (:op node)
                     :text (count (:text node))
                     :line (count (:inline node))
                     :escaped 1
                     0)
             p (vswap! pos + delta)]
         (rf res (assoc node :right p)))))))


;;; Annotate right-side of groups on their :begin nodes.  This includes the
;;; pruning algorithm which will annotate some :begin nodes as being :too-far
;;; to the right without calculating their exact sizes.

(def ^:dynamic *options* {:width 70})

(defn update-right [deque f & args]
  (conjr (pop deque) (apply f (peek deque) args)))

(defn annotate-begins [rf]
  (let [pos (volatile! 0)
        bufs (volatile! empty-deque)]
    (fn
      ([] (rf))
      ([res] (rf res))
      ([res {:keys [op right] :as node}]
       (let [position @pos
             buffers @bufs]
         (if (empty? buffers)
           (if (= op :begin)
             ;; Buffer groups
             (let [position* (+ right (:width *options*))
                   buffer {:position position* :nodes empty-deque}]
               (vreset! pos position*)
               (vreset! bufs (double-list buffer))
               res)
             ;; Emit unbuffered
             (rf res node))
           (if (= op :end)
             ;; Pop buffer
             (let [buffer (peek buffers)
                   buffers* (pop buffers)
                   begin {:op :begin :right right}
                   nodes (conjlr begin (:nodes buffer) node)]
               (if (empty? buffers*)
                 (do
                   (vreset! pos 0)
                   (vreset! bufs empty-deque)
                   (reduce rf res nodes))
                 (do
                   (vreset! bufs (update-right buffers* update-in [:nodes]
                                               ft-concat nodes))
                   res)))
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
                   (do
                     (vreset! pos position*)
                     (vreset! bufs buffers*)
                     (reduce rf res emit))
                   ;; Too far
                   (let [buffer (first buffers*)
                         buffers** (next buffers*)
                         begin {:op :begin, :right :too-far}
                         emit* (concat emit [begin] (:nodes buffer))]
                     (if (empty? buffers**)
                       ;; Root buffered group
                       (do
                         (vreset! pos 0)
                         (vreset! bufs empty-deque)
                         (reduce rf res emit*))
                       ;; Interior group
                       (let [position** (:position (first buffers**))]
                         (recur position** buffers** emit*)))))))
          )))))))


;;; Format the annotated document stream.

(defn format-nodes [rf]
  (let [fits (volatile! 0)
        length (volatile! (:width *options*))
        tab-stops (volatile! '(0)) ; Technically, this is an unbounded stack...
        column (volatile! 0)]
    (fn
      ([] (rf))
      ([res] (rf res))
      ([res {:keys [op right] :as node}]
       (let [indent (peek @tab-stops)
             width (:width *options*)]
         (case op
           :text
             (let [text (:text node)
                   res* (if (zero? @column)
                          (do (vswap! column + indent)
                              (rf res (apply str (repeat indent \space))))
                          res)]
               (vswap! column + (count text))
               (rf res* text))
           :escaped
             (let [text (:text node)
                   res* (if (zero? @column)
                          (do (vswap! column + indent)
                              (rf res (apply str (repeat indent \space))))
                          res)]
               (vswap! column inc)
               (rf res* text))
           :pass
             (rf res (:text node))
           :line
             (if (zero? @fits)
               (do
                 (vreset! length (- (+ right width) indent))
                 (vreset! column 0)
                 (rf res "\n"))
               (let [inline (:inline node)]
                 (vswap! column + (count inline))
                 (rf res inline)))
           :break
             (do
               (vreset! length (- (+ right width) indent))
               (vreset! column 0)
               (rf res "\n"))
           :nest
             (do (vswap! tab-stops conj (+ indent (:offset node)))
                 res)
           :align
             (do (vswap! tab-stops conj (+ @column (:offset node)))
                 res)
           :outdent
             (do (vswap! tab-stops pop)
                 res)
           :begin
             (do (vreset! fits (cond
                                 (pos? @fits) (inc @fits)
                                 (= right :too-far) 0
                                 (<= right @length) 1
                                 :else 0))
                 res)
           :end
             (do (vreset! fits (max 0 (dec @fits)))
                 res)
           (throw-op node)))
       ))))



(defn pprint-document [document options]
  (binding [*options* (merge *options* options)]
    (->> (serialize document)
         (eduction annotate-rights annotate-begins format-nodes)
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
  (def doc2 [:group "A" :line [:nest 2 "B" :line "C"] :line "D"])
  (def doc3 [:group "A" :line
             [:nest 2 "B-XYZ" [:align -3 :line "C"]] :line "D"])

  (serialize doc1)

  (binding [*options* {:width 3}]
    (->> doc3
         serialize
         (into [] (comp
                    annotate-rights
                    annotate-begins
                    format-nodes
                    ))
         ;(run! print)
         clojure.pprint/pprint
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
        "]"])

  (pprint-document ex1 {:width 20})
  (pprint-document ex1 {:width 6})

  (def ex2
    [:span "["
        [:align
            [:group [:line ""]] "0,"
            [:group :line] "1,"
            [:group :line] "2,"
            [:group :line] "3"]
        "]"])

  (pprint-document ex2 {:width 20})
  (pprint-document ex2 {:width 6})

)
