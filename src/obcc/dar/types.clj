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
(ns obcc.dar.types
  (:import [obcc.dar
            Dar$CompatibilityHeader
            Dar$Archive
            Dar$Archive$Signature
            Dar$Archive$Payload
            Dar$Archive$Payload$Compression
            Dar$Archive$Payload$Entries])
  (:require [flatland.protobuf.core :as fl]))

(def Header      (fl/protodef Dar$CompatibilityHeader))
(def Archive     (fl/protodef Dar$Archive))
(def Signature   (fl/protodef Dar$Archive$Signature))
(def Payload     (fl/protodef Dar$Archive$Payload))
(def Compression (fl/protodef Dar$Archive$Payload$Compression))
(def Entries     (fl/protodef Dar$Archive$Payload$Entries))
