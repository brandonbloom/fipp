(ns fipp.ednize
  (:require [clojure.instant]
            [fipp.util :refer [edn?]]))

(defprotocol IEdn
  "Perform a shallow conversion to an Edn data structure."
  (-edn [x]))

(defprotocol IOverride
  "Mark object as preferring its custom IEdn behavior.")

(defn override? [x]
  (satisfies? IOverride x))

(defn edn [x]
  (-edn x))

(defn class->edn [^Class c]
  (if (.isArray c)
    (.getName c)
    (symbol (.getName c))))

(defn tagged-object [o rep]
  (let [cls (class->edn (class o))
        id (format "0x%x" (System/identityHashCode o))]
    (tagged-literal 'object [cls id rep])))

(defn format-hack [v x]
  (let [local ^java.lang.ThreadLocal @v
        fmt ^java.text.SimpleDateFormat (.get local)]
    (.format fmt x)))

(extend-protocol IEdn

  nil
  (-edn [x]
    nil)

  java.lang.Object
  (-edn [x]
    (if (edn? x)
      x
      (tagged-object x (str x))))

  clojure.lang.IDeref
  (-edn [x]
    (let [pending? (and (instance? clojure.lang.IPending x)
                        (not (.isRealized ^clojure.lang.IPending x)))
          [ex val] (when-not pending?
                     (try [false @x]
                          (catch Throwable e
                            [true e])))
          failed? (or ex (and (instance? clojure.lang.Agent x)
                              (agent-error x)))
          status (cond
                   failed? :failed
                   pending? :pending
                   :else :ready)]
      (tagged-object x {:status status :val val})))

  java.lang.Class
  (-edn [x]
    (class->edn x))

  ;TODO (defmethod print-method StackTraceElement
  ;TODO print-throwable
  ;TODO reader-conditional
  ;TODO Eduction ??

  java.util.Date
  (-edn [x]
    (let [s (format-hack #'clojure.instant/thread-local-utc-date-format x)]
      (tagged-literal 'inst s)))

  ;TODO (defmethod print-method java.util.Calendar

  java.sql.Timestamp
  (-edn [x]
    (let [s (format-hack #'clojure.instant/thread-local-utc-timestamp-format x)]
      (tagged-literal 'inst s)))

  java.util.UUID
  (-edn [x]
    (tagged-literal 'uuid (str x)))

  clojure.lang.PersistentQueue
  (-edn [x]
    (tagged-literal 'clojure.lang.PersistentQueue (vec x)))

  )

(defn record->tagged [x]
  (tagged-literal (-> x class .getName symbol) (into {} x)))
