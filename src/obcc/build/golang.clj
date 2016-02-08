(ns obcc.build.golang
  (:refer-clojure :exclude [find compile])
  (:import [org.stringtemplate.v4 STGroupFile ST])
  (:import [java.util ArrayList])
  (:require [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.string :as string]
            [me.raynes.conch :as sh]
            [instaparse.core :as insta]
            [obcc.util :as util]
            [obcc.ast :as ast]
            [obcc.config.parser :as config]
            [obcc.build.interface :as intf]))

;; types to map to java objects that string template expects.
;;

(deftype Function  [^String rettype ^String name ^String param ^Integer index])
(deftype Interface  [^String name ^String shortname ^ArrayList functions])

(defn transactions? [ast] (ast/find :transactions ast))
(defn queries? [ast] (ast/find :queries ast))

;;-----------------------------------------------------------------
;; getX - helper functions to extract attributes from an AST function
;;-----------------------------------------------------------------
(defn getattrs [ast]
  (loop [loc ast attrs {}]
    (if (nil? loc)
      attrs
      ;; else
      (let [[k v] (zip/node loc)]
        (recur (zip/right loc) (assoc attrs k v))))))

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects from the AST
;;-----------------------------------------------------------------

(defn buildfunction [ast]
  (let [attrs (->> ast zip/down zip/right getattrs)
        {:keys [rettype functionName param index]} attrs]
    (->Function rettype functionName param index)))

(defn buildfunctions [name view namespaces]
  (loop [loc view functions {}]
    (cond

      (nil? loc)
      functions

      :else
      (let [node (zip/node loc)
            function (buildfunction loc)]
        (recur (->> loc zip/right) (assoc functions (.index function) function))))))

(defn buildinterface [name view namespaces]
  (let [functions (buildfunctions name (->> view zip/down zip/right) namespaces)]
    (->Interface name (namespaces name) functions)))

(defn build [interfaces namespaces pred]
  (let [candidates (for [[name ast] interfaces :let [view (pred ast)] :when view] [name view])]
    (into {} (map (fn [[name view]] (vector name (buildinterface name view namespaces))) candidates))))

(defn buildtransactions [interfaces namespaces] (build interfaces namespaces transactions?))
(defn buildqueries [interfaces namespaces] (build interfaces namespaces queries?))

;;-----------------------------------------------------------------
;; generate shim output - compiles the interfaces into a
;; golang shim, suitable for writing to a file
;;-----------------------------------------------------------------
(defn generateshim [config interfaces namespaces]
  (let [providedinterfaces (map #(vector % (interfaces %)) (intf/getprovides config))
        transactions (buildtransactions providedinterfaces namespaces)
        queries (buildqueries providedinterfaces namespaces)
        stg  (STGroupFile. "generators/golang.stg")
        template (.getInstanceOf stg "golang")]

    (.add template "transactions" transactions)
    (.add template "queries" queries)
    (.render template)))

(defn protoc [proto]
  (let [protoc (sh/programs protoc)]
    (try
      (protoc "--go_out=./" (str proto))
      (catch Exception e (println "protoc error:" (:stderr (ex-data e)))))))

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

    (println "Compilation complete")))
