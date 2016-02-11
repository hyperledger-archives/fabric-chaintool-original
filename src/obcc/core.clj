;; Licensed to the Apache Software Foundation (ASF) under one
;; or more contributor license agreements.  See the NOTICE file
;; distributed with this work for additional information
;; regarding copyright ownership.  The ASF licenses this file
;; to you under the Apache License, Version 2.0 (the
;; "License"); you may not use this file except in compliance
;; with the License.  You may obtain a copy of the License at
;;
;;   http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing,
;; software distributed under the License is distributed on an
;; "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
;; KIND, either express or implied.  See the License for the
;; specific language governing permissions and limitations
;; under the License.
(ns obcc.core
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [obcc.config.parser :as config]
            [obcc.util :as util]
            [obcc.subcommands.build :as buildcmd]
            [obcc.subcommands.clean :as cleancmd]
            [obcc.subcommands.package :as packagecmd])
  (:gen-class))

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "path to chaincode project" :default "./"]
   ["-v" "--version"]
   ["-h" "--help"]])

(defn exit [status msg & rest]
  (do
    (apply println msg rest)
    (System/exit status)))

(def subcommands
  {"build",   ["Build the chaincode project",          buildcmd/run],
   "clean",   ["Clean the chaincode project",          cleancmd/run]
   "package", ["Package the chaincode for deployment", packagecmd/run]})

(defn version [] (str "obcc version: v" util/app-version))

(defn usage [options-summary]
  (->> (flatten [(version)
                 ""
                 "Usage: obcc [options] action"
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
    (cond

      (:help options)
      (exit 0 (usage summary))

      (not= errors nil)
      (exit -1 "Error: " (string/join errors))

      (:version options)
      (exit 0 (version))

      (= (count arguments) 0)
      (exit 0 (usage summary))

      (not= (count arguments) 1)
      (exit -1 "Error: bad argument count")

      :else
      (let [path (:path options)
            file (io/file path util/configname)]
        (do
          (cond (not (.isFile file))
                (exit -1 "Configuration not found at " path)
                :else
                (let [config (config/parser file)]
                  (if-let [[_ func] (subcommands (first arguments))]
                    (do
                      (try
                        (func path config)
                        (catch Exception e (println "error:" (:stderr (ex-data e)))))
                      (System/exit 0))
                    (exit 1 (usage summary))))))))))
