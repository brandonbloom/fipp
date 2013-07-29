(defproject fipp "0.4.1-SNAPSHOT"
  :description "Fast Idiomatic Pretty Printer for Clojure"
  :url "https://github.com/brandonbloom/fipp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1847"]
                 ;; NOTE. Need to lein install the following deps.
                 [org.clojure/core.rrb-vector "0.0.10-SNAPSHOT"]
                 [transduce "0.1.1-SNAPSHOT"]]
  :source-paths ["src/clojure" "src/cljs"]
  :test-paths ["test/clojure"]
  :cljsbuild {:builds {:test {:source-paths ["src/cljs"
                                             "test/cljs"]
                              ;; TODO :advanced
                              :compiler {:optimizations :whitespace
                                         :output-to "out/test.js"}}}})
