(ns obcc.build.interface
  (:refer-clojure :exclude [compile])
  (:import [org.stringtemplate.v4 STGroupFile ST])
  (:import [java.util ArrayList])
  (:require [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [clojure.set :as set]
            [instaparse.core :as insta]
            [obcc.config.parser :as config]))

(def grammar (insta/parser (io/resource "parsers/interface/grammar.bnf")
                           :auto-whitespace (insta/parser (io/resource "parsers/interface/skip.bnf"))))

(defn parse [intf] (->> intf grammar zip/vector-zip))

;;-----------------------------------------------------------------
;; retrieve all "provided" interfaces, adding the implicit
;; "project.cci" and translating "self" to the name of the project
;;-----------------------------------------------------------------
(defn getprovides [config]
  (let [name (->> config (config/find [:configuration :name]) first)
        entries (config/find [:configuration :provides] config)]
    (->> entries flatten (remove nil?) (walk/postwalk-replace {"self" name}) (cons "project") (into #{}))))

(defn getconsumes [config]
  (->> config (config/find [:configuration :consumes]) (remove nil?) (into #{})))

;;-----------------------------------------------------------------
;; aggregate all of the interfaces declared in the config
;;-----------------------------------------------------------------
(defn getinterfaces [config]
  (into '() (set/union (getprovides config) (getconsumes config))))

(defn filename [intf]
  (str intf ".cci"))

;;-----------------------------------------------------------------
;; create a mapping of interface name to interface alias.  By default
;; the alias is the "short name".  E.g. for an interface
;; "com.acme.myinterface", the short name is "myinterface"
;;-----------------------------------------------------------------
(defn aliases [config]
  (into {} (map #(vector % (last (string/split % #"\."))) (getinterfaces config)))) ;; FIXME - support overrides

(defn open [path intf]
  (let [file (io/file path (filename intf))]
    (cond
      (.exists file)
      file

      :else
      (throw (Exception. (str (.getAbsolutePath file) " not found"))))))

;; takes an interface name, maps it to a file, and if present, compiles
;; it to an AST.
(defn compileintf [path intf]
  (let [ipath (str path "/src/interfaces")]
    (->> (open ipath intf) slurp parse)))

;; compile all applicable interfaces into a map of ASTs keyed by interface name
(defn compile [path config]
  (let [interfaces (getinterfaces config)]
    (into {} (map #(vector % (compileintf path %)) interfaces))))
