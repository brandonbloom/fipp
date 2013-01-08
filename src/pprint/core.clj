(ns pprint.core
  "Linear, bounded, functional pretty-printing.
  See Doitse Swierstra and Olaf Chitil
  Journal of Functional Programming, 19(1):1-16, January 2009."
  (:require [clojure.string :as s]
            [clojure.zip :as zip]
            [clojure.data.finger-tree :refer (double-list)]))

;;TODO rename to FPretty to match the Haskell project and disambiguate core?

(defn flatten-seqs [x]
  (filter (complement seq?)
          (rest (tree-seq seq? identity x))))

(defmulti expand-node first)

;;TODO more parameterless shorthands?
(def keywords (atom {:line [:line " "]
                     :softline [:group '([:line " "])]}))

(defn expand
  "Recursively expands a pretty-print document into a tree of primitives."
  [doc]
  (cond
    (seq? doc) (let [x (->> doc (map expand) flatten-seqs)]
                 (if (next x) x (first x)))
    (string? doc) doc ;TODO something like this:   (interpose :newline (s/split doc #"\n" -1))
    (vector? doc) (expand-node doc)
    (keyword? doc) (or (@keywords doc)
                       (throw (Exception. "Unexpected doc keyword " doc)))
    :else (throw (Exception. "Unexpected type for doc node"))))

;;; Primitives

;;TODO: default that delegates to parameterless keywords?

(defmethod expand-node :text [[_ & text]]
  (apply str text))

(defmethod expand-node :span [[_ & children]]
  (expand children))

(defmethod expand-node :line [[_ inline :as node]]
  (assert (string? inline))
  (assert (= (count node) 2))
  node)

(defmethod expand-node :group [[_ & children]]
  [:group (expand children)])

(defmethod expand-node :nest [[_ indent & children]]
  (assert (integer? indent))
  [:nest indent (expand children)])

(defmethod expand-node :align [[_ indent & children]]
  (assert (integer? indent))
  [:align indent (expand children)])

;;; Combinators
;TODO


;;; Normalize

(declare normalize)

(defn- normalize-text [text tt]
  [(cons text tt) nil])

(defn- normalize-span [[left & right] tt]
  (let [[td-r sd-r] (if (next right)
                      (normalize-span right tt)
                      (normalize (first right) tt))
        [td-l sd-l] (normalize left td-r)]
    [td-l (cons sd-l sd-r)]))

(defn- normalize-line [line tt]
  [nil (cons line tt)])

(defn- normalize-group [[child] tt]
  [child [:group tt]])

(defn- normalize-nest [[indent child] tt]
  [child [:nest indent tt]])

(defn- normalize-align [& args]
  (assert false "TODO normalize-align"))

(defn- normalize [doc tt]
  (cond
    (string? doc) (normalize-text doc tt)
    (seq? doc) (normalize-span doc tt)
    :else (let [op (first doc)]
            (case op
              :line (normalize-line doc tt)
              :group (normalize-group doc tt)
              :nest (normalize-nest doc tt)
              :align (normalize-align doc tt)
              (throw (Exception. (str "Unknown pretty doc op " op)))))))

(defn- go-normalize [doc]
  (normalize doc nil))


(def empty-deque (double-list))

(defn- prune [left deque]
  (if (empty? deque)
    [empty-deque nil]
    (let [[right horizontals] (first deque)]
      (if (<= left right)
        [deque nil]
        (let [[deque* horizontals*] (prune left (next deque))]
          (prn "<<<<<<<<<" left right)
          [deque* (concat (cons false horizontals) horizontals*)])))))

(defn- enter [right deque]
  (conj deque [right nil]))

(defn- leave [left deque]
  (case (count deque)
    0 [empty-deque nil]
    1 (let [[right horizontals] (peek deque)]
        [empty-deque (cons true horizontals)])
    (let [[right1 horizontals1] (peek deque)
          [right2 horizontals2] (peek (pop deque))
          deque* (pop (pop deque))
          _ (prn "!@!!!!" left right1)
          horizontals* (concat horizontals2 [(<= left right1)] horizontals1)]
      [(conj deque* [right2 horizontals*]) nil])))

(def ^:dynamic *width* 5) ;70)

(declare layout)

(defn- layout-text [text [i p dq hs r]]
  (prn "layout-text" p text (count text) r dq)
  (let [l (count text)
        [dq' as] (prune (+ p l) dq)]
    [[(+ p l) dq' hs (- r l)] [text] as]))

(defn- new-line [indent horizontal remaining inline]
  (prn "new-line" *width* horizontal remaining indent)
  (prn "WTF?")
  (if horizontal
    [(dec remaining) inline]
    [(- *width* indent) (apply str \newline (repeat indent " "))]))

(defn- layout-line [[inline] [i p dq hs r]]
  (prn "layout-line" inline i p dq hs r)
  (let [[dq' as] (prune (inc p) dq)
        [r' l'] (new-line i (first hs) r inline)]
    [[(inc p) dq' hs r'] l' as]))

(defn- layout-span [[left & right] state]
  (let [[state-l l-l as-l] (layout left state)
        state-l (cons (first state) state-l)             ; hacky cons because i is not considered part of state
        [state-r l-r as-r] (if (next right)
                             (layout-span right state-l)
                             (layout (first right) state-l))]
    [state-r (concat l-l l-r) (concat as-l as-r)]))

(defn- layout-group [[child] [i p dq hs r]]
  (prn "layout-group" p r)
  (let [[[pe dq-d hsd r-d] l-d as-d] (layout child [i p (enter (+ p r) dq) hs r])
        [dq' as'] (leave pe dq-d)]
    [[pe dq' (lazy-seq (cons (first hs) (next hsd))) r-d] l-d (concat as-d as')]))

(defn- layout-nest [[indent child] [i p dq hs r]]
  (prn "layout-nest" i indent)
  (layout child [(+ i indent) p dq hs r]))

(defn- layout-align [& args]
  (assert false "TODO layout-align"))

(defn layout [doc state]
  (prn "layout" doc)
  (cond
    (string? doc) (layout-text doc state)
    (seq? doc) (layout-span doc state)
    :else (let [[op & args] doc]
            (case op
              :line (layout-line args state)
              :group (layout-group args state)
              :nest (layout-nest args state)
              :align (layout-align args state)
              (throw (Exception. (str "Unknown pretty doc op " op)))))))

(defn pretty [doc]
  (let [state [0 0 empty-deque '(false) *width*]
        [_ l as] (layout [:group doc] state)]
    l))

(defn go [doc]
  (doseq [s (-> doc
                expand
                go-normalize
                pretty
                )]
    (print s)))

;(pretty

(comment

  (flatten-seqs (repeat 3 (range 5)))

  (expand "ab")
  (expand "a\nb")
  (expand [:text "a" "b"])

  (expand [:span [:span "a" "b"] "c" [:span "xyz" [:span "123"]]])
  (expand [:span [:text "a" "b"]])
  (expand [:span :newline [:text "a" "b"]])

  (expand [:group "a" [:span "b" "c"] "d"])

  (expand [:group "a" [:nest 2 "b" "c"] "d"])

  (expand [:span "a" "b"])

  (expand [:span [:text "foo" "bar"] [:text "baz"]])

  ;; need to have a top level group!!

  *width*

  (expand [:span [:text "foo" "bar"] [:span [:line ","] [:text "baz"]]])

  (go-normalize (expand [:group "a" [:line ","] "b"]))

  (go [:nest 4 "wtf" "this" "is" "some" "long" [:line ","] "string"])
  (go [:span [:text "foo" "bar"] [:nest 4 "wtf"] [:text "omgg"] [:line ","] [:text "baz"]])

)
