(defproject obcc "0.2-SNAPSHOT"
  :description "openblockchain compiler"
  :url "https://github.com/openblockchain/obcc"
  :license {:name "Apache License"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :min-lein-version "2.0.0"
  :javac-options ["-target" "1.7" "-source" "1.7"]
  :java-source-paths ["src"]
  :plugins [[lein-bin "0.3.5"]
            [lein-kibit "0.1.2"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/algo.generic "0.1.2"]
                 [instaparse "1.4.1"]
                 [clojure-tools "1.1.3"]
                 [org.antlr/ST4 "4.0.8"]
                 [me.raynes/conch "0.8.0"]
                 [me.raynes/fs "1.4.6"]
                 [com.google.protobuf/protobuf-java "2.6.1"]
                 [org.flatland/useful "0.9.0"]
                 [org.flatland/schematic "0.1.0"]
                 [org.flatland/io "0.3.0"]
                 [ordered-collections "0.4.0"]
                 [gloss "0.2.1"]
                 [commons-io/commons-io "2.4"]
                 [org.tukaani/xz "1.5"]
                 [org.apache.commons/commons-compress "1.10"]
                 [com.github.jponge/lzma-java "1.2"]
                 [pandect "0.5.4"]
                 [doric "0.9.0"]]
  :main ^:skip-aot obcc.core
  :bin {:name "obcc"
        :bin-path "target"
        :bootclasspath true}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
