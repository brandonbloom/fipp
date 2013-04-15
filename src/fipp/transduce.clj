(ns fipp.transduce
  "Provides reducers support for State Transducers"
  (:require [clojure.core.reducers :as r]))

(defmacro defcurried
  "Builds another arity of the fn that returns a fn awaiting the last param"
  [name doc args & body]
  (let [cargs (vec (butlast args))]
    `(defn ~name ~doc
       (~cargs (fn [x#] (~name ~@cargs x#)))
       (~args ~@body))))

(defcurried map-state
  "Like map, but threads a state through the sequence of transformations.
  For each x in coll, f is applied to [state x] and should return [state' x'].
  The first invocation of f uses init as the state."
  [f init coll]
  (r/reducer coll
    (fn [f1]
      (let [state (atom init)]
        (fn [acc x]
          (let [[state* x*] (f @state x)]
            (reset! state state*)
            (f1 acc x*)))))))

(defcurried mapcat-state
  "Like mapcat, but threads a state through the sequence of transformations.
  For each x in coll, f is applied to [state x] and should return [state' xs].
  The result is the concatenation of each returned xs."
  [f init coll]
  (r/reducer coll
    (fn [f1]
      (let [state (atom init)]
        (fn [acc x]
          (let [[state* xs] (f @state x)]
            (reset! state state*)
            (if (seq xs)
              (reduce f1 acc xs)
              acc)))))))

(defcurried each
  "Applies f to each item in coll, returns nil"
  [f coll]
  (reduce (fn [_ x] (f x) nil) nil coll))

(comment

  (->> (range 5)
    (map-state (fn [index x]
                 [(inc index) (/ x index)])
               1)
    (into []))

  (->> (range 10)
    (mapcat-state (fn [buffer x]
                     (if (< (count buffer) 5)
                       [(conj buffer x) nil]
                       [[] buffer]))
                  [])
    (into []))

  (->> (range 5)
    (each #(prn (first (str %)))))

)
