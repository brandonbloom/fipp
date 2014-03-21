(ns fipp.edn
  "Provides a pretty document serializer and pprint fn for Clojure/EDN forms.
  See fipp.clojure for pretty printing Clojure code."
  (:require [fipp.printer :as printer :refer (pprint-document *options*)]))

;;TODO Figure out what belongs in clojure.clj instead of edn.clj

(defprotocol IPretty
  (-pretty [x]))

(defn pretty [x]
  (if-let [m (and (:print-meta *options*) (meta x))]
    [:align [:span "^" (-pretty m)] :line (-pretty x)]
    (-pretty x)))

(defn system-id [obj]
  (Integer/toHexString (System/identityHashCode obj)))

(defn pretty-map [m]
  (let [kvps (for [[k v] m]
               [:span (pretty k) " " (pretty v)])
        doc [:group "{" [:align (interpose [:span "," :line] kvps)]  "}"]]
    (if (instance? clojure.lang.IRecord m)
      [:span "#" (-> m class .getName) doc]
      doc)))

(extend-protocol IPretty

  nil
  (-pretty [x]
    [:text "nil"])

  java.lang.Object
  (-pretty [x]
    [:text (pr-str x)])

  clojure.lang.IPersistentVector
  (-pretty [v]
    [:group "[" [:align (interpose :line (map pretty v))] "]"])

  clojure.lang.ISeq
  (-pretty [s]
    [:group "(" [:align (interpose :line (map pretty s))] ")"])

  clojure.lang.IPersistentMap
  (-pretty [m]
    (pretty-map m))

  clojure.lang.IPersistentSet
  (-pretty [s]
    [:group "#{" [:align (interpose :line (map pretty s)) ] "}"])

  ;;TODO figure out how inheritence is resolved...
  clojure.lang.IRecord
  (-pretty [r]
    (pretty-map r))

  clojure.lang.Atom
  (-pretty [a]
    [:span "#<Atom@" (system-id a) " " (pretty @a) ">"])

  java.util.concurrent.Future
  (-pretty [f]
    (let [value (if (future-done? f)
                  (pretty @f)
                  ":pending")]
      [:span "#<Future@" (system-id f) " " value ">"]))

  ;TODO clojure.lang.PersistentQueue, lots more stuff too

  )

(defn pprint
  ([x] (pprint x {}))
  ([x options]
   (let [options* (merge {:width 70 :print-meta *print-meta*} options)]
     (binding [*print-meta* false]
       (pprint-document (pretty x) options*)))))

(comment

  (defrecord Person [first-name last-name])

  (def fut (future 1))

  (binding [*print-meta* true]
  (->
    ;(list 1 2 3 4 [:a :b :c :d] 5 6 7 8 9)
    ;{:foo 1 :bar \c :baz "str"}
    {:small-value [1 2 3]
     :larger-value ^{:some "meta" :and "such"}
                   {:some-key "foo"
                    :some-other-key "bar"}}
    ;(Person. "Brandon" "Bloom")
    ;(atom (range 20))
    ;fut
    ;#{:foo :bar :baz}
    (pprint {:width 30})))

)
