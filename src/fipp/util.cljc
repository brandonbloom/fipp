(ns fipp.util
  (:refer-clojure :exclude [boolean? char? regexp?]))

;;TODO: CLJ-1719 and CLJS-1241
(defn boolean? [x]
  (or (true? x) (false? x)))

#?(:cljs (defn char? [x]
           false)
   :clj (def char? clojure.core/char?))

;;TODO: CLJ-1720 and CLJS-1242
#?(:clj (defn regexp? [x]
          (instance? java.util.regex.Pattern x))
   :cljs (def regexp? cljs.core/regexp?))

(defn edn?
  "Is the root of x an edn type?"
  [x]
  (or (nil? x)
      (boolean? x)
      (string? x)
      (char? x)
      (symbol? x)
      (keyword? x)
      (number? x)
      (seq? x)
      (vector? x)
      (record? x)
      (map? x)
      (set? x)
      (tagged-literal? x)
      (var? x)
      (regexp? x)))

(defn value-obj? [x]
  #?(:clj (instance? clojure.lang.IObj x)
     :cljs (and (satisfies? IWithMeta x)
                (not (var? x))))) ;TODO: CLJS-2398
