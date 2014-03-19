(ns fipp.edn-test
  (:require
   [clojure.test :refer :all]
   [fipp.edn :refer [-pretty pprint]]))

(deftest with-meta-test
  (is (= "{}\n"
         (with-out-str (pprint ^:k {}))))
  (is (= [:group
          [:group "^"
           [:group "{"
            [:align [[:span [:text ":k"] " " [:text "true"]]]]
            "}"]]
          " "
          [:group "#{" [:align ()] "}"]]
         (binding [*print-meta* true]
           (-pretty ^:k #{}))))
  (is (= "^{:k true} {}\n"
         (binding [*print-meta* true]
           (with-out-str (pprint ^:k {})))))
  (is (= "^{:k true} #{}\n"
         (binding [*print-meta* true]
           (with-out-str (pprint ^:k #{})))))
  (is (= "^{:k true} []\n"
         (binding [*print-meta* true]
           (with-out-str (pprint ^:k [])))))
  (is (.startsWith (binding [*print-meta* true]
                     (with-out-str (pprint (with-meta (future nil) {:k true}))))
                   "^{:k true} ")))
