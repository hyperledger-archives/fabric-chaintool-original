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

(ns hlcc.protobuf.generate
  (:require [clojure.java.io :as io]
            [hlcc.build.interface :as intf]
            [hlcc.util :as util])
  (:import (java.util ArrayList)
           (org.stringtemplate.v4 STGroupFile))
  (:refer-clojure :exclude [compile]))

;; types to map to java objects that string template expects.
;;

(deftype Field    [^String modifier ^String type ^String name ^String index])
(deftype Message  [^String name ^ArrayList fields])

(defn typeconvert [[_ name]] name)

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects from the AST
;;-----------------------------------------------------------------
(defn buildfields [fields]
  (into {} (map (fn [[index {:keys [modifier type fieldName]}]]
                  (vector index (->Field modifier (typeconvert type) fieldName index))) fields)))

(defn buildmessage [[name fields]]
  (->Message name (buildfields fields)))

(defn buildmessages [ast]
  (let [msgs (map buildmessage (intf/getmessages ast))]
    (into {} (map #(vector (.name %) %) msgs))))

;;-----------------------------------------------------------------
;; to-string - compiles the interface into a protobuf
;; specification in a string, suitable for writing to a file or
;; passing to protoc
;;-----------------------------------------------------------------
(defn to-string [package [name ast]]
  (let [messages (buildmessages ast)
        stg  (STGroupFile. "generators/proto.stg")
        template (.getInstanceOf stg "protobuf")]

    (.add template "package" (if (nil? package) name package))
    (.add template "messages" messages)
    (.render template)))

;;-----------------------------------------------------------------
;; to-file - generates a protobuf specification and writes
;; it to a file
;;-----------------------------------------------------------------
(defn to-file [filename package interface]
  (util/truncate-file filename (to-string package interface)))
