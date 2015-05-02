(ns fipp.printer_test
  (:use [clojure.test])
  (:require [clojure.string :as str]
            [fipp.edn :refer [pprint]]))

(defrecord Person [first-name last-name])


(defn clean [s]
  (-> s str/trim (str/replace #"\"0x[a-f0-9]+\"" "\"0xDEADBEEF\"")))

(def data [

  (list 1 2 3 4 [:a :b :c :d] 5 6 7 8 9)

  {:foo 1 :bar \c :baz "str"}

  {:small-value [1 2 3]
   :larger-value ^{:some "meta" :and "such"}
                 {:some-key "foo"
                  :some-other-key "bar"}}

  (Person. "Brandon" "Bloom")

  (tagged-literal 'x 5)

  (atom (range 20))

  (future 1)

  #{:foo :bar :baz}

  ])

(def wide (clean "
[(1 2 3 4 [:a :b :c :d] 5 6 7 8 9)
 {:foo 1, :bar \\c, :baz \"str\"}
 {:small-value [1 2 3],
  :larger-value {:some-key \"foo\", :some-other-key \"bar\"}}
 #fipp.printer_test.Person{:first-name \"Brandon\", :last-name \"Bloom\"}
 #x 5
 #object[clojure.lang.Atom
         \"0xc4ca69f\"
         {:status :ready,
          :val (0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19)}]
 #object[clojure.core$future_call$reify__6730
         \"0x31e033f0\"
         {:status :ready, :val 1}]
 #{:baz :bar :foo}]
"))

(def narrow (clean "
[(1
  2
  3
  4
  [:a :b :c :d]
  5
  6
  7
  8
  9)
 {:foo 1, :bar \\c, :baz \"str\"}
 {:small-value [1 2 3],
  :larger-value {:some-key \"foo\",
                 :some-other-key \"bar\"}}
 #fipp.printer_test.Person{:first-name \"Brandon\",
                           :last-name \"Bloom\"}
 #x 5
 #object[clojure.lang.Atom
         \"0x44b300a7\"
         {:status :ready,
          :val (0
                1
                2
                3
                4
                5
                6
                7
                8
                9
                10
                11
                12
                13
                14
                15
                16
                17
                18
                19)}]
 #object[clojure.core$future_call$reify__6730
         \"0x43eff72d\"
         {:status :ready,
          :val 1}]
 #{:baz :bar :foo}]
"))

(deftest pprint-edn
  (is (= (clean (with-out-str (pprint data {:width 70}))) wide))
  (is (= (clean (with-out-str (pprint data {:width 30}))) narrow)))

(comment

  (println (clean (with-out-str (pprint data {:width 70}))))

)
