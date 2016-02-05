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

(deftype Function  [^String rettype ^String name ^String param ^Integer index])
(deftype Interface  [^String name ^String shortname ^ArrayList functions])


;;-----------------------------------------------------------------
;; manage object names
;;-----------------------------------------------------------------
(defn qualifyname [base name]
  (str (string/replace base "." "_") "_" name))

;;-----------------------------------------------------------------
;; find a specific element in the AST
;;-----------------------------------------------------------------
(defn find [term ast]
  (loop [loc ast]
    (cond

      (or (nil? loc) (zip/end? loc))
      nil

      (= (zip/node loc) term)
      (zip/up loc)

      :else
      (recur (zip/next loc)))))

(defn transactions? [ast] (find :transactions ast))
(defn queries? [ast] (find :queries ast))

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects from the AST
;;-----------------------------------------------------------------
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

(defn buildinterface [name view aliases]
  )

(defn build [interfaces aliases pred]
  (let [candidates (for [[name ast] interfaces :let [view (pred ast)] :when view] [name view])]
    (into {} (map (fn [[name view]] (vector name (buildinterface name view aliases))) candidates))))

(defn buildtransactions [interfaces aliases] (build interfaces aliases transactions?))
(defn buildqueries [interfaces aliases] (build interfaces aliases queries?))

;;-----------------------------------------------------------------
;; generate shim output - compiles the interfaces into a
;; golang shim, suitable for writing to a file
;;-----------------------------------------------------------------
(defn generateshim [interfaces aliases]
  (let [transactions (buildtransactions interfaces aliases)
        queries (buildqueries interfaces aliases)
        stg  (STGroupFile. "generators/golang.stg")
        template (.getInstanceOf stg "golang")]

    (.add template "transactions" transactions)
    (.add template "queries" queries)
    (.render template)))

;;-----------------------------------------------------------------
;; compile - generates golang shim code and writes it to
;; the default location in the build area
;;-----------------------------------------------------------------
(defn compile [path interfaces aliases]
  (let [shim (generateshim interfaces aliases)
        shimpath (io/file path "build/src/obccshim/obccshim.go")]

    ;; ensure the path exists
    (io/make-parents shimpath)

    ;; and then emit our output
    (with-open [output (io/writer shimpath :truncate true)]
      (.write output shim))))
