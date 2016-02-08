(ns obcc.util
  (:require [clojure.string :as string]))

(def configname "chaincode.conf")
(def supportpath "build/src/chaincode_support")

;;-----------------------------------------------------------------
;; manage object names
;;-----------------------------------------------------------------
(defn qualifyname [namespace name]
  (if namespace
    (str (string/replace namespace "." "_") "_" name)
    name))
