(defproject fipp "0.6.28"
  :description "Fast Idiomatic Pretty Printer for Clojure"
  :url "https://github.com/brandonbloom/fipp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.rrb-vector "0.1.2"]]
  ;; See <https://github.com/brandonbloom/fipp/issues/72#issuecomment-576816826>
  ;; and test code in <.github/workflows/pr.yml>.
  :eval-in-leiningen ~(= "true" (System/getenv "EVAL_IN_LEININGEN"))
  :profiles {:dev {:dependencies [[criterium "0.4.3"]
                                  [org.clojure/clojurescript "1.9.854"]
                                  [javax.xml.bind/jaxb-api "2.3.1"]]}}
  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :sign-releases false}]])
