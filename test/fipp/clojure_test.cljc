(ns fipp.clojure-test
  (:require [clojure.test :refer [deftest is are testing]]
            [clojure.string :as str]
            [fipp.test-util :refer [clean]]
            [fipp.clojure :refer [pprint]]))

(deftest symbols-test
  (testing "Some symbols are treated specially."
    (is (.startsWith (with-out-str (pprint '(xx test then else) {:width 10}))
                     "(xx\n"))
    (is (.startsWith (with-out-str (pprint '(if test then else) {:width 10}))
                     "(if test"))))

(deftest meta-test
  (testing "metadata is omitted from identities"
    (is (= (clean (with-out-str (pprint [#'inc (with-meta 'x {:y 1})]
                                        {:print-meta true})))
           #?(:clj "[#'clojure.core/inc ^{:y 1} x]"
              :cljs "[#'cljs.core/inc ^{:y 1} x]")))))

(deftest lossy-reader-unexpansion-test
  (is (= (clean (with-out-str (pprint '(let [deref foo] (deref foo)))))
         "(let [deref foo] (deref foo))"))
  (is (= (clean (with-out-str (pprint '~(deref foo))))
         "~(deref foo)"))
  (is (= (clean (with-out-str (pprint '(clojure.core/deref))))
         "(clojure.core/deref)"))
  (is (= (clean (with-out-str (pprint '(clojure.core/deref 1 2 3))))
         "(clojure.core/deref 1 2 3)"))
  (is (= (clean (with-out-str (pprint '(clojure.core/unquote))))
         "(clojure.core/unquote)"))
  (is (= (clean (with-out-str (pprint '(clojure.core/unquote 1 2 3))))
         "(clojure.core/unquote 1 2 3)"))
  (is (= (clean (with-out-str (pprint '(unquote (deref foo)))))
         "(unquote (deref foo))")))

(deftest quote-deref-test
  (testing "~(deref) is not ~@"
    (is (= (clean (with-out-str (pprint '(clojure.core/unquote (clojure.core/deref foo)))))
           "~ @foo"))
    (is (= (clean (with-out-str (pprint '~(clojure.core/deref foo))))
           "~ @foo"))
    (is (= (clean (with-out-str (pprint '~(clojure.core/deref foo))))
           "~ @foo"))
    (is (= (clean (with-out-str (pprint '~ @foo)))
           "~ @foo"))
    (is (= (clean (with-out-str (pprint '~,@foo)))
           "~ @foo"))))

;;TODO lots more tests
