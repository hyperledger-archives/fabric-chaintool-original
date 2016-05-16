(defproject chaincode-inspector "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/tools.cli "0.3.3"]]
  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :plugins [[lein-npm "0.6.1"]
            [lein-cljsbuild "1.1.3"]]
  :npm {:dependencies [[source-map-support "0.4.0"]
                       [protobufjs "5.0.1"]]}
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release"]
  :target-path "target"
  :cljsbuild {:builds [{:id "chaincode-inspector"
                        :source-paths ["src"]
                        :compiler {:output-to "out/chaincode-inspector.js"
                                   :output-dir "out"
                                   :source-map true
                                   :optimizations :none
                                   :target :nodejs
                                   :main "inspector.main"
                                   :pretty-print true}}]})
