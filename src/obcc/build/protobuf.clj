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

(ns obcc.build.protobuf
  (:require [clojure.java.io :as io]
            [obcc.build.interface :as intf]
            [obcc.util :as util])
  (:import (java.util ArrayList)
           (org.stringtemplate.v4 STGroupFile))
  (:refer-clojure :exclude [compile]))

;; types to map to java objects that string template expects.
;;

(deftype Field    [^String modifier ^String type ^String name ^String index])
(deftype Message  [^String name ^ArrayList fields])

;; scalar types should just be passed naked.  user types should be fully qualified
(defn typeconvert [namespace [type name]]
  (if (= type :scalar)
    name
    (util/qualifyname namespace name)))

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects from the AST
;;-----------------------------------------------------------------
(defn buildfields [namespace fields]
  (into {} (map (fn [[index {:keys [modifier type fieldName]}]]
                  (vector index (->Field modifier (typeconvert namespace type) fieldName index))) fields)))

(defn buildmessage [namespace [name fields]]
  (->Message (util/qualifyname namespace name) (buildfields namespace fields)))

(defn buildmessages [namespace ast]
  (map #(buildmessage namespace %) (intf/getmessages ast)))

(defn buildallmessages [ast namespaces]
  (let [msgs (->> ast (map (fn [[namespace ast]] (buildmessages (namespaces namespace) ast))) flatten)]
    (into {} (map #(vector (.name %) %) msgs))))

;;-----------------------------------------------------------------
;; generate protobuf output - compiles the interfaces into a
;; protobuf specification, suitable for writing to a file or
;; passing to protoc
;;-----------------------------------------------------------------
(defn generateproto [interfaces namespaces]
  (let [messages (buildallmessages interfaces namespaces)
        stg  (STGroupFile. "generators/proto.stg")
        template (.getInstanceOf stg "protobuf")]

    (.add template "messages" messages)
    (.render template)))

;;-----------------------------------------------------------------
;; compile - generates a protobuf specification and writes it to
;; the default location in the build area
;;-----------------------------------------------------------------
(defn compile [path interfaces namespaces]
  (let [protobuf (generateproto interfaces namespaces)
        protofile (io/file path util/supportpath "wireprotocol.proto")]

    ;; ensure the path exists
    (io/make-parents protofile)

    ;; and emit our output
    (with-open [output (io/writer protofile :truncate true)]
      (.write output protobuf))

    ;; finally, return the name of the file so other tools (like protoc) may consume it
    protofile))
