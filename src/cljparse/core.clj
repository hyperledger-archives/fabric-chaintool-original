(ns cljparse.core
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "path to project to build"
    :default "./"
    :validate [#(.isDirectory (io/file %))]]
   ["-h" "--help"]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond (or (:help options) (not= errors nil))
          (exit 0 summary))

    (println "Starting cljparse with path:" (:path options) "and errors:" errors)))




