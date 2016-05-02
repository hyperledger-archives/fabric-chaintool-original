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

(ns chaintool.platforms.golang.system
  (:require [chaintool.platforms.golang.core :refer :all]
            [me.raynes.conch :as conch]
            [clojure.tools.file-utils :as fileutils]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:refer-clojure :exclude [compile]))

(conch/programs go)

(defn get-package-name [path]
  ;; FIXME: This will only work when chaintool is in CWD, need to "cd $path" first
  (string/trim-newline (go "list")))

(defn subtract-paths [fqpath relpath]
  (->> (string/replace (str fqpath) relpath "") io/file .getCanonicalPath str))

(defn get-fqp [path]
  (->> path io/file .getCanonicalPath))

(defn compute-gopath [path pkgname]
  (->> pkgname pkg-to-relpath (subtract-paths (get-fqp path))))

;;-----------------------------------------------------------------
;; compile - generates all golang platform artifacts within the
;; default location in the build area for a system-chaincode
;; application
;;-----------------------------------------------------------------
(defn compile [{:keys [path config output]}]
  (let [builddir "build"
        opath (io/file path builddir)
        pkgname (get-package-name path)
        gopath (compute-gopath path pkgname)]

    ;; ensure we clean up any previous runs
    (fileutils/recursive-delete opath)

    ;; run our code generator
    (generate {:base (str pkgname "/" builddir)
               :package pkgname
               :ipath (io/file path "interfaces")
               :opath (io/file gopath)
               :config config})

    (println "Compilation complete")))