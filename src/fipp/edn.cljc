(ns fipp.edn
  "Provides a pretty document serializer and pprint fn for Clojure/EDN forms.
  See fipp.clojure for pretty printing Clojure code."
  (:require [fipp.ednize :refer [edn record->tagged]]
            [fipp.visit :refer [visit visit*]]
            [fipp.engine :refer (pprint-document)]))

(defn- ->group [{:keys [print-length] :as printer} left-separator lines right-separator]
  [:group left-separator
          [:align lines]
          (when (and print-length (<= print-length (count lines)))
            " ...")
          right-separator])

(defn- print-length-xform [{:keys [print-length] :as printer}]
  (if print-length
    (take print-length)
    identity))

(defrecord EdnPrinter [print-meta symbols print-length]

  fipp.visit/IVisitor


  (visit-unknown [this x]
    (visit this (edn x)))


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
      (let [xform (comp (print-length-xform this)
                        (map #(visit this %))
                        (interpose :line))]
        (->group this "(" (sequence xform x) ")"))))

  (visit-vector [this x]
    (let [xform (comp (print-length-xform this)
                      (map #(visit this %))
                      (interpose :line))]
      (->group this "[" (sequence xform x) "]")))

  (visit-map [this x]
    (let [xform (comp (print-length-xform this)
                      (map (fn [[k v]]
                             [:span (visit this k) " " (visit this v)]))
                      (interpose [:span "," :line]))]
      (->group this "{" (sequence xform x) "}")))

  (visit-set [this x]
    (let [xform (comp (print-length-xform this)
                      (map #(visit this %))
                      (interpose :line))]
      (->group this "#{" (sequence xform x) "}")))

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

  (visit-record [this x]
    (visit this (record->tagged x)))

  )

(defn pprint
  ([x] (pprint x {}))
  ([x options]
   (let [printer (map->EdnPrinter (merge {:print-length *print-length*
                                          :print-meta *print-meta*
                                          :symbols {}}
                                         options))]
     (binding [*print-meta* false]
       (pprint-document (visit printer x) options)))))
