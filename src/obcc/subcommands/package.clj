(ns obcc.subcommands.package
  (:require [obcc.dar.write :as dar]
            [clojure.java.io :as io]))

(defn run [path config]
  (dar/write path ["src" "chaincode.conf"] (io/file path "build" "chaincode.cca")))
