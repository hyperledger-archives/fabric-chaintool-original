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
            [obcc.util :as util]
            [obcc.subcommands.build :as buildcmd]
            [obcc.subcommands.clean :as cleancmd]
            [obcc.subcommands.package :as packagecmd])
  (:gen-class))

(defn option-merge [& args] (into [] (apply concat args)))

;; options common to all modes, top-level as well as subcommands
(def common-options
  [["-h" "--help"]])

(def toplevel-options
  (option-merge [["-v" "--version" "Print the version and exit"]]
                common-options))

;; these options are common to subcommands that are expected to operate on a chaincode tree
(def common-path-options
  (option-merge [["-p" "--path PATH" "path to chaincode project" :default "./"]]
                common-options))

(def subcommand-descriptors
  [{:name "build" :desc "Build the chaincode project"
    :handler  buildcmd/run
    :options common-path-options}

   {:name "clean" :desc "Clean the chaincode project"
    :handler cleancmd/run
    :options common-path-options}

   {:name "package" :desc "Package the chaincode for deployment"
    :handler packagecmd/run
    :options (option-merge [["-o" "--output NAME" "path to the output destination"]]
                           common-path-options)}])

(def subcommands (->> subcommand-descriptors (map #(vector (:name %) %)) (into {})))

(defn exit [status msg & rest]
  (do
    (apply println msg rest)
    (System/exit status)))

(defn version [] (str "obcc version: v" util/app-version))

(defn prep-usage [msg] (->> msg flatten (string/join \newline)))

(defn usage [options-summary]
  (prep-usage [(version)
               ""
               "Usage: obcc [general-options] action [action-options]"
               ""
               "General Options:"
               options-summary
               ""
               "Actions:"
               (map (fn [[_ {:keys [name desc]}]] (str "  " name ": " desc)) subcommands)
               ""
               ]))

(defn subcommand-usage [subcommand options-summary]
  (prep-usage [(version)
               ""
               (str "Description: obcc " (:name subcommand) " - " (:desc subcommand))
               ""
               (str "Usage: obcc " (:name subcommand) " [options]")
               ""
               "Command Options:"
               options-summary
               ""
               ]))

(defn -main [& args]
  (let [ {:keys [options arguments errors summary]} (parse-opts args toplevel-options :in-order true) ]
    (cond

      (:help options)
      (exit 0 (usage summary))

      (not= errors nil)
      (exit -1 "Error: " (string/join errors))

      (:version options)
      (exit 0 (version))

      (= (count arguments) 0)
      (exit 0 (usage summary))

      :else
      (if-let [subcommand (subcommands (first arguments))]
        (let [{:keys [options arguments errors summary]} (parse-opts (rest arguments) (:options subcommand))]
          (cond

            (:help options)
            (exit 0 (subcommand-usage subcommand summary))

            (not= errors nil)
            (exit -1 "Error: " (string/join errors))

            :else
            (try
              ((:handler subcommand) options arguments summary)
              (System/exit 0)
              (catch Exception e (exit -1 (str e))))))
        (exit 1 (usage summary))))))
