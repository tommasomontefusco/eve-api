(ns siphonator.eve-xml
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clj-http.client :as client]
            [clojure.core.memoize :as memo]
            [clj-time.core :as joda-time]
            [clj-time.format :as joda-format])
  (:import (java.io ByteArrayInputStream)))

;; Header stuff, for HTTP calls.
;; ===========================================================================

(def default-headers {:client-params {"http.useragent" "eve-xml library
for Clojure. Cobbled together by Az, email: az4reus@gmail.com. I also hang out
on Tweetfleet Slack, tell Foxfour to poke me. Come say hi :3"}})

(def headers-cache (atom default-headers))

(defn get-headers [] (@headers-cache))

(defn set-headers
  "Sets the headers that are sent with each HTTP request. Useful for when you
  want to replace the useragent, for example, with better contact info. No
  validation of the headers is done, but if you fuck up and get errors from
  clj-http, send nil to this function and it will be reset to standard headers,
  which is just a useragent."
  [headers-map]
  (if (nil? headers-map)
    (reset! headers-cache default-headers)
    (reset! headers-cache headers-map)))

;; Make request URLs. Just some basic composition stuff.
;; ============================================================================

(defn append-api-string
  "Simple helper method to append the API verification string at the end of
  an XML-API call when needed."
  [base-request api-key v-code]
  (str base-request "?keyID=" api-key "&vCode=" v-code))

(defn append-character-id
  "Another simple helper adding the character string to an existing URL."
  [base-request character-id]
  (if-not (re-find #"\?" base-request)
    (str base-request "?characterID=" character-id)
    (str base-request "&characterID=" character-id)))

(defn make-request-url
  "Another simple helper function to create the basic request url, eg
  'eve/assets'"
  [xml-api]
  (str "https://api.eveonline.com/" xml-api ".xml.aspx"))

(defn create-authenticated-url
  "Composition of a few functions to make authed calls easier"
  [xml-api api-key v-code]
  (-> (make-request-url xml-api)
      (append-api-string api-key v-code)))

(defn create-character-authenticated-url
  "Makes a full personal query, API and character ID"
  [xml-api api-key v-code char-id]
  (-> (create-authenticated-url xml-api api-key v-code)
      (append-character-id char-id)))

;; Impure I/O stuff, mostly concerned with handling the calls and the XML.
;; ============================================================================

(defn xml-to-map [s]
  (zip/xml-zip
    (xml/parse (ByteArrayInputStream. (.getBytes s)))))

(def api-expiration-cache (atom {}))

(defn parse-timestamp
  "Extracts the timestamp from the full xml result. Returns"
  [timestamp]
  (joda-format/parse (joda-format/formatters :mysql) timestamp))

(defn cache-timestamp!
  "Stores the timestamp in the expiration cache, parsed from the XML"
  [request-url timestamp]
  (swap! api-expiration-cache conj {request-url timestamp}))

(defn extract-xml-timestamp
  "Walks the XML of any eve API and extracts the timestamp. Used for caching
  expiration dates and making sure things don't suck."
  [xml-result]
  (-> (xml-to-map xml-result)
      (get-in [0 :content 1 :content 0])
      (clojure.string/trim)))

(defn update-cache!
  "Extracts and stores the timestamp from any given XML batch. Emits entered
  XML for returning purposes."
  [request xml-result]
  (->> (extract-xml-timestamp xml-result)
       (cache-timestamp! request))
  xml-result)

(defn raw-http-get
  "Uses clj-http to send a GET request to the URL. Header-map optional,
  send with :headers. If you do not include headers, a default header
  mapping will be used. Updates expiration dates cache, but is not memoized
  itself. This seems counterintuitive until you realise that you shouild
  not be using this method, hence it being private. "
  [request-url]
  (->> (client/get request-url (get-headers))
       (update-cache! request-url)))

(def memoized-raw-http-call (memoize raw-http-get))

(defn get-cached-time
  [request-url]
  (get @api-expiration-cache request-url))

(defn cached-until
  "simply returns the cached value as a joda-time/Interval to joda-time/now."
  [request-url]
  (joda-time/minus (get-cached-time request-url) (joda-time/now)))

(defn is-expired?
  [previous-time-str-timestamp]
  (if (nil? previous-time-str-timestamp)
    true
    (let [now  (joda-time/now)
          then (parse-timestamp previous-time-str-timestamp)]
      (joda-time/after? now then))))

(defn api-request
  [request-url]
  (let [cache @api-expiration-cache]
    (if (is-expired? (get cache request-url))
      (do (memo/memo-clear! memoized-raw-http-call request-url)
          (memoized-raw-http-call request-url))
      (do (memoized-raw-http-call request-url)))))

;; high-level interface, the friendly part. Use the stuff below.
;; ===========================================================================

(defn get-asset-list
  "Grabs the corp-asset list for any given api key, if available. If not, an
  error will be thrown."
  [api-code v-key]
  (throw (IllegalStateException. "Function not properly implemented yet.")))

(defn get-sov-map
  "Grbas and returns the giant XML abomination known as the soverignty
  map. Deal with ti at your own peril. At least it's cached for you.
  And it's a clojure map now. Should make it somehwat easier to deal with."
  []
  (-> (make-request-url "map/sovereignty")
      (api-request)
      (xml-to-map)))
