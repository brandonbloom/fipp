(ns fipp.deque
  "Double-sided queue built on 2-3 finger trees."
  (:refer-clojure :exclude [empty concat])
  (:require [clojure.data.finger-tree :as ftree]))

(def create ftree/double-list)

(def empty (create))

(def conjl (fnil ftree/conjl empty))

(def conjr (fnil conj empty))

(defn conjlr [l deque r]
  (conj (conjl deque l) r))

(def concat ftree/ft-concat)
