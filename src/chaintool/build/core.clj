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
(ns chaintool.build.core
  (:require [chaintool.build.golang :as golang]
            [chaintool.util :as util])
  (:refer-clojure :exclude [compile]))

(def handlers
  {"org.hyperledger.chaincode.golang" golang/compile})

(defn compile [{:keys [config] :as params}]
  ;; generate platform output (shim, protobufs, etc)
  (let [platform (->> config :Platform :Name)]
    (if-let [handler (handlers platform)]
      (handler params)
      (util/abort -1 (str "Unknown platform type: \"" platform "\"")))))