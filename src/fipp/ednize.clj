(ns fipp.ednize)

(defprotocol IEdn
  "Perform a shallow conversion to an Edn data structure."
  (-edn [x]))

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

  java.lang.Object
  (-edn [x]
    (throw (Exception. "??"))
    (tagged-object x (str x)))

  clojure.lang.IDeref
  (-edn [x]
    (let [pending? (and (instance? clojure.lang.IPending x)
                        (not (.isRealized ^clojure.lang.IPending x)))
          [ex val] (when-not pending?
                     (try [false (deref x)]
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

(defn boolean? [x]
  (instance? Boolean x))

(defn pattern? [x]
  (instance? java.util.regex.Pattern x))

(defn record->tagged [x]
  (tagged-literal (-> x class .getName symbol) (into {} x)))
