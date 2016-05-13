(ns example02.http
  (:require [cljs.nodejs :as nodejs]))

(def http (nodejs/require "http"))

(defn post [{:keys [host port path method id func args cb]}]
  (let [meta #js {:host host
                  :port port
                  :path path
                  :method 'POST'
                  :headers #js {:Content-Type 'application/json'}}
        data #js {:jsonrpc "2.0"
                  :method method
                  :params #js {:type "3"
                               :chaincodeID id
                               :ctorMsg #js {:function func
                                             :args (.toBase64 args)}}
                  :id "1"}
        req (.request http meta cb)]

    (println data)
    (.write req data)
    (.end req)))

(defn- chaincode-post [args]
  (post (assoc args :path "/chaincode")))

(defn deploy [args]
  (chaincode-post (assoc args :method "deploy")))

(defn invoke [args]
  (chaincode-post (assoc args :method "invoke")))

(defn query [args]
  (chaincode-post (assoc args :method "query")))
