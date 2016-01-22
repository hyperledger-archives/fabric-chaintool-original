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
    :default "./"]
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

    (let [file (io/file (:path options) configname)]
      (cond (not (.isFile file))
            (exit -1 (str "Configuration not found at " (:path options))))

      (println "Starting cljparse with path:" (:path options) "and errors:" errors)
      (config/parser file))))







