(ns fipp.ednize.instant
  "Provides features that may not be available under every Clojure / JVM combination."
  (:require
   [clojure.instant]
   [fipp.ednize :refer [IEdn format-hack]])
  (:import
   (java.sql Timestamp)
   (java.util Date)))

(extend-protocol IEdn
  Timestamp
  (-edn [x]
    (let [s (format-hack #'clojure.instant/thread-local-utc-timestamp-format x)]
      (tagged-literal 'inst s)))

  Date
  (-edn [x]
    (let [s (format-hack #'clojure.instant/thread-local-utc-date-format x)]
      (tagged-literal 'inst s))))
