(ns fipp.ednize-test
  #?(:clj (:use [clojure.test])
     :cljs (:require-macros [cljs.test :refer [deftest is are testing]]))
  (:require [fipp.ednize :refer [edn]]))

(deftest jvm-to-edn-test
  (testing "Conversion to tagged literals"
    (are [obj tag rep] (= (edn obj) (tagged-literal tag rep))

      #?(:clj (java.util.Date. 0)
         :cljs (js/Date. 0))
      'inst "1970-01-01T00:00:00.000-00:00"

      #?@(:clj [
        (java.util.UUID. 0 0)
        'uuid "00000000-0000-0000-0000-000000000000"

        (java.sql.Timestamp. 0)
        'inst "1970-01-01T00:00:00"

        clojure.lang.PersistentQueue/EMPTY
        'clojure.lang.PersistentQueue []
      ])

    )))
