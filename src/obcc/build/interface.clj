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
            [obcc.ast :as ast]
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

(defn open [path intf]
  (let [file (io/file path (filename intf))]
    (cond
      (.exists file)
      file

      :else
      (throw (Exception. (str (.getAbsolutePath file) " not found"))))))

;;-----------------------------------------------------------------
;; getX - helper functions to extract messages/fields from an AST message
;;-----------------------------------------------------------------
(defn getattrs [ast]
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
      (let [attrs (->> loc zip/down zip/right getattrs)]
        (recur (zip/right loc) (assoc fields (:index attrs) attrs))))))

(defn getmessage [ast]
  (let [name (->> ast zip/right zip/node)
        fields (getfields (->> ast zip/right zip/right))]
      (vector name fields)))

(defn getmessages [interface]
  (into {} (loop [loc interface msgs '()]
             (cond

               (or (nil? loc) (zip/end? loc))
               msgs

               :else
               (let [node (->> loc zip/node)]
                 (recur (->> loc zip/next)
                        (if (= node :message)
                          (cons (getmessage loc) msgs)
                          msgs)))))))

;;-----------------------------------------------------------------
;; takes an interface name, maps it to a file, and if present, compiles
;; it to an AST.
;;-----------------------------------------------------------------
(defn compileintf [path intf]
  (let [ipath (str path "/src/interfaces")]
    (->> (open ipath intf) slurp parse)))

;;-----------------------------------------------------------------
;; returns true if the interface contains a message named "Init"
;;-----------------------------------------------------------------
(defn initmsg? [ast]
  (let [msgs (getmessages ast)]
    (if (msgs "Init")
      true
      false)))

;;-----------------------------------------------------------------
;; sythesize the project::Init() transaction
;;
;; We allow the user to omit declaring an explicit transaction { void Init() }
;; in the project.cci by simply specifying a message named "Init".  We will
;; then sythesize a transaction with the signature "void Init(Init) = 1;"
;;-----------------------------------------------------------------
(def inittxn [:transactions [:function [:rettype "void"] [:functionName "Init"] [:param "Init"] [:index "1"]]])

(defn synthinit [interfaces]
  (let [ast (interfaces "project")]
    (cond

      ;; do not synthesize anything if there are explicit transactions defined
      (ast/find :transactions ast)
      interfaces

      ;; We cannot continue if the user didnt supply a message "Init"  which will
      ;; serve as the implicit parameter to our synthesized init function
      (not (initmsg? ast))
      (throw (Exception. (str "project.cci: message Init{} not found")))

      :else
      (assoc interfaces "project" (-> ast (zip/append-child inittxn) zip/root zip/vector-zip)))))


;;-----------------------------------------------------------------
;; compile all applicable interfaces into a map of ASTs keyed by interface name
;;-----------------------------------------------------------------
(defn compile [path config]
  (let [names (getinterfaces config)
        interfaces (into {} (map #(vector % (compileintf path %)) names))]
    (synthinit interfaces)))
