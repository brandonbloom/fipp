(ns fipp.ednize-test
  (:require [clojure.test :refer [deftest is are testing]]
            [fipp.ednize :refer #?(:clj [edn java-sql-timestamp? java-sql-timestamp]
                                   :cljs [edn])]))

(defrecord R [])

(def unique
  #?(:clj (Object.)
     :cljs (js-obj)))

(def sample-sql-timestamp
  (when java-sql-timestamp?
    (-> java-sql-timestamp
        (.getConstructor (into-array Class [Long/TYPE]))
        (.newInstance (into-array Object [0])))))

(deftest ednize-test
  (testing "Conversion to tagged literals"
    (are [obj tag rep] (= (edn obj) (tagged-literal tag rep))

      #?@(:clj [
        (java.util.UUID. 0 0)
        'uuid "00000000-0000-0000-0000-000000000000"

        clojure.lang.PersistentQueue/EMPTY
        'clojure.lang.PersistentQueue []

      ])
    ))

  (when #?(:clj  java-sql-timestamp?
           :cljs true)
    (testing "Conversion to tagged literals"
      (are [obj tag rep] (= (edn obj) (tagged-literal tag rep))
        #?(:clj  (java.util.Date. 0)
           :cljs (js/Date. 0))
        'inst "1970-01-01T00:00:00.000-00:00"
        
        #?@(:clj [sample-sql-timestamp
                  'inst "1970-01-01T00:00:00"]))))

  (testing "No-op for stuff that is already shallow edn."
    (are [obj] (= (edn obj) obj)
      nil true "x" 'x \x 1 () [] (R.) #{}))

  (testing "Shallow"
    (are [obj] (= (edn obj) obj)
      [unique]
      {unique unique}
      #{unique}
      (list unique)
      ))

  )
