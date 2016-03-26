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
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
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
  (let [result (insta/add-line-and-column-info-to-metadata intf (grammar intf))]
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
(defn get-raw-attrs [ast]
  (loop [loc (zip/right ast) attrs {}]
    (if (nil? loc)
      attrs
      ;; else
      (let [[k v] (zip/node loc)]
        (recur (zip/right loc) (assoc attrs k v))))))

(defn getattrs [ast]
  (let [{:keys [type] :as attrs} (get-raw-attrs ast)
        [subType typeName] type]
    ;; sythesize the subType/typeName fields
    (merge attrs {:subType subType :typeName typeName})))

(defn get-index [ast]
  (let [{:keys [index]} (getattrs ast)]
    index))

(defn getentries [ast]
  (loop [loc ast fields {}]
    (cond

      (nil? loc)
      fields

      :else
      (let [attrs (->> loc zip/down getattrs)]
        (recur (zip/right loc) (assoc fields (:index attrs) attrs))))))

(defn get-message-name [ast]
  (->> ast zip/right zip/node))

(defn getmessage [ast]
  (let [name (get-message-name ast)
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

(defn get-definition-name [ast]
  (let [node (zip/down ast)
        type (zip/node node)]
    (when (or (= type :message) (= type :enum))
      (->> node zip/right zip/node))))

(defn find-definition-in-local [name ast]
  (let [start (zip/leftmost ast)
        type (zip/node start)]
    (loop [loc (case type
                 ;; each :message entry looks like [:message $name fields...],
                 ;; so "start + right + right" gets the first field
                 :message (->> start zip/right zip/right)

                 ;; each :interface entry looks like [:interface [:message] [:message] ...],
                 ;; so "start+right" gets the first msg
                 :interface (zip/right start)

                 ;; otherwise, skip
                 nil)]
      (cond

        (nil? loc)
        nil

        (= name (get-definition-name loc))
        true

        :else
        (recur (zip/right loc))))))

;;-----------------------------------------------------------------
;; get-lineno: get the line number for a given AST entry
;;-----------------------------------------------------------------
(defn get-lineno [ast]
  ;; use the end-line since instaparse includes whitespace and the start-line is typically the previous line
  (->> ast meta :instaparse.gll/end-line))

;;-----------------------------------------------------------------
;; error threading helpers
;;-----------------------------------------------------------------
(defn bind-error [f [val err]]
  (if (nil? err)
    (f val)
    [nil err]))

(defmacro err->> [val & fns]
  (let [fns (for [f fns] `(bind-error ~f))]
    `(->> [~val nil]
          ~@fns)))

;;-----------------------------------------------------------------
;; verify-XX - verify our interface is rational
;;-----------------------------------------------------------------
;; A sanely defined interface should ensure several things
;;
;; 1) All field types are either scalars or defined within the interface
;;    following inner-to-outer scoping.
;;
;; 2) Indexes should never overlap
;;
;; 3) All functions reference either void, or reference a valid
;;    top-level message for both return and/or input parameters
;;-----------------------------------------------------------------

;;-----------------------------------------------------------------
;; verify-usertype: fields of type :userType need to reference an
;; in scope definition (:message or :enum).  Therefore, we need to
;; walk our scope backwards to find if this usertype has been defined
;;-----------------------------------------------------------------
(defn verify-usertype [ast]
  (let [{:keys [typeName fieldName type]} (getattrs ast)]
    (loop [loc (->> ast zip/up)]
      (cond

        (nil? loc)
        [nil (str "line " (get-lineno type) ": type \"" typeName "\" for field \"" fieldName "\" is not defined")]

        (find-definition-in-local typeName loc)
        [ast nil]

        :else
        (recur (zip/up loc))))))

;;-----------------------------------------------------------------
;; verify-fieldtype: validate a field according to the type it is
;;-----------------------------------------------------------------
(defn verify-fieldtype [ast]
  (let [{:keys [subType]} (getattrs ast)]
    (case subType
      :userType (verify-usertype ast)
      :scalar [ast nil])))

;;-----------------------------------------------------------------
;; verify-index: ensures we do not have any duplicate indices
;;-----------------------------------------------------------------
(defn verify-index [indices ast]
  (let [index (get-index ast)]
    (if (contains? indices index)
      (let [node (->> ast zip/up zip/node)]
        [nil (str "line " (get-lineno node) ": duplicate index " index ", previously seen on line " (indices index))])
      ;; else
      [ast nil])))

;;-----------------------------------------------------------------
;; verify-field: ensure a field is valid by running various
;; verifications such as checking scope resolution for any custom
;; types, and ensuring our indices do not conflict
;;-----------------------------------------------------------------
(defn verify-field [ast indices]
  (let [[_ error] (err->> ast
                      verify-fieldtype
                      #(verify-index indices %))
        lineno (->> ast zip/up zip/node get-lineno)]
    (if (nil? error)
      ;; add our index to the table
      [(assoc indices (get-index ast) lineno) nil]
      ;; else, stop on error
      [indices error])))

;;-----------------------------------------------------------------
;; verify-message: verify the fields of a message by scanning through
;; all fields in the AST, skipping non :field types
;;-----------------------------------------------------------------
(defn verify-message [ast]
  (let [name (get-message-name ast)]
    (loop [loc (->> ast zip/right zip/right) _indices {}]
      (cond

        (nil? loc)
        nil

        :else
        (let [node (zip/down loc)
              type (zip/node node)
              [indices error] (case type
                                   :field (verify-field node _indices)
                                   [_indices nil])]

          (if (nil? error)
            (recur (zip/right loc) indices)
            error))))))

;;-----------------------------------------------------------------
;; verify-enum: ensure enum entries do not have any conflicting indices
;;-----------------------------------------------------------------
(defn verify-enum [ast]
  ;;FIXME
  )

;;-----------------------------------------------------------------
;; verify-intf: scan the entire interface and validate various
;; types we encounter
;;-----------------------------------------------------------------
(defn verify-intf [intf]
  (loop [loc intf]
    (cond

      (or (nil? loc) (zip/end? loc))
      nil

      :else
      (let [node (zip/node loc)]
        (if-let [error (case node
                         :message (verify-message loc)
                         :enum (verify-enum loc)
                         nil)]
          error
          (recur (zip/next loc)))))))

;;-----------------------------------------------------------------
;; takes an interface name, maps it to a file, and if present, compiles
;; it to an AST.
;;-----------------------------------------------------------------
(defn compileintf [path intf]
  (let [file (open (io/file path "src/interfaces") intf)]
    (println (str "[CCI] parse " (.getName file)))
    (let [ast (->> file slurp parse)]

      (when-let [errors (verify-intf ast)]
          (util/abort -1 (str "Errors parsing " (.getName file) ": " (string/join errors))))

      ;; return the AST
      ast)))

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
