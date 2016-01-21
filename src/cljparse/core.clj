(ns cljparse.core
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [cljparse.config.parser :as config])
  (:gen-class))

(def configname "chaincode.conf")

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "path to project to build"
    :default "./"
    :validate [#(and (.isDirectory (io/file %)) (.isFile (io/file % configname)))]]
   ["-h" "--help"]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond (:help options)
          (exit 0 summary))

    (cond (not= errors nil)
          (exit -1 (str "Error: " errors)))

    (println "Starting cljparse with path:" (:path options) "and errors:" errors)
    (config/parser (io/file (:path options) configname))))




