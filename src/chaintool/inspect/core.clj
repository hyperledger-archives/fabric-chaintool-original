;; Copyright London Stock Exchange Group 2016 All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;                  http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns chaintool.inspect.core
  (:import [org.hyperledger.chaintool.meta
            OrgHyperledgerChaintoolMeta$GetInterfacesParams
            OrgHyperledgerChaintoolMeta$Interfaces
            OrgHyperledgerChaintoolMeta$InterfaceDescriptor
            OrgHyperledgerChaintoolMeta$GetFactsParams
            OrgHyperledgerChaintoolMeta$Facts])
  (:require [clojure.java.io :as io]
            [clj-http.client :as http]
            [flatland.protobuf.core :as fl]
            [clojure.data.codec.base64 :as base64]
            [cheshire.core :as json]
            [chaintool.codecs :as codecs]
            [chaintool.util :as util]
            [doric.core :as doric]))

(def GetInterfacesParams (fl/protodef OrgHyperledgerChaintoolMeta$GetInterfacesParams))
(def Interfaces          (fl/protodef OrgHyperledgerChaintoolMeta$Interfaces))
(def InterfaceDescriptor (fl/protodef OrgHyperledgerChaintoolMeta$InterfaceDescriptor))
(def GetFactsParams      (fl/protodef OrgHyperledgerChaintoolMeta$GetFactsParams))
(def Facts               (fl/protodef OrgHyperledgerChaintoolMeta$Facts))

(defn- encode [item] (-> item fl/protobuf-dump base64/encode (String. "UTF-8")))
(defn- decode [type item] (->> item .getBytes base64/decode (fl/protobuf-load type)))

(defn- url [{:keys [host port]}]
  (str "http://" host ":" port "/chaincode"))

(defn- post [{:keys [method name func args] :as options}]
  (let [body {:jsonrpc "2.0"
              :method method
              :params {:type 3
                       :chaincodeID {:name name}
                       :ctorMsg {:function func
                                 :args [(encode args)]}}
              :id "1"}]

    (http/post (url options)
               {:content-type :json
                :accept :json
                :form-params body})))

(defn- query [args]
  (let [{:keys [body]} (post (assoc args :method "query"))
        response (-> body (json/parse-string true) (select-keys [:result :error]))]

    (if (= (-> response :result :status) "OK")
      (->> response :result :message)
      ;; else
      (util/abort -1 (str response)))))

;;--------------------------------------------------------------------------------------
;; make-input-stream - factory function for creating an input-stream for a specific entry
;;
;; We install the necessary decompressor such that the output of this stream represents
;; raw, uncompressed original data
;;--------------------------------------------------------------------------------------
(defn- make-input-stream [data]
  (let [is (.newInput data)]
    (codecs/decompressor "gzip" is)))

(defn- get-interfaces [{:keys [host port] :as options}]
  (let [response (query (assoc options
                               :func "org.hyperledger.chaintool.meta/query/1"
                               :args (fl/protobuf GetInterfacesParams :IncludeContent (some? (:interfaces options)))))]

    (decode Interfaces response)))

(defn- get-facts [{:keys [host port] :as options}]
  (let [response (query (assoc options
                               :func "org.hyperledger.chaintool.meta/query/3"
                               :args (fl/protobuf GetFactsParams)))]

    (decode Facts response)))

(defn run [options]

  (println "Connecting to" (url options))

  (let [{:keys [facts]} (get-facts options)
        interfaces (get-interfaces options)]

    (println (doric/table [{:name :name :title "Fact"} {:name :value}] facts))

    (println "Exported Interfaces:")
    (dorun (for [{:keys [name data]} (:descriptors interfaces)]
             (do
               (println "\t-" name)
               (when-let [path (:interfaces options)]
                 (let [is (make-input-stream data)
                       file (io/file path (str name ".cci"))]

                   (io/make-parents file)
                   (with-open [os (io/output-stream file)]
                     (io/copy is os)))))))))
