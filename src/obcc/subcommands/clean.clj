(ns obcc.subcommands.clean
  (:require [clojure.tools.file-utils :as fileutils]
            [clojure.java.io :as io]))

(defn run [path config]
  (println "Cleaning project found at " path)
  (fileutils/recursive-delete (io/file path "build")))
