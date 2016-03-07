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

(ns obcc.cca.unpack
  (:require [clojure.java.io :as io]
            [obcc.cca.read :as cca.read]))

;;--------------------------------------------------------------------------------------
;; unpack - given a (pre-read) index of entries and an outputdir, unpack each element
;;--------------------------------------------------------------------------------------
(defn unpack [index outputdir verbose]
  (dorun
   (for [[path item] index]
     (let [entry (:entry item)
           outputfile (io/file outputdir path)]

       ;; ensure our output path exists
       (io/make-parents outputfile)

       ;; walk each entry and stream it out to the filesystem
       (with-open [is (cca.read/entry-stream item)
                   os (io/output-stream outputfile)]

         ;; we optionally may report out status to stdout
         (when (= verbose :true)
           (println (:sha1 entry) (:path entry) (str "(" (:size entry) " bytes)")))

         ;; stream it out, pulling the input stream through any appropriate decompressor transparently
         (io/copy is os))))))
