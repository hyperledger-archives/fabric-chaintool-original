(ns obcc.config.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.zip :as zip]))

(def skipper (insta/parser (io/resource "parsers/config/skip.bnf")))

(def grammar (insta/parser (io/resource "parsers/config/grammar.bnf") :auto-whitespace skipper))

(defn parser [file] (->> file slurp grammar zip/vector-zip))

(defn isnode? [node] (if (and (vector? node) (keyword? (first node))) :true nil))
(defn nodename [node] (if (isnode? node) (first node) nil))

(defn fqp [_loc]
  (loop [loc _loc
         path ()]

    (cond
      (= loc nil)
      (vec path)

      :else
      (recur (zip/up loc) (->> loc zip/node nodename (conj path))))))

(defn find [path tree]
  (loop [loc tree]
    (cond
      (zip/end? loc)
      nil

      (= (fqp loc) path)
      (->> loc zip/node rest vec)

      :else
      (recur (zip/next loc)))))
