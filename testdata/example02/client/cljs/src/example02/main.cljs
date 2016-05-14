(ns example02.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as string]
            [cljs.nodejs :as nodejs]
            [cljs.tools.cli :refer [parse-opts]]
            [cljs.core.async :refer [<!]]
            [example02.core :as core]))


(nodejs/enable-util-print!)

(def _commands
  [["deploy"
    {:fn core/deploy
     :default-args #js {:partyA #js {
                                     :entity "foo"
                                     :value 100
                                     }
                        :partyB #js {
                                     :entity "bar"
                                     :value 100
                                     }}}]
   ["check-balance"
    {:fn core/check-balance
     :default-args #js {:id "foo"}}]])

(def commands (into {} _commands))
(defn print-commands [] (->> commands keys vec print-str))

(def options
  [[nil "--host HOST" "Host name"
    :default "localhost"]
   ["-p" "--port PORT" "Port number"
    :default 3000
    :parse-fn #(js/parseInt %)
    :validate [#(< 0 % 65536) "Must be a number between 0 and 65536"]]
   ["-c" "--command CMD" (str "Command " (print-commands))
    :default "check-balance"
    :validate [#(contains? commands %) (str "Supported commands: " (print-commands))]]
   ["-a" "--args ARGS" "JSON formatted arguments to submit"]
   ["-h" "--help"]])

(defn exit [status msg & rest]
  (do
    (apply println msg rest)
    status))

(defn prep-usage [msg] (->> msg flatten (string/join \newline)))

(defn usage [options-summary]
  (prep-usage ["Usage: hello [general-options]"
               ""
               "General Options:"
               options-summary
               ""
               ]))

(defn run [{:keys [command args] :as options}]
  (let [desc (commands command)
        _args (if (nil? args) (:default-args desc) (.parse js/JSON args))]
    (println (str "Running " command "(" (.stringify js/JSON _args) ")"))
    ((:fn desc) (assoc options :args _args))))

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
