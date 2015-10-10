(defproject siphonator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [refactor-nrepl "1.2.0"]
                 [cider/cider-nrepl "0.9.1"]
                 [org.clojure/tools.nrepl "0.2.11"]
                 [clj-http "2.0.0"]]
  :main ^:skip-aot siphonator.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
