(ns obcc.build.interface
  (:import [org.stringtemplate.v4 STGroupFile ST])
  (:require [clojure.java.io :as io]
            [clojure.zip :as zip]
            [instaparse.core :as insta]
            [obcc.config.parser :as config]))

(def grammar (insta/parser (io/resource "parsers/interface/grammar.bnf")
                           :auto-whitespace (insta/parser (io/resource "parsers/interface/skip.bnf"))))

(defn parse [intf] (->> intf grammar zip/vector-zip))

(defn getinterfaces [config]
  (let [keys [[:configuration :provides] [:configuration :consumes]]]
        (->> (map #(config/find % config) keys) flatten (into #{}) (into '()))))

(defn open [path intf]
  (let [file (io/file path (str intf ".cci"))]
    (cond
      (.exists file)
      file

      :else
      (throw (Exception. (str (.getAbsolutePath file) " not found"))))))

(defn compileintf [path proto intf]
  (println "Compile " intf)
  (let [ipath (str path "/src/interfaces")
        ast (->> (open ipath intf) slurp parse)
        tree {:interface intf :ast ast}]
    (.write proto (str tree))
    tree))

(defn compile [path config]
  (let [interfaces (->> config getinterfaces (cons "project") (remove nil?))
        protopath (io/file path "build/proto/project.proto")]

    ;; ensure the path exists
    (io/make-parents protopath)

    ;; and then generate our output
    (with-open [proto (io/writer protopath :truncate true)]
      (doall (map #(compileintf path proto %) interfaces)))))
