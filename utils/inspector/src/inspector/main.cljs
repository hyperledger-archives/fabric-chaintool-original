(ns inspector.main
  (:require [clojure.string :as string]
            [cljs.nodejs :as nodejs]
            [cljs.tools.cli :refer [parse-opts]]
            [inspector.core :as core]))

(nodejs/enable-util-print!)

(def options
  [[nil "--host HOST" "Host name"
    :default "localhost"]
   [nil "--port PORT" "Port number"
    :default 3000
    :parse-fn #(js/parseInt %)
    :validate [#(< 0 % 65536) "Must be a number between 0 and 65536"]]
   ["-n" "--name NAME" "Name of the chaincode"]
   ["-h" "--help"]])

(defn exit [status msg & rest]
  (do
    (apply println msg rest)
    status))

(defn prep-usage [msg] (->> msg flatten (string/join \newline)))

(defn usage [options-summary]
  (prep-usage ["Usage: chaincode-inspector [options]"
               ""
               "Options Summary:"
               options-summary
               ""
               ]))

(defn run [{:keys [name] :as options}]
  (if name
    (core/inspect (assoc options :id #js {:name name}))
    (println "ERROR: --name required")))

(defn -main [& args]
    (let [{:keys [options arguments errors summary]} (parse-opts args options)]
    (cond

      (:help options)
      (exit 0 (usage summary))

      (not= errors nil)
      (exit -1 "Error: " (string/join errors))

      :else
      (run options))))

(set! *main-cli-fn* -main)
