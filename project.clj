(defproject eve-api "0.1.0-SNAPSHOT"
  :description "A small library to facilitate Eve Online API interaction."
  :url "https://github.com/Az4reus/eve_api"
  :license {:name "MIT License"
            :url  "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "2.0.0"]
                 [org.clojure/core.memoize "0.5.6"]
                 [clj-time "0.11.0"]
                 [cheshire "5.5.0"]]
  :main ^:skip-aot eve-api.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

