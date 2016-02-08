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
            [obcc.config.parser :as config]
            [obcc.build.interface :as intf]))

;; types to map to java objects that string template expects.
;;

(deftype Function  [^String rettype ^String name ^String param ^Integer index])
(deftype Interface  [^String name ^String shortname ^ArrayList functions])


;;-----------------------------------------------------------------
;; manage object names
;;-----------------------------------------------------------------
(defn qualifyname [base name]
  (str (string/replace base "." "_") "_" name))

;;-----------------------------------------------------------------
;; find a specific element in the AST
;;-----------------------------------------------------------------
(defn find [term ast]
  (loop [loc ast]
    (cond

      (or (nil? loc) (zip/end? loc))
      nil

      (= (zip/node loc) term)
      (zip/up loc)

      :else
      (recur (zip/next loc)))))

(defn transactions? [ast] (find :transactions ast))
(defn queries? [ast] (find :queries ast))

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

(defn buildfunctions [name view aliases]
  (loop [loc view functions {}]
    (cond

      (nil? loc)
      functions

      :else
      (let [node (zip/node loc)
            function (buildfunction loc)]
        (recur (->> loc zip/right) (assoc functions (.index function) function))))))

(defn buildinterface [name view aliases]
  (let [functions (buildfunctions name (->> view zip/down zip/right) aliases)]
    (->Interface name (aliases name) functions)))

(defn build [interfaces aliases pred]
  (let [candidates (for [[name ast] interfaces :let [view (pred ast)] :when view] [name view])]
    (into {} (map (fn [[name view]] (vector name (buildinterface name view aliases))) candidates))))

(defn buildtransactions [interfaces aliases] (build interfaces aliases transactions?))
(defn buildqueries [interfaces aliases] (build interfaces aliases queries?))

;;-----------------------------------------------------------------
;; generate shim output - compiles the interfaces into a
;; golang shim, suitable for writing to a file
;;-----------------------------------------------------------------
(defn generateshim [config interfaces aliases]
  (let [providedinterfaces (map #(vector % (interfaces %)) (intf/getprovides config))
        transactions (buildtransactions providedinterfaces aliases)
        queries (buildqueries providedinterfaces aliases)
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
(defn compile [path config interfaces aliases protofile]
  (let [shim (generateshim config interfaces aliases)
        shimpath (io/file path util/supportpath "shim.go")]

    ;; ensure the path exists
    (io/make-parents shimpath)

    ;; emit our output
    (with-open [output (io/writer shimpath :truncate true)]
      (.write output shim))

    ;; generate protobuf output
    (protoc protofile)

    (println "Compilation complete")))
