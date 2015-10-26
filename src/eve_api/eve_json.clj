(ns eve-api.eve-json
  (:require [cheshire.core :as ch]
            [clj-time.core :as joda-time]
            [clj-time.format :as joda-format]
            [clj-http.client :as client]))

;; TODO caching
(def json-expiration-cache (atom {}))

(defn delay-by
  [delay-in-seconds]
  (joda-time/plus (joda-time/now) (joda-time/seconds delay-in-seconds)))

(defn unparse-time-interval
  [time-interval-object]
  (joda-format/unparse (joda-format/formatters :mysql) time-interval-object))

(defn store-url-expiration
  [url expiration-str]
  (swap! json-expiration-cache conj {url expiration-str}))

(defn cache-url-expiration
  [url clj-time-object]
  (->> (unparse-time-interval clj-time-object)
       (store-url-expiration url)))

(defn http-get
  [url]
  (client/get url {:as :json}))

(def memo-http-get (clojure.core.memoize/memo http-get))