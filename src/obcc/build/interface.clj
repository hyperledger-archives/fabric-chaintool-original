(ns obcc.build.interface
  (:import [org.stringtemplate.v4 STGroupFile ST])
  (:import [java.util ArrayList])
  (:require [clojure.java.io :as io]
            [clojure.zip :as zip]
            [instaparse.core :as insta]
            [obcc.config.parser :as config]))

;; types to map to java objects that string template expects.
;;

(deftype Field    [^String modifier ^String type ^String name ^String index])
(deftype Message  [^String name ^ArrayList fields])

(def grammar (insta/parser (io/resource "parsers/interface/grammar.bnf")
                           :auto-whitespace (insta/parser (io/resource "parsers/interface/skip.bnf"))))

(defn parse [intf] (->> intf grammar zip/vector-zip))

;; aggregate all of the interfaces declared in the config, adding the implicit
;; "project.cci"
(defn getinterfaces [config]
  (let [keys [[:configuration :provides] [:configuration :consumes]]
        explicit (map #(config/find % config) keys)]
    (->> explicit flatten (into #{}) (cons "project") (remove nil?) (into '()))))

(defn open [path intf]
  (let [file (io/file path (str intf ".cci"))]
    (cond
      (.exists file)
      file

      :else
      (throw (Exception. (str (.getAbsolutePath file) " not found"))))))

;; takes an interface name, maps it to a file, and if present, compiles
;; it to an AST.
(defn compileintf [path intf]
  (let [ipath (str path "/src/interfaces")]
    (->> (open ipath intf) slurp parse)))

;; compile all applicable interfaces into a map of ASTs keyed by interface name
(defn compileall [path config]
  (let [interfaces (getinterfaces config)]
    (into {} (map #(vector % (compileintf path %)) interfaces))))

;;-----------------------------------------------------------------
;; getX - extracts fields from an AST message
;;-----------------------------------------------------------------
(defn getfieldattrs [ast]
  (loop [loc ast attrs {}]
    (if (nil? loc)
      attrs
      ;; else
      (let [[k v] (zip/node loc)]
        (recur (zip/right loc) (assoc attrs k v))))))

(defn getfields [ast]
  (loop [loc ast fields {}]
    (cond

      (nil? loc)
      fields

      :else
      (let [attrs (->> loc zip/down zip/right getfieldattrs)]
        (recur (zip/right loc) (assoc fields (:index attrs) attrs))))))

;;-----------------------------------------------------------------
;; manage object names
;;-----------------------------------------------------------------
(defn qualifyname [base name]
  (str base "_" name))

;; scalar types should just be passed naked.  user types should be fully qualified
(defn typeconvert [basename [type name]]
  (if (= type :scalar)
      name
      (qualifyname basename name)))

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects from the AST
;;-----------------------------------------------------------------
(defn buildfields [basename ast]
  (let [rawfields (getfields ast)]
    (into {} (map (fn [[index {:keys [modifier type fieldName]}]]
                    (vector index (->Field modifier (typeconvert basename type) fieldName index))) rawfields))))

(defn buildmessage [basename ast]
  (let [name (->> ast zip/right zip/node)
        fields (buildfields basename (->> ast zip/right zip/right))]
    (->Message (qualifyname basename name) fields )))

(defn buildmessages [fqname ast]
  (loop [loc ast msgs '()]
    (cond

      (or (nil? loc) (zip/end? loc))
      msgs

      :else
      (let [node (->> loc zip/node)]
        (recur (->> loc zip/next)
               (if (= node :message)
                 (cons (buildmessage fqname loc) msgs)
                 msgs))))))

(defn buildallmessages [ast]
  (let [msgs (->> ast (map (fn [[fqname ast]] (buildmessages fqname ast))) flatten)]
    (into {} (map #(vector (.name %) %) msgs))))

(defn generateproto [path config]
  (let [asts (compileall path config)
        messages (buildallmessages asts)
        stg  (STGroupFile. "generators/proto.stg")
        template (.getInstanceOf stg "protobuf")]

    (.add template "messages" messages)
    (.render template)))

(defn compile [path config]
  (let [protobuf (generateproto path config)
        protopath (io/file path "build/proto/project.proto")]

    ;; ensure the path exists
    (io/make-parents protopath)

    ;; and then emit our output
    (with-open [output (io/writer protopath :truncate true)]
      (.write output protobuf))))
