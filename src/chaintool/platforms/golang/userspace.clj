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

(ns chaintool.platforms.golang.userspace
  (:require [chaintool.platforms.golang.core :refer :all]
            [clojure.java.io :as io])
  (:refer-clojure :exclude [compile]))

;;-----------------------------------------------------------------
;; compile - generates all golang platform artifacts within the
;; default location in the build area for standard chaincode
;; applications.
;;-----------------------------------------------------------------
(defn compile [{:keys [path config output]}]
  (let [builddir (io/file path "build")]

    ;; run our code generator
    (generate {:base "hyperledger"
               :ipath (io/file path "src/interfaces")
               :opath (io/file builddir "src")
               :config config})

    ;; install go dependencies
    (go-cmd path {} "get" "-d" "-v" "chaincode")

    ;; build the actual code
    (let [gobin (io/file builddir "bin")]
      (io/make-parents (io/file gobin ".dummy"))
      (io/make-parents output)
      (go-cmd path {"GOBIN" (.getCanonicalPath gobin)} "build" "-o" (.getCanonicalPath output) "chaincode"))

    (println "Compilation complete")))
