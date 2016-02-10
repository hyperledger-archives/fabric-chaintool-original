(ns obcc.util
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(def app-version (System/getProperty "obcc.version"))
(def configname "chaincode.conf")
(def supportpath "build/src/chaincode_support")

;;-----------------------------------------------------------------
;; manage object names
;;-----------------------------------------------------------------
(defn qualifyname [namespace name]
  (if namespace
    (str (string/replace namespace "." "_") "_" name)
    name))
