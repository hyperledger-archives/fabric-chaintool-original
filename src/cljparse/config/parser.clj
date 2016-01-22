(ns cljparse.config.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def grammar (insta/parser (io/resource "config.bnf")))

(defn parser [file]
  (grammar (slurp file)))
