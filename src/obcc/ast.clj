(ns obcc.ast
  (:refer-clojure :exclude [find])
  (:require [clojure.zip :as zip]))

;;-----------------------------------------------------------------
;; find a specific element in the AST
;;-----------------------------------------------------------------
(defn find [term ast]
  (loop [loc ast]
    (cond

      (or (nil? loc) (zip/end? loc))
      nil

      (= (zip/node loc) term)
      (zip/up loc)

      :else
      (recur (zip/next loc)))))
