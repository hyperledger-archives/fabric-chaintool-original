(ns obcc.subcommands.build
  (:require [obcc.build.interface :as intf]
            [obcc.build.protobuf :as pb]))

(defn run [path config]
  (println "Build using configuration for " path)
  (let [interfaces (intf/compile path config)
        aliases (intf/aliases config)]
    (pb/compile path interfaces aliases)))
