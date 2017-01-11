(ns fipp.repl
  (:require [clojure.repl :as clj]
            [fipp.edn :refer [pprint]]))

(defn pst
  "Like clojure.repl/pst, but with ex-info fipp pretty-printing."
  ([] (pst 12))
  ([e-or-depth]
     (if (instance? Throwable e-or-depth)
       (pst e-or-depth 12)
       (when-let [e *e]
         (pst (clj/root-cause e) e-or-depth))))
  ([^Throwable e depth]
     (binding [*out* *err*]
       (println (str (-> e class .getSimpleName) " " (.getMessage e)))
       (when-let [info (ex-data e)]
         (pprint info))
       (let [st (.getStackTrace e)
             cause (.getCause e)]
         (doseq [el (take depth
                          (remove #(#{"clojure.lang.RestFn"
                                      "clojure.lang.AFn"}
                                     (.getClassName %))
                                  st))]
           (println (str \tab (clj/stack-element-str el))))
         (when cause
           (println "Caused by:")
           (pst cause (min depth
                           (+ 2 (- (count (.getStackTrace cause))
                                   (count st))))))))))
