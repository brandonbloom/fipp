(ns fipp.edn
  "Provides a pretty document serializer and pprint fn for Clojure/EDN forms.
  See fipp.clojure for pretty printing Clojure code."
  (:require [fipp.printer :as printer :refer (pprint-document)]))

(defprotocol IEdn
  "Perform a shallow conversion to an Edn data structure."
  (-edn [x]))

(defn tagged-object [o rep]
  (let [c (class o)
        cls (if (.isArray c) (.getName c) (symbol (.getName c)))
        id (format "0x%x" (System/identityHashCode o))]
    (tagged-literal 'object [cls id rep])))

(extend-protocol IEdn

  java.lang.Object
  (-edn [x]
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

  ;TODO clojure.lang.PersistentQueue, lots more stuff too

  )

(defn boolean? [x]
  (instance? Boolean x))

(defprotocol IVisitor
  (visit-meta [this meta x]) ; not strictly edn, but oh well.
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
  (visit-unknown [this x]))

(defn record->tagged [x]
  (tagged-literal (-> x class .getName symbol) (into {} x)))

(defn visit [visitor x]
  (if-let [m (meta x)]
    (visit-meta visitor m (with-meta x nil))
    (cond
      (nil? x) (visit-nil visitor)
      (boolean? x) (visit-boolean visitor x)
      (string? x) (visit-string visitor x)
      (char? x) (visit-character visitor x)
      (symbol? x) (visit-symbol visitor x)
      (keyword? x) (visit-keyword visitor x)
      (number? x) (visit-number visitor x)
      (seq? x) (visit-seq visitor x)
      (vector? x) (visit-vector visitor x)
      (record? x) (visit-tagged visitor (record->tagged x))
      (map? x) (visit-map visitor x)
      (set? x) (visit-set visitor x)
      (tagged-literal? x) (visit-tagged visitor x)
      :else (visit-unknown visitor x))))

(defrecord EdnPrinter [print-meta]

  IVisitor

  (visit-meta [this m x]
    (if print-meta
      [:align [:span "^" (visit this m)] :line (visit this x)]
      (visit this x)))

  (visit-nil [this]
    [:text "nil"])

  (visit-boolean [this x]
    [:text (str x)])

  (visit-string [this x]
    [:text (pr-str x)])

  (visit-character [this x]
    [:text (pr-str x)])

  (visit-symbol [this x]
    [:text (pr-str x)])

  (visit-keyword [this x]
    [:text (pr-str x)])

  (visit-number [this x]
    [:text (pr-str x)])

  (visit-seq [this x]
    [:group "(" [:align (interpose :line (map #(visit this %) x))] ")"])

  (visit-vector [this x]
    [:group "[" [:align (interpose :line (map #(visit this %) x))] "]"])

  (visit-map [this x]
    (let [kvps (for [[k v] x]
                 [:span (visit this k) " " (visit this v)])]
      [:group "{" [:align (interpose [:span "," :line] kvps)]  "}"]))

  (visit-set [this x]
    [:group "#{" [:align (interpose :line (map #(visit this %) x)) ] "}"])

  (visit-tagged [this {:keys [tag form]}]
    [:group "#" (pr-str tag)
            (when-not (coll? form) " ")
            (visit this form)])

  (visit-unknown [this x]
    (visit this (-edn x)))

  )

(defn pprint
  ([x] (pprint x {}))
  ([x options]
   (let [printer (map->EdnPrinter (merge {:print-meta *print-meta*} options))]
     (binding [*print-meta* false]
       (pprint-document (visit printer x) options)))))
