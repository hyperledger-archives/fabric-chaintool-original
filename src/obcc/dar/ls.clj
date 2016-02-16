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

(ns obcc.dar.ls
  (:require [clojure.java.io :as io]
            [doric.core :as doric]
            [flatland.protobuf.core :as fl]
            [obcc.dar.types :refer :all]
            [obcc.dar.codecs :as codecs]
            [obcc.config.parser :as config.parser]
            [obcc.config.util :as config.util]
            [pandect.algo.sha256 :refer :all]
            [pandect.algo.sha1 :refer :all]))

(defn read-protobuf [t is] (->> is (fl/protobuf-seq t) first))

(defn read-header [is] (read-protobuf Header is))
(defn read-archive [is] (read-protobuf Archive is))

(defn verify-header [is]
  (if-let [header (read-header is)]
    (let [compat (select-keys header [:magic :version])]
      (if-not (= compat CompatVersion)
        (throw (Exception. (str "Incompatible header detected (expected: " CompatVersion " got: " compat ")")))))
    (throw (Exception. (str "Failed to read archive header")))))

(defn open-entry [type entry]
  (let [is (->> entry :data .newInput)]
    (codecs/decompressor type is)))

(defn read-entry [type entry]
  (let [is (open-entry type entry)
        data (slurp is)
        sha (sha1 data)]
    (if (not= sha (:sha1 entry))
        (throw (Exception. (str (:path entry) ": hash verification failure")))
        data)))

(defn platform-version [config]
  (str (config.util/findfirst config [:platform :name]) " version " (config.util/findfirst config [:platform :version])))

(defn ls [file]
  (with-open [is (io/input-stream file)]
    (verify-header is)
    (let [archive (read-archive is)
          payload (->> archive :payload .newInput (fl/protobuf-load-stream Payload))
          compression (:compression payload)
          entries (->> (:entries payload) (map #(vector (:path %) %)) (into {}))
          config  (->> (entries config.util/configname) (read-entry (:description compression)) (config.parser/from-string))]

      (println (doric/table [{:name :size} {:name :sha1 :title "SHA1"} {:name :path}] (:entries payload)))
      (println "Platform:          " (platform-version config))
      (println "Digital Signature:  none")
      (println "Raw Data Size:     " (->> payload :entries (map :size) (reduce +)) "bytes")
      (println "Archive Size:      " (.length file) "bytes")
      (println "Compression Alg:   " (:description compression))
      (println "Chaincode SHA-256: " (sha256 file)))))
