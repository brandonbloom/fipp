(ns fipp.clojure
  "Provides a pretty document serializer and pprint fn for Clojure code.
  See fipp.edn for pretty printing Clojure/EDN data structures"
  (:require [clojure.walk :as walk]
            [fipp.engine :refer (pprint-document)]))

;;TODO leverage fipp.edn


(defprotocol IPretty
  (-pretty [x ctx]))

(defn pretty [x ctx]
  (if-let [m (and (:print-meta ctx) (meta x))]
    [:align [:span "^" (-pretty m ctx)] :line (-pretty x ctx)]
    (-pretty x ctx)))


;;; Helper functions

(defn block [nodes]
  [:nest 2 (interpose :line nodes)])

(defn list-group [& nodes]
  [:group "(" nodes ")"])

(defn maybe-a [pred xs]
  (let [x (first xs)] (if (pred x) [x (rest xs)] [nil xs])))


;;; Format case, cond, condp

(defn pretty-cond-clause [[test result] ctx]
  [:group (-pretty test ctx) :line [:nest 2 (pretty result ctx)]])

(defn pretty-case [[head expr & more] ctx]
  (let [clauses (partition 2 more)
        default (when (odd? (count more)) (last more))]
    (list-group
      (-pretty head ctx) " " (pretty expr ctx) :line
      (block (concat (map #(pretty-cond-clause % ctx) clauses)
                     (when default [(pretty default ctx)]))))))

(defn pretty-cond [[head & more] ctx]
  (let [clauses (partition 2 more)]
    (list-group
      (-pretty head ctx) :line
      (block (map #(pretty-cond-clause % ctx) clauses)))))

;;TODO this will get tripped up by ternary (test :>> result) clauses
(defn pretty-condp [[head pred expr & more] ctx]
  (let [clauses (partition 2 more)
        default (when (odd? (count more)) (last more))]
    (list-group
      (-pretty head ctx) " " (-pretty pred ctx) " " (pretty expr ctx) :line
      (block (concat (map #(pretty-cond-clause % ctx) clauses)
                     (when default [(pretty default ctx)]))))))


;;; Format arrows, def, if, and similar

(defn pretty-arrow [[head & stmts] ctx]
  (list-group
    (-pretty head ctx) " "
    [:align (interpose :line (map #(pretty % ctx) stmts))]))

;;TODO we're also using this to format def – should that be separate?
(defn pretty-if [[head test & more] ctx]
  (list-group
    (-pretty head ctx) " " (pretty test ctx) :line
    (block (map #(pretty % ctx) more))))


;;; Format defn, fn, and similar

(defn pretty-method [[params & body] ctx]
  (list-group
    (-pretty params ctx) :line
    (block (map #(pretty % ctx) body))))

(defn pretty-defn [[head fn-name & more] ctx]
  (let [[docstring more] (maybe-a string? more)
        [attr-map more]  (maybe-a map?    more)
        [params body]    (maybe-a vector? more)
        params-on-first-line?  (and params (nil? docstring) (nil? attr-map))
        params-after-attr-map? (and params (not params-on-first-line?))]
    (list-group
      (concat [(-pretty head ctx) " " (pretty fn-name ctx)]
              (when params-on-first-line? [" " (-pretty params ctx)]))
      :line
      (block (concat (when docstring [(-pretty docstring ctx)])
                     (when attr-map  [(-pretty attr-map ctx)])
                     (when params-after-attr-map? [(-pretty params ctx)])
                     (if body (map #(pretty % ctx) body)
                              (map #(pretty-method % ctx) more)))))))

(defn pretty-fn [[head & more] ctx]
  (let [[fn-name more] (maybe-a symbol? more)
        [params body]  (maybe-a vector? more)]
    (list-group
      (concat [(-pretty head ctx)]
              (when fn-name [" " (pretty fn-name ctx)])
              (when params  [" " (-pretty params ctx)]))
      :line
      (block (if body (map #(pretty % ctx) body)
                      (map #(pretty-method % ctx) more))))))

(defn pretty-fn* [[_ params body :as form] ctx]
  (if (and (vector? params) (seq? body))
    (let [[inits rests] (split-with #(not= % '&) params)
          params* (merge (if (= (count inits) 1)
                           {(first inits) '%}
                           (zipmap inits (map #(symbol (str \% (inc %))) (range))))
                         (when (seq rests) {(second rests) '%&}))
          body* (walk/prewalk-replace params* body)]
      [:group "#(" [:align 2 (interpose :line (map #(pretty % ctx) body*))] ")"])
    (pretty-fn form ctx)))


;;; Format ns

(defn pretty-libspec [[head & clauses] ctx]
  (list-group
    (-pretty head ctx) " "
    [:align (interpose :line (map #(pretty % ctx) clauses))]))

(defn pretty-ns [[head ns-sym & more] ctx]
  (let [[docstring more] (maybe-a string? more)
        [attr-map specs] (maybe-a map?    more)]
    (list-group
      (-pretty head ctx) " " (pretty ns-sym ctx) :line
      (block (concat (when docstring [(-pretty docstring ctx)])
                     (when attr-map  [(-pretty attr-map ctx)])
                     (map #(pretty-libspec % ctx) specs))))))


;;; Format deref, quote, unquote, var

(defn pretty-quote [[macro arg] ctx]
  [:span (case (keyword (name macro))
           :deref "@", :quote "'", :unquote "~", :var "#'")
         (pretty arg ctx)])

;;; Format let, loop, and similar

(defn pretty-bindings [bvec ctx]
  (let [kvps (for [[k v] (partition 2 bvec)]
               [:span (-pretty k ctx) " " [:align (pretty v ctx)]])]
    [:group "[" [:align (interpose [:line ", "] kvps)] "]"]))

(defn pretty-let [[head bvec & body] ctx]
  (list-group
    (-pretty head ctx) " " (pretty-bindings bvec ctx) :line
    (block (map #(pretty % ctx) body))))


;;; Types and interfaces

(defn pretty-impls [opts+specs ctx]
  ;;TODO parse out opts
  ;;TODO parse specs and call pretty on methods
  (block (map #(-pretty % ctx) opts+specs)))

(defn pretty-type [[head fields & opts+specs] ctx]
  (list-group (-pretty head ctx) " " (pretty fields ctx) :line
              (pretty-impls opts+specs ctx)))

(defn pretty-reify [[head & opts+specs] ctx]
  (list-group (-pretty head ctx) :line
              (pretty-impls opts+specs ctx)))


;;; Symbol table

(defn build-symbol-map [dispatch]
  (into {} (for [[pretty-fn syms] dispatch
                 sym syms
                 sym (cons sym (when-not (special-symbol? sym)
                                 [(symbol "clojure.core" (name sym))]))]
             [sym pretty-fn])))

(def default-symbols
  (build-symbol-map
    {pretty-arrow '[. .. -> ->> and doto or some-> some->>]
     pretty-case  '[case cond-> cond->>]
     pretty-cond  '[cond]
     pretty-condp '[condp]
     pretty-defn  '[defmacro defmulti defn defn-]
     pretty-fn    '[fn]
     pretty-fn*   '[fn*]
     pretty-if    '[def defonce if if-not when when-not]
     pretty-ns    '[ns]
     pretty-let   '[binding doseq dotimes for if-let if-some let let* loop loop*
                    when-first when-let when-some with-local-vars with-open with-redefs]
     pretty-quote '[deref quote unquote var]
     pretty-type  '[deftype defrecord]
     pretty-reify '[reify]}))


;;; Data structures

(extend-protocol IPretty

  nil
  (-pretty [x ctx]
    [:text "nil"])

  java.lang.Object
  (-pretty [x ctx]
    [:text (pr-str x)])

  clojure.lang.IPersistentVector
  (-pretty [v ctx]
    [:group "[" [:align (interpose [:group :line] (map #(pretty % ctx) v))] "]"])

  clojure.lang.ISeq
  (-pretty [s ctx]
    (if-let [pretty-special (get (:symbols ctx) (first s))]
      (pretty-special s ctx)
      (list-group [:align 1 (interpose :line (map #(pretty % ctx) s))])))

  clojure.lang.IPersistentMap
  (-pretty [m ctx]
    (let [kvps (for [[k v] m]
                 [:span (-pretty k ctx) " " (pretty v ctx)])
          doc [:group "{" [:align (interpose [:span "," :line] kvps)]  "}"]]
      (if (record? m)
        [:span "#" (-> m class .getName) doc]
        doc)))

  clojure.lang.IPersistentSet
  (-pretty [s ctx]
    [:group "#{" [:align (interpose [:group :line] (map #(pretty % ctx) s))] "}"])

  ;TODO clojure.pprint supports clojure.lang.{IDeref,PersistentQueue} – do we want these?

  )


(defn pprint
  ([x] (pprint x {}))
  ([x options]
   (let [symbols (:symbols options default-symbols)
         ctx (merge {:symbols symbols :print-meta *print-meta*}
                    (dissoc options :symbols))]
     (binding [*print-meta* false]
       (pprint-document (pretty x ctx) options)))))
