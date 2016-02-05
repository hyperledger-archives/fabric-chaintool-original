(ns obcc.build.golang
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

;;(deftype Field    [^String modifier ^String type ^String name ^String index])
;;(deftype Message  [^String name ^ArrayList fields])
;;
;;;;-----------------------------------------------------------------
;;;; manage object names
;;;;-----------------------------------------------------------------
;;(defn qualifyname [base name]
;;  (str (string/replace base "." "_") "_" name))
;;
;;;; scalar types should just be passed naked.  user types should be fully qualified
;;(defn typeconvert [basename [type name]]
;;  (if (= type :scalar)
;;      name
;;      (qualifyname basename name)))
;;
;;;;-----------------------------------------------------------------
;;;; buildX - build our ST friendly objects from the AST
;;;;-----------------------------------------------------------------
;;(defn buildfields [basename ast]
;;  (let [rawfields (intf/getfields ast)]
;;    (into {} (map (fn [[index {:keys [modifier type fieldName]}]]
;;                    (vector index (->Field modifier (typeconvert basename type) fieldName index))) rawfields))))
;;
;;(defn buildmessage [basename ast]
;;  (let [name (->> ast zip/right zip/node)
;;        fields (buildfields basename (->> ast zip/right zip/right))]
;;    (->Message (qualifyname basename name) fields )))
;;
;;(defn buildmessages [fqname ast]
;;  (loop [loc ast msgs '()]
;;    (cond
;;
;;      (or (nil? loc) (zip/end? loc))
;;      msgs
;;
;;      :else
;;      (let [node (->> loc zip/node)]
;;        (recur (->> loc zip/next)
;;               (if (= node :message)
;;                 (cons (buildmessage fqname loc) msgs)
;;                 msgs))))))
;;
;;(defn buildallmessages [ast aliases]
;;  (let [msgs (->> ast (map (fn [[fqname ast]] (buildmessages (aliases fqname) ast))) flatten)]
;;    (into {} (map #(vector (.name %) %) msgs))))

(defn buildtransactions [ast] nil)
(defn buildqueries [ast] nil)

;;-----------------------------------------------------------------
;; generate shim output - compiles the interfaces into a
;; golang shim, suitable for writing to a file
;;-----------------------------------------------------------------
(defn generateshim [interfaces]
  (let [transactions (buildtransactions interfaces)
        queries (buildqueries interfaces)
        stg  (STGroupFile. "generators/golang.stg")
        template (.getInstanceOf stg "golang")]

    ;;(.add template "transactions" transactions)
    ;;(.add template "queries" queries)
    (.render template)))

;;-----------------------------------------------------------------
;; compile - generates golang shim code and writes it to
;; the default location in the build area
;;-----------------------------------------------------------------
(defn compile [path interfaces]
  (let [shim (generateshim interfaces)
        shimpath (io/file path "build/src/obccshim/obccshim.go")]

    ;; ensure the path exists
    (io/make-parents shimpath)

    ;; and then emit our output
    (with-open [output (io/writer shimpath :truncate true)]
      (.write output shim))))
