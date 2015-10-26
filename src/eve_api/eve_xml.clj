(ns eve-api.eve-xml
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

(def default-headers {:client-params {"http.useragent" "github.com/az4reus/eve-api"}})

(def headers-cache (atom default-headers))

(defn get-headers [] (deref headers-cache))

(defn add-header
  "Adds a new header to the header cache, instead of replacing it wholesale,
  like `set-headers` does. If your new header is in a nested map, you will
  have to provide that path, this function uses `conj`."
  ([new-header-map]
   (swap! headers-cache conj new-header-map))
  ([new-header-key new-header-value]
   (swap! headers-cache conj {new-header-key new-header-value})))

(defn set-headers!
  "Sets the headers that are sent with each HTTP request. Useful for when you
  want to replace the useragent, for example, with better contact info. No
  validation of the headers is done."
  [headers-map]
  (reset! headers-cache headers-map))

(defn reset-headers!
  "Simply resets the headers cache to the default (a useragent for this lib)"
  []
  (reset! headers-cache default-headers))

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

(defn create-basic-request-url
  "Another simple helper function to create the basic request url, eg
  'map/sovereignty'"
  [xml-api-request]
  (str "https://api.eveonline.com/" xml-api-request ".xml.aspx"))

(defn create-authenticated-url
  "Composition of a few functions to make authed calls easier"
  [xml-api api-key v-code]
  (-> (create-basic-request-url xml-api)
      (append-api-string api-key v-code)))

(defn create-char-authenticated-url
  "Makes a full personal query, API and character ID"
  [xml-api api-key v-code char-id]
  (-> (create-authenticated-url xml-api api-key v-code)
      (append-character-id char-id)))

;; Impure I/O stuff, mostly concerned with handling the calls and the XML.
;; ============================================================================

(defn xml-to-map [s]
  "Helper method shamelessly copied from clojuredocs, because fuck XML parsing."
  (zip/xml-zip
    (xml/parse (ByteArrayInputStream. (.getBytes s)))))

(def api-expiration-cache (atom {}))

(defn parse-timestamp
  "Extracts the timestamp from the full xml result. Returns a clj-time object."
  [timestamp]
  (joda-format/parse (joda-format/formatters :mysql) timestamp))

(defn cache-timestamp!
  "Stores the timestamp in the expiration cache, parsed from the XML"
  [request-url timestamp]
  (swap! api-expiration-cache conj {request-url timestamp}))

(defn extract-xml-timestamp
  "Walks the XML of any eve API and extracts the timestamp. Used for caching
  expiration dates and making sure things don't suck."
  [api-result]
  (->
    (xml-to-map api-result)
    (get-in [0 :content 2 :content 0])
    (clojure.string/trim)))

(defn extract-rowset
  [full-xml-map]
  (get-in full-xml-map [0 :content 1]))

(defn update-cache!
  "Extracts and stores the timestamp from any given XML batch. Emits entered
  XML for chaining purposes."
  [request xml-result]
  (->> (extract-xml-timestamp xml-result)
       (cache-timestamp! request))
  xml-result)

(defn http-get
  "Uses clj-http to send a GET request to the URL, with the headers in the
  cache. Updates expiration dates cache, but is not memoized itself yet. "
  [request-url]
  (->> (client/get request-url (get-headers))
       (:body)
       (update-cache! request-url)))

(def memoized-http-get (memoize http-get))

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

(defn cached-http-get
  [request-url]
  (let [cache @api-expiration-cache]
    (if (is-expired? (get cache request-url))
      (do (memo/memo-clear! memoized-http-get request-url)
          (memoized-http-get request-url))
      (do (memoized-http-get request-url)))))

;; high-level interface, the friendly part. Use the stuff below.
;; ===========================================================================

(defn api-call
  ([xml-uri]
   (-> (create-basic-request-url xml-uri)
       (cached-http-get)
       (xml-to-map)
       (extract-rowset)
       (:content)
       (first)))
  ([xml-uri api-key v-code]
   (-> (create-authenticated-url xml-uri api-key v-code)
       (cached-http-get)
       (xml-to-map)
       (extract-rowset)
       (:content)
       (first)))
  ([xml-uri api-key v-code char-id]
   (-> (create-char-authenticated-url xml-uri api-key v-code char-id)
       (cached-http-get)
       (xml-to-map)
       (extract-rowset)
       (:content)
       (first))))

(defn get-asset-list
  "Grabs the corp-asset list for any given api key, if available. If not, an
  error will be thrown."
  [api-key v-code]
  (api-call "char/AssetList" api-key v-code))

(defn get-sov-map
  "Grbas and returns the giant XML abomination known as the soverignty
  map. Deal with ti at your own peril. At least it's cached for you.
  And it's a clojure map now. Should make it somehwat easier to deal with."
  []
  (api-call "Map/Sovereignty"))

(defn get-server-status
  []
  (api-call "Server/ServerStatus"))