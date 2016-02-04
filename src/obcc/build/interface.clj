(ns obcc.build.interface
  (:import [org.stringtemplate.v4 STGroupFile ST])
  (:import [java.util ArrayList])
  (:require [clojure.java.io :as io]
            [clojure.zip :as zip]
            [instaparse.core :as insta]
            [obcc.config.parser :as config]))

;; types to map to java objects that string template expects.
;;

;;(deftype Field    [^String modifier ^String type ^String name ^Int index])
;;(deftype Message  [^String name ^ArrayList fields])

(def grammar (insta/parser (io/resource "parsers/interface/grammar.bnf")
                           :auto-whitespace (insta/parser (io/resource "parsers/interface/skip.bnf"))))

(defn parse [intf] (->> intf grammar zip/vector-zip))

;; aggregate all of the interfaces declared in the config, adding the implicit
;; "project.cci"
(defn getinterfaces [config]
  (let [keys [[:configuration :provides] [:configuration :consumes]]
        explicit (map #(config/find % config) keys)]
    (->> explicit flatten (into #{}) (cons "project") (remove nil?) (into '()))))

(defn open [path intf]
  (let [file (io/file path (str intf ".cci"))]
    (cond
      (.exists file)
      file

      :else
      (throw (Exception. (str (.getAbsolutePath file) " not found"))))))

;; takes an interface name, maps it to a file, and if present, compiles
;; it to an AST.
(defn compileintf [path intf]
  (println "Compile " intf)
  (let [ipath (str path "/src/interfaces")
        ast (->> (open ipath intf) slurp parse)]
    [intf ast]))

;; compile all applicable interfaces into a map of ASTs keyed by interface name
(defn compileall [path config]
  (let [interfaces (getinterfaces config)]
    (into {} (map #(compileintf path %) interfaces))))

;;(defn generateproto [intf ast template]
;;  (loop [loc ast]
;;    (cond
;;
;;      (zip/end? loc)
;;      nil
;;
;;      :else
;;      (do
;;        ;; do something with each node here
;;        (recur (zip/next loc))))))

(defn compile [path config]

  (let [asts (compileall path config)
        protopath (io/file path "build/proto/project.proto")]

    ;; ensure the path exists
    (io/make-parents protopath)

    ;; and then generate our output
    (let [stg  (STGroupFile. "generators/proto.stg")
          template (.getInstanceOf stg "protobuf")]
      (with-open [output (io/writer protopath :truncate true)]
        (.write output (.render template))))))
