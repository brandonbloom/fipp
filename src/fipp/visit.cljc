(ns fipp.visit
  "Convert to and visit edn structures."
  (:require [fipp.util :as util]
            [fipp.ednize :refer [override?]]))

;;;TODO Stablize public interface

(defprotocol IVisitor

  (visit-unknown [this x])

  (visit-nil [this])
  (visit-boolean [this x])
  (visit-string [this x])
  (visit-character [this x])
  (visit-symbol [this x])
  (visit-keyword [this x])
  (visit-number [this x])
  (visit-seq [this x])
  (visit-vector [this x])
  (visit-map [this x])
  (visit-set [this x])
  (visit-tagged [this x])

  ;; Not strictly Edn...
  (visit-meta [this meta x])
  (visit-var [this x])
  (visit-pattern [this x])
  (visit-record [this x])
  )

(defn visit*
  "Visits objects, ignoring metadata."
  [visitor x]
  (cond
    (nil? x) (visit-nil visitor)
    (override? x) (visit-unknown visitor x)
    (util/boolean? x) (visit-boolean visitor x)
    (string? x) (visit-string visitor x)
    (util/char? x) (visit-character visitor x)
    (symbol? x) (visit-symbol visitor x)
    (keyword? x) (visit-keyword visitor x)
    (number? x) (visit-number visitor x)
    (seq? x) (visit-seq visitor x)
    (vector? x) (visit-vector visitor x)
    (record? x) (visit-record visitor x)
    (map? x) (visit-map visitor x)
    (set? x) (visit-set visitor x)
    (tagged-literal? x) (visit-tagged visitor x)
    (var? x) (visit-var visitor x)
    (util/regexp? x) (visit-pattern visitor x)
    :else (visit-unknown visitor x)))

(defn value-meta [x]
  (when (util/value-obj? x)
    (meta x)))

(defn visit [visitor x]
  (if-let [m (value-meta x)]
    (visit-meta visitor m x)
    (visit* visitor x)))
