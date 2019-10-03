(ns fipp.engine-test
  (:require [clojure.test :refer [deftest is are testing]]
            [fipp.engine :as e])
  #?(:clj (:import (java.io StringWriter))))

;; Tests for doc1 converted from the Haskell in the literature.
;; Group (Text "A" :+: (Line :+: Group (Text "B" :+: (Line :+: Text "C"))))
(def doc1 [:group "A" :line [:group "B" :line "C"]])

(deftest serialize-test
  (testing "Simple"
    (is (= (e/serialize doc1)
           [{:op :begin}
            {:op :text :text "A"}
            {:op :line :inline " " :terminate ""}
            {:op :begin}
            {:op :text :text "B"}
            {:op :line :inline " " :terminate ""}
            {:op :text :text "C"}
            {:op :end}
            {:op :end}])))
  (testing ":nest indent defaults to 2"
    (is (= [{:op :nest, :offset 2}
            {:op :text, :text "foo"}
            {:op :outdent}]
           (e/serialize [:nest "foo"])
           (e/serialize [:nest 2 "foo"]))))
  (testing ":align offset defaults to 0"
    (is (= [{:op :align, :offset 0}
            {:op :text, :text "foo"}
            {:op :outdent}]
           (e/serialize [:align "foo"])
           (e/serialize [:align 0 "foo"]))))

  ;;TODO test seq expansion
  )

(deftest annotate-rights-test
  (testing "A.2  Computing the horizontal position"
    (is (= (->> doc1 e/serialize (into [] e/annotate-rights))
           [; Generated: GBeg 0
            {:op :begin :right 0}
            ; Generated: TE 1 "A"
            {:op :text :right 1 :text "A"}
            ; Generated: LE 2
            {:op :line :right 2 :inline " " :terminate ""}
            ; Generated: GBeg 2
            {:op :begin :right 2}
            ; Generated: TE 3 "B"
            {:op :text :right 3 :text "B"}
            ; Generated: LE 4
            {:op :line :right 4 :inline " " :terminate ""}
            ; Generated: TE 5 "C"
            {:op :text :right 5 :text "C"}
            ; Generated: GEnd 5
            {:op :end :right 5}
            ; Generated: GEnd 5
            {:op :end :right 5}]))))

(deftest annotate-begins-test

  (testing "A.3 Determining group widths"
    (is (= (->> (e/serialize doc1)
                (eduction e/annotate-rights (e/annotate-begins {:width 70}))
                vec)
        [; Generated: GBeg 5
         {:op :begin :right 5}
         ; Generated: TE 1 "A"
         {:op :text :right 1 :text "A"}
         ; Generated: LE 2
         {:op :line :right 2 :inline " " :terminate ""}
         ; Generated: GBeg 5
         {:op :begin :right 5}
         ; Generated: TE 3 "B"
         {:op :text :right 3 :text "B"}
         ; Generated: LE 4
         {:op :line :right 4 :inline " " :terminate ""}
         ; Generated: TE 5 "C"
         {:op :text :right 5 :text "C"}
         ; Generated: GEnd 5
         {:op :end :right 5}
         ; Generated: GEnd 5
         {:op :end :right 5}])))

  (testing "A.4 Pruning"
    (let [acc (atom [])
          log (fn [prefix]
                (map (fn [x]
                       (swap! acc conj [prefix x])
                       x)))
          options {:width 3}]
      (->> (e/serialize doc1)
           (eduction e/annotate-rights
                     (log :in)
                     (e/annotate-begins options)
                     (log :out))
           (run! identity)) ; discard output
      (is (= @acc
             [; trHPP: read: GBeg 0
              [:in {:op :begin :right 0}]
              ; trHPP: read: TE 1 "A"
              [:in {:op :text :right 1 :text "A"}]
              ; trHPP: read: LE 2
              [:in {:op :line :right 2 :inline " " :terminate ""}]
              ; trHPP: read: GBeg 2
              [:in {:op :begin :right 2}]
              ; trHPP: read: TE 3 "B"
              [:in {:op :text :right 3 :text "B"}]
              ; trHPP: read: LE 4
              [:in {:op :line :right 4 :inline " " :terminate ""}]
              ; Generated: GBeg TooFar
              [:out {:op :begin :right :too-far}]
              ; Generated: TE 1 "A"
              [:out {:op :text :right 1 :text "A"}]
              ; Generated: LE 2
              [:out {:op :line :right 2 :inline " " :terminate ""}]
              ; trHPP: read: TE 5 "C"
              [:in {:op :text :right 5 :text "C"}]
              ; trHPP: read: GEnd 5
              [:in {:op :end :right 5}]
              ; Generated: GBeg (Small 5)
              [:out {:op :begin :right 5}]
              ; Generated: TE 3 "B"
              [:out {:op :text :right 3 :text "B"}]
              ; Generated: LE 4
              [:out {:op :line :right 4 :inline " " :terminate ""}]
              ; Generated: TE 5 "C"
              [:out {:op :text :right 5 :text "C"}]
              ; Generated: GEnd 5
              [:out {:op :end :right 5}]
              ; trHPP: read: GEnd 5
              [:in {:op :end :right 5}]
              ; Generated: GEnd 5
              [:out {:op :end :right 5}]])))))

(defn ppstr [doc width]
  (with-out-str
    (e/pprint-document doc {:width width})))

(deftest formatted-test
  (testing "e/pprint-document"
    (is (= (ppstr doc1 6) "A B C\n"))
    (is (= (ppstr doc1 5) "A B C\n"))
    (is (= (ppstr doc1 4) "A\nB C\n"))
    (is (= (ppstr doc1 3) "A\nB C\n"))
    (is (= (ppstr doc1 2) "A\nB\nC\n"))
    (is (= (ppstr doc1 1) "A\nB\nC\n"))))

(deftest escaped-test
  (testing "escaped nodes output verbatim, have width 1"
    (is (= (ppstr [:group (repeat 5 [:span "a" :line])] 9)
           (str (apply str (repeat 5 "a\n")) "\n")))
    (is (= (ppstr [:group (repeat 5 [:span "a" :line])] 10)
           (str (apply str (repeat 5 "a ")) "\n")))
    (is (= (ppstr [:group (repeat 5 [:span [:escaped "&#97;"] :line])] 9)
           (str (apply str (repeat 5 "&#97;\n")) "\n")))
    (is (= (ppstr [:group (repeat 5 [:span [:escaped "&#97;"] :line])] 10)
           (str (apply str (repeat 5 "&#97; ")) "\n")))))

(deftest passthrough-node-test
  ;; reminder to reader: :pass nodes are to be used with non-visible characters such as ANSI escape codes
  (testing "indentation without :pass (as baseline for comparison with subsequent tests)"
    (is (= (ppstr [:group
                   "AA"
                   [:align
                    :line
                    "BB"
                    :line
                    "CC"
                    [:align
                     :line
                     "DD"]]] 6)
           (str "AA\n"
                "  BB\n"
                "  CC\n"
                "    DD\n"))))
  (testing ":pass nodes have a width of zero and respect indentation"
    (is (= (ppstr [:group
                   [:span [:pass "<"] "AA" [:pass ">"]]
                   [:align
                    :line
                    [:span [:pass "<"] "BB" [:pass ">>"]]
                    :line
                    [:span [:pass "<<<<<<<<<<"] "CC" [:pass ">>>"]]
                    [:align
                     :line
                     [:span [:pass "<"] "DD" [:pass ">"]]]]] 6)
           (str "<AA>\n"
                "  <BB>>\n"
                "  <<<<<<<<<<CC>>>\n"
                "    <DD>\n"))))
  (testing ":pass nodes can be used to affect indent whitespace by placing them before newline"
    (is (= (ppstr [:group
                   "AA"
                   [:align
                    [:pass "<"]
                    :line
                    "BB"
                    [:pass "<"]
                    :line
                    "CC"
                    [:align
                     [:pass "<"]
                     :line
                     [:span "DD"]]]] 6)
           (str "AA<\n"
                "  BB<\n"
                "  CC<\n"
                "    DD\n")))) )

(deftest terminate-test
  (is (= (ppstr [:group "a" [:line "-" ";"] "b"] 1000)
         "a-b\n"))
  (is (= (ppstr [:group "a" [:line "-" ";"] "b"] 2)
         "a;\nb\n")))

#?(:clj (deftest writer-test
          (testing ":writer option works"
            (let [sw (StringWriter.)]
              (e/pprint-document doc1 {:writer sw})
              (is (= "A B C\n" (str sw)))))

          (testing ":writer option does not interfere with *out*"
            (let [sw1 (StringWriter.)
                  sw2 (StringWriter.)]
              (binding [*out* sw1]
                (e/pprint-document doc1 {:writer sw2}))
              (is (= "" (str sw1)))
              (is (= "A B C\n" (str sw2))))

            (let [doc (->> (repeatedly (fn []
                                         (print "foo")
                                         "bar"))
                           (take 3))
                  sw1 (StringWriter.)
                  sw2 (StringWriter.)]
              (binding [*out* sw1]
                (e/pprint-document doc {:writer sw2}))
              (is (= "foofoofoo" (str sw1)))
              (is (= "barbarbar\n" (str sw2)))))))

(deftest print-dup-test
  (testing "Not affected by *print-dup* binding"
    (is (= "A B C\n"
           (with-out-str
             (binding [*print-dup* true]
               (e/pprint-document doc1)))))))

#?(:cljs (deftest print-fn-test
           (testing ":print-fn option works"
             (let [res (volatile! "")]
               (e/pprint-document doc1 {:print-fn #(vswap! res str %)})
               (is (= "A B C" @res))))

           (testing ":print-fn option does not interfere with *print-fn*"
             (let [res1 (volatile! "")
                   res2 (volatile! "")]
               (binding [*print-fn* #(vswap! res1 str %)]
                 (e/pprint-document doc1 {:print-fn #(vswap! res2 str %)}))
               (is (= "" @res1))
               (is (= "A B C" @res2)))

             (let [doc (->> (repeatedly (fn []
                                          (print "foo")
                                          "bar"))
                            (take 3))
                   res1 (volatile! "")
                   res2 (volatile! "")]
               (binding [*print-fn* #(vswap! res1 str %)]
                 (e/pprint-document doc {:print-fn #(vswap! res2 str %)}))
               (is (= "foofoofoo" @res1))
               (is (= "barbarbar" @res2))))))
