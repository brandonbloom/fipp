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
  (bench fipp.edn/pprint n x)
  (bench clojure.pprint/pprint n x))

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


  (bench-all 300 (vec (range 1000)))

;  #object[clojure.core$prn 0x7ca1273f "clojure.core$prn@7ca1273f"]
;  "Elapsed time: 124.948 msecs"
;
;  #object[fipp.edn$pprint 0x25a8f408 "fipp.edn$pprint@25a8f408"]
;  "Elapsed time: 2819.549 msecs"
;
;  #object[clojure.pprint$pprint 0x756f13ae "clojure.pprint$pprint@756f13ae"]
;  "Elapsed time: 26679.661 msecs"


  (bench-all 2000 (random-map))

;  #object[clojure.core$prn 0x7ca1273f "clojure.core$prn@7ca1273f"]
;  "Elapsed time: 49.925 msecs"
;
;  #object[fipp.edn$pprint 0x25a8f408 "fipp.edn$pprint@25a8f408"]
;  "Elapsed time: 1730.908 msecs"
;
;  #object[clojure.pprint$pprint 0x756f13ae "clojure.pprint$pprint@756f13ae"]
;  "Elapsed time: 4655.917 msecs"

)
