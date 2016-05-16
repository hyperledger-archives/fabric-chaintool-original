(ns inspector.core
  (:require [cljs.nodejs :as nodejs]
            [inspector.rpc :as rpc]))

(def pb (nodejs/require "protobufjs"))
(def builder (.newBuilder pb))

(defn- loadproto [name]
  (do
    (.loadProtoFile pb (str "./" name ".proto") builder)
    (.build builder name)))

(def app (loadproto "org.hyperledger.chaintool.meta"))

(defn inspect [options]
  (rpc/query (assoc options
                    :func "org.hyperledger.chaintool.meta/query/1"
                    :args (app.GetInterfacesParams. #js {:IncludeContent true})
                    :cb (fn [resp]
                          (if (= (->> resp :result :status) "OK")
                            (let [result (->> resp :result :message app.Interfaces.decode64)]
                              (println "Success:")
                              (dorun (for [desc result.descriptors]
                                         (println "\t" desc.name))))
                            ;; else
                            (println "Failure:" resp))))))
