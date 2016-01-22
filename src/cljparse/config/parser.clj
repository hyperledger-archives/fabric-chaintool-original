(ns cljparse.config.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def whitespace (insta/parser "whitespace = #'[ \t]*'"))

(def grammar (insta/parser (io/resource "config.bnf") :auto-whitespace whitespace))

(defn parser [file]
  (grammar (slurp file)))
