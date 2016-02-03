(ns obcc.build.interface
  (:require [clojure.java.io :as io]
            [clojure.zip :as zip]
            [instaparse.core :as insta]
            [obcc.config.parser :as config]))


(def grammar (insta/parser (io/resource "interface/grammar.bnf")
                           :auto-whitespace (insta/parser (io/resource "interface/skip.bnf"))))

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

(defn compileintf [path intf]
  (println "Compile " intf)
  (let [ipath (str path "/src/interfaces")
        opath (str path "/build/interfaces")
        tree (->> (open ipath intf) slurp parse)]
    {:interface intf :ast tree}))

(defn compile [path config]
  (let [interfaces (-> config getinterfaces (conj "project"))]
    (println interfaces)
    (map #(compileintf path %) (remove nil? interfaces))))
