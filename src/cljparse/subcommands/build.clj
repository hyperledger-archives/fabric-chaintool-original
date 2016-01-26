(ns cljparse.subcommands.build
  (:require [cljparse.config.parser :as config]))

(defn interfaces [config]
  (let [keys [[:configuration :provides] [:configuration :consumes]]]
        (->> (map #(config/find % config) keys) flatten (into #{}))))

(defn run [path config]
  (println "Build using configuration: " config)
  (println "Interfaces: " (interfaces config)))

