(ns eve-api.xml
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clj-http.client :as client]
            [clojure.core.memoize :as memo]
            [clj-time.core :as time]
            [clj-time.format :as t-format]

            [eve-api.util :refer :all])
  (:import (java.io ByteArrayInputStream)))

;; Make request URLs. Just some basic composition stuff.
;; ============================================================================

(defn has-questionmark? [s]
  (not (= (.indexOf s "?")
           -1)))

(defn with-api-key
  "Simple helper method to append the API verification string at the end of
  an XML-API call when needed."
  [base-request api-key v-code]
  (str base-request "?keyID=" api-key "&vCode=" v-code))

(defn with-flat-response [base-url]
  (str base-url "&flat=1"))

(defn with-market-order [base-url market-order]
  (str base-url "&MarketOrder=" market-order))

(defn with-character-id [base-url char-id]
  (if (has-questionmark? base-url)
    (str base-url "&CharacterID=" char-id)
    (str base-url "?CharacterID=" char-id)))

(defn create-basic-request-url
  "Another simple helper function to create the basic request url, e.g.
  'map/sovereignty'"
  [xml-api-request]
  (str "https://api.eveonline.com/" xml-api-request ".xml.aspx"))

(defn create-authenticated-url
  "Composition of a few functions to make authed calls easier"
  [xml-api api-key v-code]
  (-> (create-basic-request-url xml-api)
      (with-api-key api-key v-code)))

(defn create-char-authenticated-url
  "Makes a full personal query, API and character ID"
  [xml-api api-key v-code char-id]
  (-> (create-authenticated-url xml-api api-key v-code)
      (with-character-id char-id)))

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
  (t-format/parse (t-format/formatters :mysql) timestamp))

(defn cache-timestamp!
  "Stores the timestamp in the expiration cache, parsed from the XML"
  [request-url timestamp]
  (swap! api-expiration-cache conj {request-url timestamp}))

(defn extract-xml-timestamp
  "Walks the XML of any eve API and extracts the timestamp. Used for caching
  expiration dates and making sure things don't suck."
  [api-result]
  (-> (xml-to-map api-result)
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
  (time/minus (get-cached-time request-url) (time/now)))

(defn is-expired?
  [previous-time-str-timestamp]
  (if (nil? previous-time-str-timestamp)
    true
    (let [now  (time/now)
          then (parse-timestamp previous-time-str-timestamp)]
      (time/after? now then))))

(defn cached-http-get
  [request-url]
  (let [cache @api-expiration-cache]
    (if (is-expired? (get cache request-url))
      (do (memo/memo-clear! memoized-http-get request-url)
          (memoized-http-get request-url))
      (memoized-http-get request-url))))

;; high-level interface, the friendly part. Use the stuff below.
;; ===========================================================================

(defn- request-url [url]
  (-> (cached-http-get url)
      (xml-to-map)
      (extract-rowset)
      (:content)))

(defn api-call
  ([xml-uri]
   (-> (create-basic-request-url xml-uri)
       (request-url)))
  ([xml-uri api-key v-code]
   (-> (create-basic-request-url xml-uri)
       (with-api-key api-key v-code)
       (request-url)))
  ([xml-uri api-key v-code char-id]
   (-> (create-basic-request-url xml-uri)
       (with-api-key api-key v-code)
       (with-character-id char-id)
       (request-url))))

;; Concrete endpoints. Aka the really fuckin' boring shit. Or interesting, if
;; you don't have to write it.
;; ============================================================================

;; Char Endpoints
;; ----------------------------------------------------------------------------

(defn get-account-status
  [api-key v-code]
  (api-call "account/AccountStatus" api-key v-code))

(defn get-api-key-info
  [api-key v-code]
  (api-call "account/APIKeyInfo" api-key v-code))

(defn get-characters
  [api-key v-code]
  (api-call "account/Characters" api-key v-code))

(defn get-call-list []
  (api-call "api/CallList"))

(defn get-account-balance
  [api-key v-code]
  (api-call "char/AccountBalance" api-key v-code))

;; TODO introduce another flag to be able to request flat response xml.
(defn get-asset-list
  [api-key v-code char-id]
  (api-call "char/AssetList" api-key v-code char-id))

(defn get-char-bookmarks
  [api-key v-code]
  (api-call "char/Bookmarks" api-key v-code))

(defn get-char-chat-channels
  [api-key v-code]
  (api-call "char/ChatChannels" api-key v-code))

(defn get-char-contact-list
  [api-key v-code char-id]
  (api-call "char/ContactList" api-key v-code char-id))

;; TODO Introduce &marketOrder=12321 flag.
(defn get-market-orders
  [api-key v-code]
  (api-call "char/MarketOrders" api-key v-code))

(defn get-char-contact-notifications
  [api-key v-code char-id]
  (api-call "char/ContactNotifications" api-key v-code char-id))

;; Corp Endpoints.
;; ----------------------------------------------------------------------------

(defn get-corp-account-balance
  [api-key v-code char-id]
  (api-call "corp/AccountBalance" api-key v-code char-id))

;; TODO Add the rest of endpoints, I guess.

;; Global endpoints
;; ----------------------------------------------------------------------------

(defn get-sov-map
  "Grbas and returns the giant XML abomination known as the soverignty
  map. Deal with ti at your own peril. At least it's cached for you.
  And it's a clojure map now. Should make it somehwat easier to deal with."
  []
  (api-call "Map/Sovereignty"))

(defn get-server-status []
  (api-call "Server/ServerStatus"))

