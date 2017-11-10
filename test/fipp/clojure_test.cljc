(ns fipp.clojure-test
  (:require [clojure.test :refer [deftest is are testing]]
            [clojure.string :as str]
            [fipp.clojure :refer [pprint]]))

(deftest symbols-test
  (testing "Some symbols are treated specially."
    (is (.startsWith (with-out-str (pprint '(xx test then else) {:width 10}))
                     "(xx\n"))
    (is (.startsWith (with-out-str (pprint '(if test then else) {:width 10}))
                     "(if test"))))

(deftest meta-test
  (testing "metadata is omitted from identities"
    (is (= (with-out-str (pprint [#'inc (with-meta 'x {:y 1})]
                                 {:print-meta true}))
           "[#'clojure.core/inc ^{:y 1} x]\n"))))

;;TODO lots more tests
