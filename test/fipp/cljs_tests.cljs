(ns fipp.cljs-tests
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [cljs.nodejs :as nodejs]
            [fipp.ednize-test]
            [fipp.engine-test]
            [fipp.clojure-test]))

(nodejs/enable-util-print!)

(defn -main [& args]
  (run-tests 'fipp.ednize-test
             'fipp.engine-test
             'fipp.clojure-test))

(set! *main-cli-fn* -main)
