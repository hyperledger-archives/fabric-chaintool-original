(defproject obcc "0.1_SNAPSHOT"
  :description "openblockchain compiler"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-bin "0.3.5"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/algo.generic "0.1.2"]
                 [instaparse "1.4.1"]
                 [clojure-tools "1.1.3"]
                 [org.antlr/ST4 "4.0.8"]
                 [me.raynes/conch "0.8.0"]
                 [me.raynes/fs "1.4.6"]
                 [clojurewerkz/propertied "1.2.0"]]
  :main ^:skip-aot obcc.core
  :bin {:name "obcc"
        :bin-path "target"
        :bootclasspath true}
  :target-path "target/%s"
  :resource-paths ["resources" "_PROPSDIR_"]
  :profiles {:uberjar {:aot :all}})
