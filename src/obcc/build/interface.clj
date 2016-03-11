;; Licensed to the Apache Software Foundation (ASF) under one
;; or more contributor license agreements.  See the NOTICE file
;; distributed with this work for additional information
;; regarding copyright ownership.  The ASF licenses this file
;; to you under the Apache License, Version 2.0 (the
;; "License"); you may not use this file except in compliance
;; with the License.  You may obtain a copy of the License at
;;
;;   http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing,
;; software distributed under the License is distributed on an
;; "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
;; KIND, either express or implied.  See the License for the
;; specific language governing permissions and limitations
;; under the License.

(ns obcc.build.interface
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.walk :as walk]
            [clojure.zip :as zip]
            [clojure.pprint :refer :all]
            [instaparse.core :as insta]
            [obcc.ast :as ast]
            [obcc.util :as util])
  (:refer-clojure :exclude [compile]))

(def skipper (insta/parser (io/resource "parsers/interface/skip.bnf")))
(def grammar (insta/parser (io/resource "parsers/interface/grammar.bnf") :auto-whitespace skipper))

(defn parse [intf]
  (let [result (grammar intf)]
    (if (insta/failure? result)
      (let [{:keys [line column text]} result]
        (util/abort -1 (str "could not parse \"" text "\": line=" line " column=" column)))
      (zip/vector-zip result))))

;;-----------------------------------------------------------------
;; retrieve all "provided" interfaces, adding the implicit
;; "project.cci" and translating "self" to the name of the project
;;-----------------------------------------------------------------
(defn getprovides [config]
  (let [name (:Name config)
        entries (:Provides config)]
    (->> entries flatten (remove nil?) (walk/postwalk-replace {"self" name}) (cons "project") (into #{}))))

(defn getconsumes [config]
  (->> config :Consumes (remove nil?) (into #{})))

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
      (util/abort -1 (str (.getCanonicalPath file) " not found")))))

;;-----------------------------------------------------------------
;; getX - helper functions to extract data from an interface AST
;;-----------------------------------------------------------------
(defn getattrs [ast]
  (loop [loc ast attrs {}]
    (if (nil? loc)
      attrs
      ;; else
      (let [[k v] (zip/node loc)]
        (recur (zip/right loc) (assoc attrs k v))))))

(defn getentries [ast]
  (loop [loc ast fields {}]
    (cond

      (nil? loc)
      fields

      :else
      (let [attrs (->> loc zip/down zip/right getattrs)]
        (recur (zip/right loc) (assoc fields (:index attrs) attrs))))))

(defn getmessage [ast]
  (let [name (->> ast zip/right zip/node)
        fields (getentries (->> ast zip/right zip/right))]
    (vector name fields)))

(defn getmessages [interface]
  (into {} (loop [loc interface msgs '()]
             (cond

               (or (nil? loc) (zip/end? loc))
               msgs

               :else
               (let [node (zip/node loc)]
                 (recur (zip/next loc)
                        (if (= node :message)
                          (cons (getmessage loc) msgs)
                          msgs)))))))

(defn getfunctions [ast]
  (let [name (->> ast zip/down zip/node)
        functions (getentries (->> ast zip/down zip/right))]
    (vector name functions)))

(defn getgeneric [ast term]
  (if-let [results (ast/find term ast)]
    (getfunctions results)))

(defn gettransactions [ast] (getgeneric ast :transactions))
(defn getqueries [ast] (getgeneric ast :queries))
(defn getallfunctions [ast] (into {} (vector (gettransactions ast) (getqueries ast))))

;;-----------------------------------------------------------------
;; takes an interface name, maps it to a file, and if present, compiles
;; it to an AST.
;;-----------------------------------------------------------------
(defn compileintf [path intf]
  (let [file (open (io/file path "src/interfaces") intf)]
    (println (str "[CCI] parse " (.getName file)))
    (->> file slurp parse)))

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
      (util/abort -1 (str "project.cci: message Init{} not found"))

      :else
      (assoc interfaces "project" (-> ast (zip/append-child inittxn) zip/root zip/vector-zip)))))

;;-----------------------------------------------------------------
;; compile all applicable interfaces into a map of ASTs keyed by interface name
;;-----------------------------------------------------------------
(defn compile [path config]
  (let [names (getinterfaces config)
        interfaces (into {} (map #(vector % (compileintf path %)) names))]
    (synthinit interfaces)))
