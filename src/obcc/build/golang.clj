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

(ns obcc.build.golang
  (:require [clojure.algo.generic.functor :as algo]
            [clojure.java.io :as io]
            [me.raynes.conch :as conch]
            [me.raynes.conch.low-level :as sh]
            [obcc.build.interface :as intf]
            [obcc.util :as util])
  (:import (java.util ArrayList)
           (org.stringtemplate.v4 STGroupFile))
  (:refer-clojure :exclude [compile find]))

;; types to map to java objects that string template expects.
;;

(deftype Function  [^String rettype ^String name ^String param ^Integer index])
(deftype Interface  [^String name ^String ns ^ArrayList transactions ^ArrayList queries])

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects
;;-----------------------------------------------------------------

(defn buildfunction [{:keys [rettype functionName param index]}]
  (vector functionName (->Function (when (not= rettype "void") rettype) functionName param index)))

(defn buildfunctions [functions]
  (into {} (for [[k v] functions]
             (buildfunction v))))

(defn buildinterface [name interface namespaces]
  (let [transactions (buildfunctions (:transactions interface))
        queries (buildfunctions (:queries interface))]
    (vector name (->Interface name (namespaces name) transactions queries))))

(defn build [interfaces namespaces]
  (into {} (map (fn [[name interface]] (buildinterface name interface namespaces)) interfaces)))

;;-----------------------------------------------------------------
;; generate shim output - compiles the interfaces into a
;; golang shim, suitable for writing to a file
;;-----------------------------------------------------------------
(defn generateshim [config interfaces namespaces]
  (let [functions (algo/fmap intf/getallfunctions interfaces)
        provides (build (select-keys functions (intf/getprovides config)) namespaces)
        consumes (build (select-keys functions (intf/getconsumes config)) namespaces)
        stg  (STGroupFile. "generators/golang.stg")
        template (.getInstanceOf stg "golang")]

    (.add template "provides" provides)
    (.add template "consumes" consumes)
    (.render template)))

(defn protoc [proto]
  (let [protoc (conch/programs protoc)]
    (println "protoc")
    (println (:stderr (protoc "--go_out=./" (str proto) {:verbose true})))))

(def cwd (System/getProperty "user.dir"))
(defn fqpath [path] (->> (io/file cwd path) .getAbsolutePath))

(defn go [path env & args]
  (let [fqpath (fqpath path)
        gopath (str fqpath "/build/deps" ":" fqpath "/build" ":" fqpath ":" (System/getenv "GOPATH"))
        _args (vec (concat ["go"] args [:env (merge {"GOPATH" gopath} env)]))]
    (apply println _args)
    (let [result (apply sh/proc _args)]
      (sh/done result)
      (println (sh/stream-to-string result :err)))))

;;-----------------------------------------------------------------
;; compile - generates golang shim code and writes it to
;; the default location in the build area
;;-----------------------------------------------------------------
(defn compile [path config interfaces namespaces protofile]
  (let [shim (generateshim config interfaces namespaces)
        shimpath (io/file path util/supportpath "shim.go")]

    ;; ensure the path exists
    (io/make-parents shimpath)

    ;; emit our output
    (with-open [output (io/writer shimpath :truncate true)]
      (.write output shim))

    ;; clean up the generated code with gofmt
    (let [gofmt (conch/programs gofmt)]
      (gofmt "-w" (.getAbsolutePath shimpath)))

    ;; generate protobuf output
    (protoc protofile)

    ;; install dependencies
    (go path {} "get" "-d" "-v" "chaincode")

    ;; build the actual code
    (let [gobin (io/file (fqpath path) "build/bin")]
      (io/make-parents (io/file gobin ".dummy"))
      (go path {"GOBIN" (.getAbsolutePath gobin)} "build" "chaincode"))

    (println "Compilation complete")))
