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

(ns chaintool.codecs
  (:import (lzma.streams LzmaOutputStream$Builder)
           (org.apache.commons.compress.compressors.lzma LZMACompressorInputStream)
           (org.apache.commons.compress.compressors.bzip2 BZip2CompressorOutputStream
                                                          BZip2CompressorInputStream)
           (org.apache.commons.compress.compressors.gzip GzipCompressorOutputStream
                                                         GzipCompressorInputStream
                                                         GzipParameters)
           (org.apache.commons.compress.compressors.xz XZCompressorOutputStream
                                                       XZCompressorInputStream)
           (org.apache.commons.io.output ProxyOutputStream)
           (org.apache.commons.io.input ProxyInputStream)))

;;--------------------------------------------------------------------------------------
;; compression support
;;--------------------------------------------------------------------------------------
(def codec-descriptors
  [{:name "none"
    :output #(ProxyOutputStream. %)
    :input #(ProxyInputStream. %)}

   {:name "gzip"
    :output #(let [params (GzipParameters.)] (.setCompressionLevel params 9) (GzipCompressorOutputStream. % params))
    :input #(GzipCompressorInputStream. %)}

   {:name "lzma"
    :output #(-> (LzmaOutputStream$Builder. %) .build)
    :input #(-> (LZMACompressorInputStream. %))}

   {:name "bzip2"
    :output #(BZip2CompressorOutputStream. %)
    :input #(BZip2CompressorInputStream. %)}

   {:name "xz"
    :output #(XZCompressorOutputStream. % 6)
    :input #(XZCompressorInputStream. %)}])

(def codec-types (->> codec-descriptors (map #(vector (:name %) %)) (into {})))

(defn compressor [type os] ((->> type codec-types :output) os))
(defn decompressor [type is] ((->> type codec-types :input) is))
