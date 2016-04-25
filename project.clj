(defproject chaintool "0.5.2-SNAPSHOT"
  :description "hyperledger chaincode tool"
  :url "https://github.com/ghaskins/chaintool"
  :license {:name "Apache License"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :min-lein-version "2.0.0"
  :lein-release {:deploy-via :shell :shell ["true"]}
  :javac-options ["-target" "1.7" "-source" "1.7"]
  :java-source-paths ["src"]
  :plugins [[lein-bin "0.3.5"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/algo.generic "0.1.2"]
                 [instaparse "1.4.1"]
                 [clojure-tools "1.1.3"]
                 [org.antlr/ST4 "4.0.8"]
                 [me.raynes/conch "0.8.0"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojars.ghaskins/protobuf "0.1"]
                 [commons-io/commons-io "2.4"]
                 [org.tukaani/xz "1.5"]
                 [org.apache.commons/commons-compress "1.10"]
                 [com.github.jponge/lzma-java "1.3"]
                 [pandect "0.5.4"]
                 [doric "0.9.0"]
                 [circleci/clj-yaml "0.5.5"]
                 [slingshot "0.12.2"]]
  :main ^:skip-aot chaintool.core
  :bin {:name "chaintool"
        :bin-path "target"
        :bootclasspath true}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
