(ns cljparse.core
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [cljparse.config.parser :as config]
            [cljparse.subcommands.deps :as depscmd]
            [cljparse.subcommands.build :as buildcmd]
            [cljparse.subcommands.package :as packagecmd])
  (:gen-class))

(def configname "chaincode.conf")

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "path to chaincode project" :default "./"]
   ["-h" "--help"]])

(defn exit [status msg & rest]
  (do
    (apply println msg rest)
    status))

(def subcommands
  {"deps",    ["Resolve dependencies",                 depscmd/run],
   "build",   ["Build the chaincode project",          buildcmd/run],
   "package", ["Package the chaincode for deployment", packagecmd/run]})
  
(defn usage [options-summary]
  (->> (flatten ["Usage: obcc [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        (map (fn [[name [desc func]]] (str "  " name " -> " desc)) subcommands)
        ""
        "Please refer to the manual page for more information."])
       (string/join \newline)))

(defn -main [& args]
  (let [ {:keys [options arguments errors summary]} (parse-opts args cli-options) ]
    (cond (or (:help options) (= (count arguments) 0))
          (exit 0 (usage summary))

          (not= errors nil)
          (exit -1 "Error: " (string/join errors))

          (not= (count arguments) 1)
          (exit -1 "Error: bad argument count")

          :else
          (let [ path (:path options)
                 file (io/file path configname) ]
            (do
              (cond (not (.isFile file))
                    (exit -1 "Configuration not found at " path)
                    :else
                    (do
                      (if-let [[_ func] (subcommands (first arguments))]
                        (func options)
                        (exit 1 (usage summary))))))))))
