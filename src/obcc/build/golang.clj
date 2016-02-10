(ns obcc.build.golang
  (:refer-clojure :exclude [find compile])
  (:import [org.stringtemplate.v4 STGroupFile ST])
  (:import [java.util ArrayList])
  (:require [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.string :as string]
            [clojure.algo.generic.functor :as algo]
            [me.raynes.conch :as conch]
            [me.raynes.conch.low-level :as sh]
            [instaparse.core :as insta]
            [obcc.util :as util]
            [obcc.ast :as ast]
            [obcc.config.parser :as config]
            [obcc.build.interface :as intf]))

;; types to map to java objects that string template expects.
;;

(deftype Function  [^String rettype ^String name ^String param ^Integer index])
(deftype Interface  [^String name ^String ns ^ArrayList transactions ^ArrayList queries])

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects
;;-----------------------------------------------------------------

(defn buildfunction [{:keys [rettype functionName param index]}]
  (vector functionName (->Function (if (not= rettype "void") rettype nil) functionName param index)))

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

(defn go [path & args]
  (let [cwd (System/getProperty "user.dir")
        fqpath (str cwd "/" path)
        gopath (str fqpath "/build/deps" ":" fqpath "/build" ":" fqpath ":" (System/getenv "GOPATH"))
        _args (into [] (concat ["go"] args [:env {"GOPATH" gopath}]))]
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

    ;; generate protobuf output
    (protoc protofile)

    ;; test-compile our chaincode
    (go path "get" "-d" "-v" "chaincode")
    (go path "build" "chaincode")

    (println "Compilation complete")))
