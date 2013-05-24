(ns fipp.benchmark
  (:require [clojure.pprint]
            [fipp.edn]))

(defn bench [f n x]
  (prn)
  (prn f)
  (time
    (dotimes [i n]
      (with-out-str
        (f x)))))

(defn bench-all [n x]
  (bench prn n x)
  (bench clojure.pprint/pprint n x)
  (bench fipp.edn/pprint n x))

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

  (fipp.edn/pprint (random-map))

  (bench-all 100 (vec (range 1000)))
;
;  #<core$prn clojure.core$prn@52305318>
;  "Elapsed time: 119.552 msecs"
;
;  #<pprint$pprint clojure.pprint$pprint@5331429e>
;  "Elapsed time: 37272.828 msecs"
;
;  #<edn$pprint fipp.edn$pprint@2747ac17>
;  "Elapsed time: 4306.488 msecs"

  (bench-all 1000 (random-map))
;
;  #<core$prn clojure.core$prn@52305318>
;  "Elapsed time: 113.016 msecs"
;
;  #<pprint$pprint clojure.pprint$pprint@5331429e>
;  "Elapsed time: 14043.387 msecs"
;
;  #<edn$pprint fipp.edn$pprint@2747ac17>
;  "Elapsed time: 4788.604 msecs"

)
