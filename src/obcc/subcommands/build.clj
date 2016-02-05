(ns obcc.subcommands.build
  (:require [obcc.build.interface :as intf]
            [obcc.build.protobuf :as pb]
            [obcc.build.golang :as go]))

(defn run [path config]
  (println "Build using configuration for " path)
  (let [interfaces (intf/compile path config)
        aliases (intf/aliases config)
        protofile (pb/compile path interfaces aliases)]

    ;; generate golang shim output
    (go/compile path interfaces aliases protofile)))
