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
(ns obcc.subcommands.buildcca
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.tools.file-utils :as fileutils]
            [obcc.config.util :as config.util]
            [obcc.cca.read :as cca.read]
            [obcc.cca.unpack :as cca.unpack]
            [obcc.build.core :as build.core]))

(defn getoutput [options]
  (if-let [output (:output options)]
    (io/file output)
    (throw (Exception. "Missing -o output (see -h for details)"))))

(defn run [options args]
  (let [output (getoutput options)
        file (io/file (first args))
        {:keys [index config]} (with-open [is (io/input-stream file)] (cca.read/read is))
        workingdir (fs/temp-dir "buildcca-")]

    (cca.unpack/unpack index workingdir :false)
    (let [config (config.util/load workingdir)]
      (println "Building CCA" (.getCanonicalPath file))
      (build.core/compile workingdir config output)
      (fileutils/recursive-delete (io/file workingdir)))))
