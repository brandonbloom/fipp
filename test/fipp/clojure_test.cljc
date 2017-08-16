(ns fipp.clojure-test
  #?(:clj (:use [clojure.test])
     :cljs (:require-macros [cljs.test :refer [deftest is are testing]]))
  (:require [clojure.string :as str]
            [fipp.clojure :refer [pprint]]))

(deftest symbols-test
  (testing "Some symbols are treated specially."
    (is (.startsWith (with-out-str (pprint '(xx test then else) {:width 10}))
                     "(xx\n"))
    (is (.startsWith (with-out-str (pprint '(if test then else) {:width 10}))
                     "(if test"))))

;;TODO lots more tests
