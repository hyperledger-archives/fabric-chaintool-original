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

(ns hlcc.build.golang
  (:require [clojure.algo.generic.functor :as algo]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [me.raynes.conch :as conch]
            [me.raynes.conch.low-level :as sh]
            [hlcc.build.interface :as intf]
            [hlcc.protobuf.generate :as pb]
            [hlcc.util :as util])
  (:import (java.util ArrayList)
           (org.stringtemplate.v4 STGroupFile))
  (:refer-clojure :exclude [compile find]))

(conch/programs protoc)
(conch/programs gofmt)

;; types to map to java objects that string template expects.
;;

(deftype Function  [^String rettype ^String name ^String param ^Integer index])
(deftype Interface  [^String name ^String package ^String packageCamel ^String packagepath ^ArrayList transactions ^ArrayList queries])

;;------------------------------------------------------------------
;; helper functions
;;------------------------------------------------------------------
(defn package-name [name] (-> name (string/split #"\.") last))
(defn package-camel [name] (-> name package-name string/capitalize))
(defn package-path [name] (str "hyperledger/cci/" (string/replace name "." "/")))

(defn conjpath [components]
  (.getCanonicalPath (apply io/file components)))

;;------------------------------------------------------------------
;; return a string with composite GOPATH elements, separated by ":"
;;
;; (note that the first entry is where the system will write dependencies
;; retrived by "go get")
;;------------------------------------------------------------------
(defn buildgopath [path]
  (let [gopath (map conjpath [[path "build/deps"][path "build"][path][(System/getenv "GOPATH")]])]

    (clojure.string/join ":" gopath)))

;;------------------------------------------------------------------
;; X-cmd interfaces: Invoke external commands
;;------------------------------------------------------------------
(defn protoc-cmd [_path _proto]
  (let [path (->> _path io/file .getCanonicalPath)
        go_out (str "--go_out=" path)
        proto_path (str "--proto_path=" path)
        proto (.getCanonicalPath _proto)]
    (println "[PB] protoc" go_out proto_path proto)
    (try
      (let [result (protoc go_out proto_path proto {:verbose true})]
        (println (:stderr result)))
      (catch clojure.lang.ExceptionInfo e
        (util/abort -1 (-> e ex-data :stderr))))))

(defn go-cmd [path env & args]
  (println "[GO] go" (apply print-str args))
  (let [gopath (buildgopath path)
        _args (vec (concat ["go"] args [:env (merge {"GOPATH" gopath} env)]))]

    (println "\tUsing GOPATH" gopath)
    (let [result (apply sh/proc _args)
          _ (sh/done result)
          stderr (sh/stream-to-string result :err)]

      (if (zero? (sh/exit-code result))
        (println stderr)
        (util/abort -1 stderr)))))

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects
;;-----------------------------------------------------------------

(defn buildfunction [{:keys [rettype functionName param index]}]
  (vector functionName (->Function (when (not= rettype "void") rettype) functionName param index)))

(defn buildfunctions [functions]
  (into {} (for [[k v] functions]
             (buildfunction v))))

(defn buildinterface [name interface]
  (let [transactions (buildfunctions (:transactions interface))
        queries (buildfunctions (:queries interface))]
    (vector name (->Interface name (package-name name) (package-camel name) (package-path name) transactions queries))))

(defn build [interfaces]
  (into {} (map (fn [[name interface]] (buildinterface name interface)) interfaces)))

;;-----------------------------------------------------------------
;; generic template rendering
;;-----------------------------------------------------------------
(defn render-golang [templatename params]
  (let [stg  (STGroupFile. "generators/golang.stg")
        template (.getInstanceOf stg templatename)]

    (dorun (for [[param value] params] (.add template param value)))
    (.render template)))

;;-----------------------------------------------------------------
;; render shim output - compiles the interfaces into the primary
;; golang shim, suitable for writing to a file
;;-----------------------------------------------------------------
(defn render-primary-shim [config interfaces]
  (let [functions (algo/fmap intf/getallfunctions interfaces)
        provides (build (select-keys functions (intf/getprovides config)))]

    (render-golang "primary" [["provides" provides]])))

;;-----------------------------------------------------------------
;; write golang source to the filesystem, using gofmt to clean
;; up the generated code
;;-----------------------------------------------------------------
(defn emit-golang [outputfile content]
  (util/truncate-file outputfile content)
  (gofmt "-w" (.getCanonicalPath outputfile)))

;;-----------------------------------------------------------------
;; emit-shim
;;-----------------------------------------------------------------
(defn emit-shim [name functions template srcdir filename]
  (let [[_ interface] (buildinterface name functions)
        content (render-golang template [["intf" interface]])
        output (io/file srcdir (package-path name) filename)]

    (emit-golang output content)))

(defn emit-server-shim [name functions srcdir]
  (emit-shim name functions "server" srcdir "server-shim.go"))

;;-----------------------------------------------------------------
;; emit-proto
;;-----------------------------------------------------------------
(defn emit-proto [srcdir [name ast :as interface]]
  (let [outputdir (io/file srcdir (package-path name))
        output (io/file outputdir "interface.proto")]

    ;; emit the .proto file
    (pb/to-file output (package-name name) interface)

    ;; execute the protoc compiler to generate golang
    (protoc-cmd outputdir output)))

;;-----------------------------------------------------------------
;; compile - generates all golang platform artifacts within the
;; default location in the build area
;;-----------------------------------------------------------------
(defn compile [path config interfaces output]
  (dorun
   (let [builddir (io/file path "build")
         srcdir (io/file builddir "src")]

     ;; generate protobuf output
     (dorun (for [interface interfaces]
              (emit-proto srcdir interface)))

     ;; generate our primary shim
     (let [content (render-primary-shim config interfaces)
           filename (io/file srcdir "hyperledger/ccs" "shim.go")]
       (emit-golang filename content))

     ;; generate our server shims
     (let [provides (->> config intf/getprovides (filter #(not= % "appinit")))]

       ;; first process all _except_ the appinit interface
       (dorun (for [name provides]
                (let [functions (intf/getallfunctions (interfaces name))]
                  (emit-server-shim name functions srcdir))))

       ;; and now special case the appinit  interface
       (emit-server-shim "appinit" {:transactions {1 {:rettype "void", :functionName "Init", :param "Init", :index 1, :subType nil, :typeName nil}}} srcdir))

     ;; generate our client shims
     (dorun (for [name (intf/getconsumes config)]
              (emit-shim name interfaces "client" srcdir "client-shim.go")))

     ;; install go dependencies
     (go-cmd path {} "get" "-d" "-v" "chaincode")

     ;; build the actual code
     (let [gobin (io/file builddir "bin")]
       (io/make-parents (io/file gobin ".dummy"))
       (io/make-parents output)
       (go-cmd path {"GOBIN" (.getCanonicalPath gobin)} "build" "-o" (.getCanonicalPath output) "chaincode"))

     (println "Compilation complete"))))
