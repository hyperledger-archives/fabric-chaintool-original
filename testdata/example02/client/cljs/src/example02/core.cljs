(ns example02.core
  (:require [cljs.nodejs :as nodejs]
            [example02.http :as http]))

(def pb (nodejs/require "protobufjs"))
(def builder (.newBuilder pb))

(defn- loadproto [name]
  (do
    (.loadProtoFile pb (str "./" name ".proto") builder)
    (.build builder name)))

(def init (loadproto "appinit"))
(def app (loadproto "org.hyperledger.chaincode.example02"))

(defn deploy [{:keys [host port args cb]}]
  (http/deploy {:host host
                :port port
                :id #js {:name "mycc"}
                :func "init"
                :args (init.Init. args)
                :cb cb}))

(defn check-balance [{:keys [host port id cb]}]
  (http/query {:host host
               :port port
               :id #js {:name "mycc"}
               :func "org.hyperledger.chaincode.example02/query/1"
               :args (app.Entity. #js {:id id})
               :cb cb}))

(defn handler [resp]
  (.setEncoding resp "utf8")
  (.on resp "data" (fn [data]
                     (println data))))

(defn run [{:keys [host port] :as options}]
  (let [url (str "http://" host ":" port)]
    (println "Connecting to" url)

    (deploy {:host host
             :port port
             :args #js {:partyA #js {
                                     :entity "foo"
                                     :value 100
                                     }
                        :partyB #js {
                                     :entity "bar"
                                     :value 100
                                     }}
             :cb handler})

    (check-balance {:host host
                    :port port
                    :id "foo"
                    :cb handler})))
