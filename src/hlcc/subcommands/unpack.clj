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

(ns hlcc.subcommands.unpack
  (:require [hlcc.config.util :as config]
            [hlcc.util :as util]
            [hlcc.cca.read :as cca.read]
            [hlcc.cca.unpack :as cca.unpack]
[clojure.java.io :as io]))

(defn getoutputdir [options config]
  (if-let [dir (:directory options)]
    (io/file dir)
    (io/file "./" (str (config/compositename config)))))

(defn run [options args]
  (let [file (io/file (first args))
        {:keys [index config]} (with-open [is (io/input-stream file)] (cca.read/read is))
        outputdir (getoutputdir options config)]

    (when (.exists outputdir)
      (util/abort -1 (str "output directory " (.getCanonicalPath outputdir) " exists")))

    (println "Unpacking CCA to:" (.getCanonicalPath outputdir))
    (println)
    (cca.unpack/unpack index outputdir :true)))