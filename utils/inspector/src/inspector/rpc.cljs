(ns inspector.rpc
  (:require [cljs.nodejs :as nodejs]))

(def http (nodejs/require "http"))

(defn- stringify [json]
  (.stringify js/JSON json))

(defn- response-handler [cb resp]
  (.setEncoding resp "utf8")
  (.on resp "data" (fn [data]
                     (let [resp (js->clj (.parse js/JSON data) :keywordize-keys true)]
                       (cb (select-keys resp [:error :result]))))))

(defn- post [{:keys [host port path method id func args cb]}]
  (let [meta #js {:host host
                  :port port
                  :path path
                  :method "POST"
                  :headers #js {:Content-Type "application/json"}}
        data (stringify
              #js {:jsonrpc "2.0"
                   :method method
                   :params #js {:type 3
                                :chaincodeID id
                                :ctorMsg #js {:function func
                                              :args #js [(.toBase64 args)]}}
                   :id "1"})
        req (.request http meta (partial response-handler cb))]

    (.write req data)
    (.end req)))

(defn query [args]
  (post (assoc args :path "/chaincode" :method "query")))
