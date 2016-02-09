(ns obcc.util
  (:import (java.io File))
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojurewerkz.propertied.properties :as props]))

;;(defn load-props [file-name] (->> file-name io/resource props/load-from props/properties->map))
(defn load-props [file-name] {})

(defn app-props [] (load-props "application.properties"))

(def app-version ((app-props) "version"))
(def configname "chaincode.conf")
(def supportpath "build/src/chaincode_support")

;;-----------------------------------------------------------------
;; manage object names
;;-----------------------------------------------------------------
(defn qualifyname [namespace name]
  (if namespace
    (str (string/replace namespace "." "_") "_" name)
    name))
