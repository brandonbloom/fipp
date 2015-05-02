(ns fipp.printer_test
  (:use [clojure.test])
  (:require [fipp.printer :as p]))

;; Tests for doc1 converted from the Haskell in the literature.
;; Group (Text "A" :+: (Line :+: Group (Text "B" :+: (Line :+: Text "C"))))
(def doc1 [:group "A" :line [:group "B" :line "C"]])

(deftest serialize-test
  (testing "Simple"
    (is (= (p/serialize doc1)
           [{:op :begin}
            {:op :text :text "A"}
            {:op :line :inline " "}
            {:op :begin}
            {:op :text :text "B"}
            {:op :line :inline " "}
            {:op :text :text "C"}
            {:op :end}
            {:op :end}])))
  ;;TODO test seq expansion
  )

(deftest annotate-rights-test
  (testing "A.2  Computing the horizontal position"
    (is (= (->> doc1 p/serialize (into [] p/annotate-rights))
           [; Generated: GBeg 0
            {:op :begin :right 0}
            ; Generated: TE 1 "A"
            {:op :text :right 1 :text "A"}
            ; Generated: LE 2
            {:op :line :right 2 :inline " "}
            ; Generated: GBeg 2
            {:op :begin :right 2}
            ; Generated: TE 3 "B"
            {:op :text :right 3 :text "B"}
            ; Generated: LE 4
            {:op :line :right 4 :inline " "}
            ; Generated: TE 5 "C"
            {:op :text :right 5 :text "C"}
            ; Generated: GEnd 5
            {:op :end :right 5}
            ; Generated: GEnd 5
            {:op :end :right 5}]))))

(deftest annotate-begins-test

  (testing "A.3 Determining group widths"
    (is (= (->> (p/serialize doc1)
                (eduction p/annotate-rights (p/annotate-begins {:width 70}))
                vec)
        [; Generated: GBeg 5
         {:op :begin :right 5}
         ; Generated: TE 1 "A"
         {:op :text :right 1 :text "A"}
         ; Generated: LE 2
         {:op :line :right 2 :inline " "}
         ; Generated: GBeg 5
         {:op :begin :right 5}
         ; Generated: TE 3 "B"
         {:op :text :right 3 :text "B"}
         ; Generated: LE 4
         {:op :line :right 4 :inline " "}
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
      (->> (p/serialize doc1)
           (eduction p/annotate-rights
                     (log :in)
                     (p/annotate-begins options)
                     (log :out))
           (run! identity)) ; discard output
      (is (= @acc
             [; trHPP: read: GBeg 0
              [:in {:op :begin :right 0}]
              ; trHPP: read: TE 1 "A"
              [:in {:op :text :right 1 :text "A"}]
              ; trHPP: read: LE 2
              [:in {:op :line :right 2 :inline " "}]
              ; trHPP: read: GBeg 2
              [:in {:op :begin :right 2}]
              ; trHPP: read: TE 3 "B"
              [:in {:op :text :right 3 :text "B"}]
              ; trHPP: read: LE 4
              [:in {:op :line :right 4 :inline " "}]
              ; Generated: GBeg TooFar
              [:out {:op :begin :right :too-far}]
              ; Generated: TE 1 "A"
              [:out {:op :text :right 1 :text "A"}]
              ; Generated: LE 2
              [:out {:op :line :right 2 :inline " "}]
              ; trHPP: read: TE 5 "C"
              [:in {:op :text :right 5 :text "C"}]
              ; trHPP: read: GEnd 5
              [:in {:op :end :right 5}]
              ; Generated: GBeg (Small 5)
              [:out {:op :begin :right 5}]
              ; Generated: TE 3 "B"
              [:out {:op :text :right 3 :text "B"}]
              ; Generated: LE 4
              [:out {:op :line :right 4 :inline " "}]
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
    (p/pprint-document doc {:width width})))

(deftest formatted-test
  (testing "p/pprint-document"
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
