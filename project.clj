(defproject fipp "0.6.9"
  :description "Fast Idiomatic Pretty Printer for Clojure"
  :url "https://github.com/brandonbloom/fipp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.rrb-vector "0.0.11"]]
  :profiles {:dev {:dependencies [[criterium "0.4.3"]]}})
