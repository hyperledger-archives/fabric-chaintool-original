(defproject obcc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [instaparse "1.4.1"]
                 [clojure-tools "1.1.3"]]
  :main ^:skip-aot obcc.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
