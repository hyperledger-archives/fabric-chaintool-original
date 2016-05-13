(ns example02.core
  (:require [cljs.nodejs :as nodejs]
            [example02.http :as http]))

(def pb (nodejs/require "protobufjs"))
(def builder (.newBuilder pb))

(defn- loadproto [name]
  (do
    (.loadProtoFile pb (str "./" name ".proto") builder)
    (.build builder name)))

(def app (loadproto "org.hyperledger.chaincode.example02"))

(defn check-balance [{:keys [host port id cb]}]
  (http/invoke {:host host
                :port port
                :id #js {:name "mycc"}
                :func "org.hyperledger.chaincode.example02/query/1"
                :args (app/Entity. #js {:id id})}))

(defn run [{:keys [host port] :as options}]
  (let [url (str "http://" host ":" port)]
    (println "Connecting to" url)
    (check-balance (assoc options :id "foo" :cb (fn [resp] (println resp))))))
