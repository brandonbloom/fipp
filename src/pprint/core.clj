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

(defn- command-op [command]
  (cond
    (string? command) :text
    (keyword? command) command
    (vector? command) (first command)
    :else (throw (Exception. "Unexpected command class"))))

(defn- prune [position deque]
  (if (empty? deque)
    [empty-deque nil]
    (let [x (first deque)]
      (if (<= position (:position x))
        [deque nil]
        (let [[deque* horizontals*] (prune position (next deque))]
          [deque* (concat [false] (:horizontals x) horizontals*)])))))

(defn- prune-state [state increment]
  (let [position (+ (:position state) increment)
        [deque horizontals] (prune position (:deque state))]
    (assoc state :deque deque :horizontals horizontals)))

(defn- layout-text [state text]
  (let [length (count text)]
    (-> state
      (prune-state length)
      (update-in [:remaining] - length)
      (update-in [:output] concat [text]))))

(defn- begin-group [{:keys [position remaining] :as state}]
  (update-in state [:deque] conj {:position (+ position remaining)
                                  :horizontals nil}))

(defn- end-group [{:keys [position deque] :as state}]
  (case (count deque)
    0 (assoc state :deque empty-deque)
    1 (let [{:keys [horizontals]} (peek deque)]
        (-> state
          (assoc :deque empty-deque)
          (update-in [:horizontals] concat (cons true horizontals))))
    (let [[x1 x2] (reverse deque)]
      (-> state
        (assoc :deque (pop (pop deque)))
        (update-in [:deque]
                   conj
                   {:position (:position x2)
                    :horizontals (concat (:horizontals x2)
                                         [(<= position (:position x1))]
                                         (:horizontals x1))})))))

(defn- layout-break [{:keys [indent max-width] :as state} inline]
  (let [state* (prune-state state 1)]
    (if (first (:horizontals state*))
      (-> state*
        (update-in [:remaining] dec)
        (update-in [:output] concat [inline]))
      (-> state*
        (assoc :remaining (- max-width indent))
        (update-in [:output] concat [\newline] (repeat indent " "))))))

(defn- dent [state n]
  (update-in state [:indent] + n))

(defn- advance [state command]
  (let [state (assoc state :command command)]
    (case (command-op command)
      :text (layout-text state command)
      :begin (begin-group state)
      :end (end-group state)
      :line (layout-break state (second command))
      :indent (dent state (second command))
      :outdent (dent state (- (second command)))
      (throw (Exception. (str "Unknown command " (command-op command)))))))

(defn- initial-state [width]
  {:indent 0
   :position 0
   :deque empty-deque
   :remaining width
   :max-width width
   :horizontals '(false)
   :output nil})

(defn- each [f coll]
  (doseq [x coll]
    (f x)))

(defn pretty
  ([doc] (pretty doc 70))
  ([doc width]
   (->> (serialize [:group doc])
        (reductions advance (initial-state width))
        dbg
        :output
        (each print))))


(comment

  (defn dbg [x]
    (pprint x)
    x)

  (pretty [:text "word"])

  (pretty [:span "cool" :line "dog"])

  (pretty [:group "omg" :line [:text "wtf" "is" "up?"] [:nest 2 "dog?"]])

)
