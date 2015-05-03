(ns fipp.edn
  "Provides a pretty document serializer and pprint fn for Clojure/EDN forms.
  See fipp.clojure for pretty printing Clojure code."
  (:require [fipp.visit :refer [IVisitor visit visit*]]
            [fipp.engine :refer (pprint-document)]))

(defrecord EdnPrinter [print-meta symbols]

  IVisitor


  (visit-unknown [this x]
    (visit this (fipp.visit/-edn x)))


  (visit-nil [this]
    [:text "nil"])

  (visit-boolean [this x]
    [:text (str x)])

  (visit-string [this x]
    [:text (pr-str x)])

  (visit-character [this x]
    [:text (pr-str x)])

  (visit-symbol [this x]
    [:text (str x)])

  (visit-keyword [this x]
    [:text (str x)])

  (visit-number [this x]
    [:text (pr-str x)])

  (visit-seq [this x]
    (if-let [pretty (symbols (first x))]
      (pretty this x)
      [:group "(" [:align (interpose :line (map #(visit this %) x))] ")"]))

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
            (when (or (and print-meta (meta form))
                      (not (coll? form)))
              " ")
            (visit this form)])


  (visit-meta [this m x]
    (if print-meta
      [:align [:span "^" (visit this m)] :line (visit* this x)]
      (visit* this x)))

  (visit-var [this x]
    [:text (str x)])

  (visit-pattern [this x]
    [:text (pr-str x)])


  )

(defn pprint
  ([x] (pprint x {}))
  ([x options]
   (let [printer (map->EdnPrinter (merge {:print-meta *print-meta*
                                          :symbols {}}
                                         options))]
     (binding [*print-meta* false]
       (pprint-document (visit printer x) options)))))
