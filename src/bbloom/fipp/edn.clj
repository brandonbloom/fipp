(ns bbloom.fipp.edn
  "Provides a pretty document serializer and pprint fn for Clojure/EDN forms.
  See bbloom.fipp.clojure for pretty printing Clojure code."
  (:require [bbloom.fipp.printer :as printer :refer (defprinter)]))

(defmulti pretty class)

(defmethod pretty :default [x]
  [:text (pr-str x)])

(defmethod pretty clojure.lang.IPersistentVector [v]
  [:group "[" [:align (interpose :line (map pretty v))] "]"])

(defmethod pretty clojure.lang.ISeq [s]
  [:group "(" [:align (interpose :line (map pretty s))] ")"])

(defn pretty-map [m]
  (let [kvps (map (fn [[k v]]
                    [:span (pretty k) " " (pretty v)])
                  m)]
    [:group "{" [:align (interpose [:span "," :line] kvps)]  "}"]))

(defmethod pretty clojure.lang.IPersistentMap [m]
  (pretty-map m))

(defmethod pretty clojure.lang.IRecord [r]
  [:span "#" (-> r class .getName) (pretty-map r)])

(prefer-method pretty clojure.lang.IRecord clojure.lang.IPersistentMap)

(defmethod pretty clojure.lang.IPersistentSet [s]
  [:group "#{" [:align (interpose :line (map pretty s))] "}"])

;clojure.lang.PersistentQueue pprint-pqueue)


;;; Below here probably belongs in clojure.clj not edn.clj

(defn system-id [obj]
  (Integer/toHexString (System/identityHashCode obj)))

;;TODO these could benefit from a ::unreadable expander

(defmethod pretty clojure.lang.Atom [a]
  [:span "#<Atom@" (system-id a) " " (pretty @a) ">"])

(defmethod pretty java.util.concurrent.Future [f]
  (let [value (if (future-done? f)
                (pretty @f)
                ":pending")]
    [:span "#<Future@" (system-id f) " " value ">"]))


(defprinter pprint pretty
  {:width 70})

(comment

  (defrecord Person [first-name last-name])

  (def fut (future 1))

  (->
    ;(list 1 2 3 4 [:a :b :c :d] 5 6 7 8 9)
    ;{:foo 1 :bar \c :baz "str"}
    ;{:small-value [1 2 3]
    ; :larger-value {:some-key "foo"
    ;                :some-other-key "bar"}}
    ;(Person. "Brandon" "Bloom")
    (atom (range 20))
    ;fut
    (pprint {:width 10}))

)
