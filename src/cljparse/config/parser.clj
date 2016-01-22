(ns cljparse.config.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))

(def grammar (insta/parser (io/resource "config.bnf")))

(defn parser [file]
  (grammar file))
