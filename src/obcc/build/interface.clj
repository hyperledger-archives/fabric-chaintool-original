(ns obcc.build.interface
  (:import [org.stringtemplate.v4 STGroupFile ST])
  (:import [java.util ArrayList])
  (:require [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [instaparse.core :as insta]
            [obcc.config.parser :as config]))

(def grammar (insta/parser (io/resource "parsers/interface/grammar.bnf")
                           :auto-whitespace (insta/parser (io/resource "parsers/interface/skip.bnf"))))

(defn parse [intf] (->> intf grammar zip/vector-zip))

;;-----------------------------------------------------------------
;; aggregate all of the interfaces declared in the config, adding the implicit
;; "project.cci" and translating "self" to the name of the project
;;-----------------------------------------------------------------
(defn getinterfaces [config]
  (let [name (->> config (config/find [:configuration :name]) first)
        keys [[:configuration :provides] [:configuration :consumes]]
        explicit (map #(config/find % config) keys)]
    (->> explicit flatten (into #{}) (walk/postwalk-replace {"self" name}) (cons "project") (remove nil?) (into '()))))

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

;;-----------------------------------------------------------------
;; getX - helper functions to extract fields from an AST message
;;-----------------------------------------------------------------
(defn getfieldattrs [ast]
  (loop [loc ast attrs {}]
    (if (nil? loc)
      attrs
      ;; else
      (let [[k v] (zip/node loc)]
        (recur (zip/right loc) (assoc attrs k v))))))

(defn getfields [ast]
  (loop [loc ast fields {}]
    (cond

      (nil? loc)
      fields

      :else
      (let [attrs (->> loc zip/down zip/right getfieldattrs)]
        (recur (zip/right loc) (assoc fields (:index attrs) attrs))))))
