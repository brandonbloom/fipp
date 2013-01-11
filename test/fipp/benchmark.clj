(ns bbloom.fipp.benchmark
  (:require [clojure.pprint]
            [bbloom.fipp.edn]))

(defn bench [f x]
  (time
    (dotimes [i 100]
      (with-out-str
        (f x)))))

(def x (vec (range 1000)))
(bench clojure.pprint/pprint x)  ; 13389.499 msecs
(bench bbloom.fipp.edn/pprint x) ;  5111.764 msecs
