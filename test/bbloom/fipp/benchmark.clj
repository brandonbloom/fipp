(ns bbloom.fipp.benchmark
  (:require [clojure.pprint]
            [bbloom.fipp.edn]))

(defn bench [f n x]
  (time
    (dotimes [i n]
      (with-out-str
        (f x)))))

(defn compare [n x]
  (bench clojure.pprint/pprint n x)
  (bench bbloom.fipp.edn/pprint n x))

(defn random-value []
  (let [f (rand-nth [identity keyword symbol])
        x (rand-nth ["foo" "bar" "baz" "abc123" "qwertyuiop"])]
    (f x)))

(defn random-seq []
  (let [n (+ (rand-int 7) 2)]
    (repeatedly n random-value)))

(defn random-map []
  (reduce (fn [m [v & path]]
            (assoc-in m path v)) ; this sometimes fails, come up with some better random-map
          {}
          (repeatedly 5 random-seq)))

(comment

  (bbloom.fipp.edn/pprint (random-map))

  (compare 100 (vec (range 1000)))
  "Elapsed time: 12827.101 msecs"
  "Elapsed time:  4505.121 msecs"

  (compare 1000 (random-map))
  "Elapsed time: 4412.506 msecs"
  "Elapsed time: 2732.823 msecs"

)
