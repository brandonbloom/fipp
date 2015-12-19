(ns fipp.benchmark
  (:require [fipp.clojure]
            [fipp.edn]
            [clojure.java.io :as io]
            [clojure.pprint]
            [clojure.test.check.generators :as gen]
            [clojure.tools.reader.edn :as edn]
            [criterium.core :refer [bench]])
  (:import [java.io File Writer]))

(set! *warn-on-reflection* true)

(def benched-fns [prn
                  fipp.edn/pprint
                  fipp.clojure/pprint
                  clojure.pprint/pprint])

(def writer
  (proxy [Writer] []
    (write [_])
    (flush [])
    (close [])))

(defn samples []
  (let [^File file (io/file ".benchmark-samples")]
    (when-not (.exists file)
      (spit file (with-out-str
                   (fipp.edn/pprint (gen/sample gen/any-printable 1000)))))

    (edn/read-string (slurp file))))

;; lein run -m fipp.benchmark
(defn -main []
  (let [samples (samples)]
    (doseq [f benched-fns]
      (println "Benchmarking " f)
      (time
       (bench
        (binding [*out* writer]
          (doseq [sample samples]
            (f sample)))))
      (println))))
