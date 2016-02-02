(ns obcc.subcommands.build
  (:require [obcc.build.interface :as intf]))

(defn run [path config]
  (println "Build using configuration for " path)
  (intf/compile path config))

