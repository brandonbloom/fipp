(ns fipp.edn-test
  (:require [clojure.test :refer [deftest is are testing]]
            [fipp.test-util :refer [clean]]
            [fipp.edn :refer [pprint]]
            [fipp.ednize :refer [IEdn IOverride]]))

(defrecord Person [first-name last-name])

(def data [

  (list 1 2 3 4 [:a :b :c :d] 5 6 7 8 9)

  {:foo 1 :bar \c :baz "str"}

  {:small-value [1 2 3]
   :larger-value ^{:some "meta" :and "such"}
                 {:some-key "foo"
                  :some-other-key "bar"}}

  (Person. "Brandon" "Bloom")

  (tagged-literal 'x 5)

  ;;XXX CLJS doesn't report addresses, so these tests fail.
  (atom (range 20))

  ;; CLJS doesn't have futures, so dummy one up.
  #?(:clj (future 1)
     :cljs (tagged-literal 'object ['clojure.core$future_call$reify__6730
                                    "0x31e033f0"
                                    {:status :ready, :val 1}]))

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
           "#'clojure.core/inc\n"))
    (is (= (with-out-str (pprint #"x\?y"))
           "#\"x\\?y\"\n")))
  (testing ":print-length option"
    (is (= (with-out-str (pprint (range) {:print-length 3}))
           "(0 1 2 ...)\n"))
    (is (= (with-out-str (pprint (range 4) {:print-length 4}))
           "(0 1 2 3)\n"))
    (is (= (with-out-str (pprint (range 4) {:print-length 3}))
           "(0 1 2 ...)\n"))
    (is (= (with-out-str (pprint [0 1 2 3] {:print-length 3}))
           "[0 1 2 ...]\n"))
    (is (= (with-out-str (pprint [0 1 2 3] {:print-length 3 :width 2}))
           "[0\n 1\n 2\n ...]\n"))
    (is (= (with-out-str (pprint (into (sorted-map) {:a 0 :b 1 :c 2 :d 3})
                                 {:print-length 3}))
           "{:a 0, :b 1, :c 2, ...}\n"))
    (is (= (with-out-str (pprint (into (sorted-map) {:a 0 :b 1 :c 2 :d 3})
                                 {:print-length 3 :width 2}))
           "{:a 0,\n :b 1,\n :c 2,\n ...}\n"))
    (is (= (with-out-str (pprint (into (sorted-set) [0 1 2 3])
                                 {:print-length 3}))
           "#{0 1 2 ...}\n")))
  (testing ":print-level option"
    (is (= (with-out-str (pprint '(:a (:b (:c (:d)))) {:print-level 3}))
           "(:a (:b (:c (#))))\n"))
    (is (= (with-out-str (pprint '(:a (:b (:c (:d) :e) :f) :g)
                                 {:print-level 3}))
           "(:a (:b (:c (#) :e) :f) :g)\n"))
    (is (= (with-out-str (pprint [:a [:b [:c [:d]]]] {:print-level 3}))
           "[:a [:b [:c [#]]]]\n"))
    (is (= (with-out-str (pprint [:a [:b [:c [:d] :e] :f] :g]
                                 {:print-level 3}))
           "[:a [:b [:c [#] :e] :f] :g]\n"))
    (is (= (with-out-str (pprint (into (sorted-map) {:a {:b {:c {:d nil}}}})
                                 {:print-level 3}))
           "{:a {:b {:c {#}}}}\n"))
    (is (= (with-out-str (pprint #{#{#{#{}}}} {:print-level 3}))
           "#{#{#{#{#}}}}\n")))
  )

;;XXX Is IOverride working correctly for CLJS?
(deftype SomeType []
  #?(:clj clojure.lang.IPersistentMap
     :cljs cljs.core/IMap)
  IEdn
  (-edn [_] :override)
  IOverride)

(deftest override-test
  (is (= (with-out-str (pprint (SomeType.))) ":override\n")))

(comment

  (pprint (tagged-literal 'foo (with-meta [] {:blah true}))
          ;(with-meta [] {:blah true})
          {:print-meta true})

  (println (clean (with-out-str (pprint (atom 1)))))

)
