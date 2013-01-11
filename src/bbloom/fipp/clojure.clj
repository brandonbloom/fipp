(ns bbloom.fipp.clojure
  "Provides a pretty document serializer and pprint fn for Clojure code.
  See bbloom.fipp.edn for pretty printing Clojure/EDN data structures"
  (:require [bbloom.fipp.edn :as edn]))

;; TODO port clojure.pprint/code-dispatch

(def pretty edn/pretty)
(def pprint edn/pprint)
