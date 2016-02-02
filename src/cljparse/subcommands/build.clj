(ns cljparse.subcommands.build
  (:require [cljparse.build.interface :as intf]))

(defn run [path config]
  (println "Build using configuration for " path)
  (intf/compile path config))

