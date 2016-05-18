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
            OrgHyperledgerChaintoolMeta$InterfaceDescriptor])
  (:require [clojure.java.io :as io]
            [clj-http.client :as http]
            [flatland.protobuf.core :as fl]
            [clojure.data.codec.base64 :as base64]
            [cheshire.core :as json]
            [chaintool.codecs :as codecs]))

(def GetInterfacesParams (fl/protodef OrgHyperledgerChaintoolMeta$GetInterfacesParams))
(def Interfaces          (fl/protodef OrgHyperledgerChaintoolMeta$Interfaces))
(def InterfaceDescriptor (fl/protodef OrgHyperledgerChaintoolMeta$InterfaceDescriptor))

(defn- encode [item] (-> item fl/protobuf-dump base64/encode (String. "UTF-8")))
(defn- decode [type item] (->> item .getBytes base64/decode (fl/protobuf-load type)))

(defn- post [{:keys [host port method name func args]}]
  (let [url (str "http://" host ":" port "/chaincode")
        body {:jsonrpc "2.0"
              :method method
              :params {:type 3
                       :chaincodeID {:name name}
                       :ctorMsg {:function func
                                 :args [(encode args)]}}
              :id "1"}]
    ;; (println (str "HTTP POST:" url " - " body))
    (println "Connecting to" url)
    (http/post url
               {:content-type :json
                :accept :json
                :form-params body})))

(defn- query [args]
  (post (assoc args :method "query")))

;;--------------------------------------------------------------------------------------
;; make-input-stream - factory function for creating an input-stream for a specific entry
;;
;; We install the necessary decompressor such that the output of this stream represents
;; raw, uncompressed original data
;;--------------------------------------------------------------------------------------
(defn make-input-stream [data]
  (let [is (.newInput data)]
    (codecs/decompressor "gzip" is)))

(defn run [{:keys [host port] :as options}]
  (let [{:keys [body]} (query (assoc options
                               :func "org.hyperledger.chaintool.meta/query/1"
                               :args (fl/protobuf GetInterfacesParams :IncludeContent (some? (:interfaces options)))))
        response (-> body (json/parse-string true) (select-keys [:result :error]))]

    (if (= (-> response :result :status) "OK")

      (let [interfaces (->> response :result :message (decode Interfaces))]
        (println "Exported Interfaces:")
        (dorun (for [{:keys [name data]} (:descriptors interfaces)]
                 (do
                   (println "\t-" name)
                   (when-let [path (:interfaces options)]
                     (let [is (make-input-stream data)
                           file (io/file path (str name ".cci"))]

                       (io/make-parents file)
                       (with-open [os (io/output-stream file)]
                         (io/copy is os))))))))

      ;; else
      (println "Error:" response))))
