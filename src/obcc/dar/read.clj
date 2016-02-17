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

(ns obcc.dar.read
  (:require [flatland.protobuf.core :as fl]
            [obcc.dar.types :refer :all]
            [obcc.dar.codecs :as codecs]
            [pandect.algo.sha1 :refer :all])
  (:refer-clojure :exclude [read]))

(defn read-protobuf [t is] (->> is (fl/protobuf-seq t) first))

(defn read-header [is] (read-protobuf Header is))
(defn read-archive [is] (read-protobuf Archive is))

(defn make-input-stream [type entry]
  (let [is (->> entry :data .newInput)]
    (codecs/decompressor type is)))

(defn import-header [is]
  (if-let [header (read-header is)]
    (let [compat (select-keys header [:magic :version])]
      (if (= compat CompatVersion)
        (:features header)
        (throw (Exception. (str "Incompatible header detected (expected: " CompatVersion " got: " compat ")")))))
    (throw (Exception. (str "Failed to read archive header")))))

(defn import-archive [is]
  (read-archive is)) ;; FIXME - check digitial signature

(defn import-entry [compression entry]
  (let [type (:description compression)
        factory #(make-input-stream type entry)]

    ;; verify the SHA1
    (with-open [is (factory)]
      (let [sha (sha1 is)]
        (when (not= sha (:sha1 entry))
          (throw (Exception. (str (:path entry) ": hash verification failure (expected: " (:sha1 entry) ", got: " sha ")"))))))

    ;; and inject our stream factory
    {:entry entry :input-stream-factory factory}))

(defn import-payload [archive] (->> archive :payload .newInput (fl/protobuf-load-stream Payload)))

(defn synth-index [payload]
  (let [compression (:compression payload)]
    (->> (:entries payload) (map #(vector (:path %) (import-entry compression %))) (into {}))))

(defn read [is]
  (let [features (import-header is)
        archive (import-archive is)
        payload (import-payload archive)
        index (synth-index payload)]
    {:features features :payload payload :index index}))

(defn entry-stream [entry]
  (let [factory (:input-stream-factory entry)]
      (factory)))
