(ns eve-api.crest
  (:require [cheshire.core :as ch]
            [clj-time.core :as time]
            [clj-time.format :as t-format]
            [clj-http.client :as client]))


;; Time functions, used for caching etc.
;; =============================================================================

(defn delay-by
  [delay-in-seconds]
  (time/plus (time/now) (time/seconds delay-in-seconds)))

(defn unparse-time-interval
  [time-interval-object]
  (t-format/unparse (t-format/formatters :mysql) time-interval-object))

(defn parse-time-str
  [time-str]
  (t-format/parse (t-format/formatters :mysql) time-str))

;; Caching the JSON requests
;; =============================================================================

(def json-expiration-cache (atom {}))

(defn store-url-expiration
  [url expiration-str]
  (swap! json-expiration-cache conj {url expiration-str}))

(defn cache-url-expiration
  ([url clj-time-object]
   (->> (unparse-time-interval clj-time-object)
        (store-url-expiration url)))
  ([url expiration-timestamp]
    (store-url-expiration url expiration-timestamp)))

(defn http-get-json
  [url]
  (client/get url {:as :json}))

(def memo-http-get-json (clojure.core.memoize/memo http-get-json))