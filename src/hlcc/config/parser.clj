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

(ns hlcc.config.parser
  (:require [clojure.java.io :as io]
            [hlcc.util :as util]
            [clojure.zip :as zip]
            [clj-yaml.core :as yaml]))

(def supported-schema 1)

(defn from-string [data]
  (let [config (yaml/parse-string data)
        schema (:Schema config)]
    (if (not= schema supported-schema)
      (util/abort -1 (str "Unsuported configuration schema (read:" schema " expected:" supported-schema ")"))
      config)))

(defn from-file [file] (->> file slurp from-string))
