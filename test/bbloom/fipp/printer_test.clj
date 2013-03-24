(ns bbloom.fipp.printer_test
  (:use [clojure.test])
  (:require [bbloom.fipp.printer :as p]))

;; Group (Text ”A” :+: (Line :+: Group (Text ”B” :+: (Line :+: Text ”C”))))
(def doc1 [:group "A" :line [:group "B" :line "C"]])

(deftest annotate-rights-test
  (testing "A.2  Computing the horizontal position"
    (is (= (->> doc1 p/serialize p/annotate-rights (into []))
           [; Generated: GBeg 0
            {:op :begin :right 0}
            ; Generated: TE 1 ”A”
            {:op :text :right 1 :text "A"}
            ; Generated: LE 2
            {:op :line :right 2 :inline " "}
            ; Generated: GBeg 2
            {:op :begin :right 2}
            ; Generated: TE 3 ”B”
            {:op :text :right 3 :text "B"}
            ; Generated: LE 4
            {:op :line :right 4 :inline " "}
            ; Generated: TE 5 ”C”
            {:op :text :right 5 :text "C"}
            ; Generated: GEnd 5
            {:op :end :right 5}
            ; Generated: GEnd 5
            {:op :end :right 5}]))))


; A.3 Determining group widths
; Generated: GBeg 5
; Generated: TE 1 ”A”
; Generated: LE 2
; Generated: GBeg 5
; Generated: TE 3 ”B”
; Generated: LE 4
; Generated: TE 5 ”C”
; Generated: GEnd 5
; Generated: GEnd 5


; A.4 Pruning
; ;; page width 3
; trHPP: read: GBeg 0
; trHPP: read: TE 1 ”A”
; trHPP: read: LE 2
; trHPP: read: GBeg 2
; trHPP: read: TE 3 ”B”
; trHPP: read: LE 4
; Generated: GBeg TooFar
; Generated: TE 1 ”A”
; Generated: LE 2
; trHPP: read: TE 5 ”C”
; trHPP: read: GEnd 5
; Generated: GBeg (Small 5)
; Generated: TE 3 ”B”
; Generated: LE 4
; Generated: TE 5 ”C”
; Generated: GEnd 5
; trHPP: read: GEnd 5
; Generated: GEnd 5
