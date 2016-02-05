(ns obcc.build.protobuf
  (:import [org.stringtemplate.v4 STGroupFile ST])
  (:import [java.util ArrayList])
  (:require [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.string :as string]
            [instaparse.core :as insta]
            [obcc.config.parser :as config]
            [obcc.build.interface :as intf]))

;; types to map to java objects that string template expects.
;;

(deftype Field    [^String modifier ^String type ^String name ^String index])
(deftype Message  [^String name ^ArrayList fields])

;;-----------------------------------------------------------------
;; manage object names
;;-----------------------------------------------------------------
(defn qualifyname [base name]
  (str (string/replace base "." "_") "_" name))

;; scalar types should just be passed naked.  user types should be fully qualified
(defn typeconvert [basename [type name]]
  (if (= type :scalar)
      name
      (qualifyname basename name)))

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects from the AST
;;-----------------------------------------------------------------
(defn buildfields [basename ast]
  (let [rawfields (intf/getfields ast)]
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

(defn buildallmessages [ast aliases]
  (let [msgs (->> ast (map (fn [[fqname ast]] (buildmessages (aliases fqname) ast))) flatten)]
    (into {} (map #(vector (.name %) %) msgs))))

;;-----------------------------------------------------------------
;; generate protobuf output - compiles the interfaces into a
;; protobuf specification, suitable for writing to a file or
;; passing to protoc
;;-----------------------------------------------------------------
(defn generateproto [interfaces aliases]
  (let [messages (buildallmessages interfaces aliases)
        stg  (STGroupFile. "generators/proto.stg")
        template (.getInstanceOf stg "protobuf")]

    (.add template "messages" messages)
    (.render template)))

;;-----------------------------------------------------------------
;; compile - generates a protobuf specification and writes it to
;; the default location in the build area
;;-----------------------------------------------------------------
(defn compile [path interfaces aliases]
  (let [protobuf (generateproto interfaces aliases)
        protopath (io/file path "build/proto/project.proto")]

    ;; ensure the path exists
    (io/make-parents protopath)

    ;; and then emit our output
    (with-open [output (io/writer protopath :truncate true)]
      (.write output protobuf))))
