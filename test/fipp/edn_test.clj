(ns fipp.edn-test
  (:use [clojure.test])
  (:require [clojure.string :as str]
            [fipp.edn :refer [pprint]]))

(defrecord Person [first-name last-name])


(defn clean [s]
  (-> s
    str/trim
    (str/replace #"\"0x[a-f0-9]+\"" "\"0xDEADBEEF\"")
    (str/replace #"reify__[0-9]+" "reify__123")))

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
 #fipp.edn_test.Person{:first-name \"Brandon\", :last-name \"Bloom\"}
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
 #fipp.edn_test.Person{:first-name \"Brandon\",
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

(def mdata (tagged-literal 'foo (with-meta [] {:x 1})))

(deftest pprint-edn
  (testing "Pretty printing Edn without metadata"
    (is (= (clean (with-out-str (pprint data {:width 70}))) wide))
    (is (= (clean (with-out-str (pprint data {:width 30}))) narrow)))
  (testing "Pretty printing metadata"
    (is (= (clean (with-out-str (pprint mdata {:width 70 :print-meta true})))
           "#foo ^{:x 1} []")))
  (testing "Not quite Edn"
    (is (= (with-out-str (pprint #'inc))
           "#'clojure.core/inc\n")))
    (is (= (with-out-str (pprint #"x\?y")
           "#\"x\\?y\"")))
  )

(comment

  (pprint (tagged-literal 'foo (with-meta [] {:blah true}))
          ;(with-meta [] {:blah true})
          {:print-meta true})

  (println (clean (with-out-str (pprint (atom 1)))))

)
