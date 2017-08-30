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

;;TODO lots more tests
