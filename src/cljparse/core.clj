(ns cljparse.core
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [cljparse.config.parser :as config])
  (:gen-class))

(def configname "chaincode.conf")

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "path to project to build" :default "./"]
   ["-h" "--help"]])

(defn exit [status msg & rest]
  (do
    (println msg rest)
    status))

(defn -main [& args]
  (let [ {:keys [options arguments errors summary]} (parse-opts args cli-options) ]
         (cond (:help options)
               (exit 0 summary)

               (not= errors nil)
               (exit -1 "Error: " errors)

               :else (let [ path (:path options)
                           file (io/file path configname) ]
                      (do
                        (cond (not (.isFile file))
                            (exit -1 "Configuration not found at " path)
                            :else
                            (do
                              (println "Starting cljparse on:" (.getAbsolutePath file))
                              (config/parser file))))))))
