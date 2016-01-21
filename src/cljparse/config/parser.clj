(ns cljparse.config.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))

(defn parser [file]
  (println "Parsing" file))
