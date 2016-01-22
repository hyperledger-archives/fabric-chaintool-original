(ns cljparse.config.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def skipper (insta/parser (io/resource "skip.bnf")))

(def grammar (insta/parser (io/resource "config.bnf") :auto-whitespace skipper))

(defn parser [file]
  (grammar (slurp file)))
