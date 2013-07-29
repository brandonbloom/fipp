(ns fipp.edn
  "Provides a pretty document serializer and pprint fn for Clojure/EDN forms.
  See fipp.clojure for pretty printing Clojure code."
  (:require [fipp.printer :as printer :refer (pprint-document)])
  (:require-macros [fipp.macros :refer [defprinter]]))

;;TODO Figure out what belongs in clojure.clj instead of edn.clj

(defprotocol IPretty
  (-pretty [x]))

(defn system-id [obj]
  ;; TODO: What to put here? This is not trivial in js I think.
  )

(defn pretty-map [m]
  (let [kvps (for [[k v] m]
               [:span (-pretty k) " " (-pretty v)])
        doc [:group "{" [:align (interpose [:span "," :line] kvps)]  "}"]]
    (if (satisfies? IRecord m)
      [:span "#" (-> m type pr-str) doc]
      doc)))

(extend-protocol IPretty

  nil
  (-pretty [x]
    [:text "nil"])

  js/Object
  (-pretty [x]
    [:text (pr-str x)])

  ;; IVector
  ;; TODO: Subvec, BlackNode(?), RedNode(?)
  PersistentVector
  (-pretty [v]
    [:group "[" [:align (interpose :line (map -pretty v))] "]"])

  ;; ISeq 
  ;; TODO: IndexedSeq, RSeq, EmptyList, Cons, LazySeq,
  ;; ChunkedCons, ChunkedSeq, PersistentQueueSeq, PersistentQueue(?),
  ;; PersistentArrayMapSeq, NodeSeq, ArrayNodeSeq,
  ;; PersistentTreeMapSeq, KeySeq, ValSeq, Range,
  List
  (-pretty [s]
    [:group "(" [:align (interpose :line (map -pretty s))] ")"])
    
  ;; IMap
  ObjMap
  (-pretty [m]
    (pretty-map m))

  PersistentArrayMap
  (-pretty [m]
    (pretty-map m))

  PersistentHashMap
  (-pretty [m]
    (pretty-map m))

  PersistentTreeMap
  (-pretty [m]
    (pretty-map m))

  ;; ISet
  PersistentHashSet
  (-pretty [s]
    [:group "#{" [:align (interpose :line (map -pretty s)) ] "}"])

  PersistentTreeSet
  (-pretty [s]
    [:group "#{" [:align (interpose :line (map -pretty s)) ] "}"])

  
  Atom
  (-pretty [a]
    [:span "#<Atom: " (-pretty @a) ">"])

  ;; java.util.concurrent.Future
  ;; (-pretty [f]
  ;;   (let [value (if (future-done? f)
  ;;                 (-pretty @f)
  ;;                 ":pending")]
  ;;     [:span "#<Future@" (system-id f) " " value ">"]))

  ;; ;; TODO clojure.lang.PersistentQueue, lots more stuff too
  )

(defn pretty [x]
  (-pretty x))

(defprinter pprint pretty
  {:width 70})
