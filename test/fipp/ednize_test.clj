(ns fipp.ednize-test
  (:use [clojure.test])
  (:require [fipp.ednize :refer [-edn]]))

(deftest to-edn-test
  (testing "Conversion to tagged literals"
    (are [obj tag rep] (= (-edn obj) (tagged-literal tag rep))

      (java.util.UUID. 0 0)
      'uuid "00000000-0000-0000-0000-000000000000"

      (java.util.Date. 0)
      'inst "1970-01-01T00:00:00.000-00:00"

      (java.sql.Timestamp. 0)
      'inst "1970-01-01T00:00:00"

      clojure.lang.PersistentQueue/EMPTY
      'clojure.lang.PersistentQueue []

    )))
