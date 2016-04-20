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

(ns chaintool.core
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [slingshot.slingshot :as slingshot]
            [chaintool.subcommands.build :as buildcmd]
            [chaintool.subcommands.buildcca :as buildccacmd]
            [chaintool.subcommands.clean :as cleancmd]
            [chaintool.subcommands.lscca :as lsccacmd]
            [chaintool.subcommands.package :as packagecmd]
            [chaintool.subcommands.unpack :as unpackcmd]
[chaintool.util :as util])
  (:gen-class))

(defn option-merge [& args] (vec (apply concat args)))

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
    :options (option-merge [["-o" "--output NAME" "path to the output destination"]]
                           common-path-options)}

   {:name "buildcca" :desc "Build the chaincode project from a CCA file"
    :handler  buildccacmd/run
    :arguments "path/to/file.cca"
    :validate (fn [options arguments] (= (count arguments) 1))
    :options (option-merge [["-o" "--output NAME" "path to the output destination"]]
                           common-options)}

   {:name "clean" :desc "Clean the chaincode project"
    :handler cleancmd/run
    :options common-path-options}

   {:name "package" :desc "Package the chaincode into a CCA file for deployment"
    :handler packagecmd/run
    :options (option-merge [["-o" "--output NAME" "path to the output destination"]
                            ["-c" "--compress NAME" "compression algorithm to use" :default "gzip"]]
                           common-path-options)}

   {:name "unpack" :desc "Unpackage a CCA file"
    :handler unpackcmd/run
    :arguments "path/to/file.cca"
    :validate (fn [options arguments] (= (count arguments) 1))
    :options (option-merge [["-d" "--directory NAME" "path to the output destination"]]
                           common-options)}

   {:name "lscca" :desc "List the contents of a CCA file"
    :handler lsccacmd/run
    :arguments "path/to/file.cca"
    :validate (fn [options arguments] (= (count arguments) 1))
    :options common-options}])

;; N.B. the resulting map values are vectors each with a single map as an element
;;
(def subcommands (group-by :name subcommand-descriptors))

(defn exit [status msg & rest]
  (do
    (apply println msg rest)
    status))

(defn version [] (str "chaintool version: v" util/app-version))

(defn prep-usage [msg] (->> msg flatten (string/join \newline)))

(defn usage [options-summary]
  (prep-usage [(version)
               ""
               "Usage: chaintool [general-options] action [action-options]"
               ""
               "General Options:"
               options-summary
               ""
               "Actions:"
               (map (fn[[ _ [{:keys [name desc]}] ]] (str "  " name " -> " desc)) subcommands)
               ""
               "(run \"chaintool <action> -h\" for action specific help)"]))

(defn subcommand-usage [subcommand options-summary]
  (prep-usage [(version)
               ""
               (str "Description: chaintool " (:name subcommand) " - " (:desc subcommand))
               ""
               (str "Usage: chaintool " (:name subcommand) " [options] " (when-let [arguments (:arguments subcommand)] arguments))
               ""
               "Command Options:"
               options-summary
               ""]))

(defn -app [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args toplevel-options :in-order true)]
    (cond

      (:help options)
      (exit 0 (usage summary))

      (not= errors nil)
      (exit -1 "Error: " (string/join errors))

      (:version options)
      (exit 0 (version))

      (zero? (count arguments))
      (exit -1 (usage summary))

      :else
      (if-let [ [subcommand] (subcommands (first arguments))]
        (let [{:keys [options arguments errors summary]} (parse-opts (rest arguments) (:options subcommand))]
          (cond

            (:help options)
            (exit 0 (subcommand-usage subcommand summary))

            (not= errors nil)
            (exit -1 "Error: " (string/join errors))

            (and (:validate subcommand) (not ((:validate subcommand) options arguments)))
            (exit -1 (subcommand-usage subcommand summary))

            :else
            (slingshot/try+
              ((:handler subcommand) options arguments)
              (exit 0 "")
              (catch [:type :chaintoolabort] {:keys [msg retval]}
                (exit retval (str "Error: " msg))))))

        ;; unrecognized subcommand
        (exit 1 (usage summary))))))

(defn -main [& args]
  (System/exit (apply -app args)))
