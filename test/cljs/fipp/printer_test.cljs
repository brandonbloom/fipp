(ns fipp.printer_test
  (:require [fipp.printer :as p]
            [fipp.edn :as e]))

;; TODO: How to test this stuff?

(set-print-fn! js/print)

(def ex1 [:group "["
          [:nest 2
           [:line ""] "0,"
           :line "1,"
           :line "2,"
           :line "3"
           [:line ""]]"]"])

;; (print (with-out-str ...)) is needed
;; since js/print is println

(print
 (with-out-str
   (p/pprint-document ex1 {:width 20})))

(print 
 (with-out-str
   (p/pprint-document ex1 {:width 6})))

(def ex2 [:span "["
          [:align
           [:group [:line ""]] "0,"
           [:group :line] "1,"
           [:group :line] "2,"
           [:group :line] "3"]"]"])

(print 
 (with-out-str
   (p/pprint-document ex2 {:width 20})))

(print 
 (with-out-str
   (p/pprint-document ex2 {:width 6})))

(defrecord Person [first-name last-name])

(print
 (with-out-str
   (e/pprint ['#{a b c d e} 
              :b
              (atom nil)
              '(1 2 3 4 5) 
              (Person. "John" "Doe") 
              {:g 1 :h 2}])))
