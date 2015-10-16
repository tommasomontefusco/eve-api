(ns siphonator.eve-xml
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clj-http.client :as client]
            [clojure.core.memoize :as memo]
            [clj-time.core :as joda-time]
            [clj-time.format :as joda-format])
  (:import (java.io ByteArrayInputStream)))

(defn create-default-header-map
  "Creates a default header mapping to us with `raw-http-get`"
  []
  {:client-params {"http.useragent" "eve-xml library for Clojure. Cobbled
  together by Az, email: az4reus@gmail.com. Come say hi :3"}})

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

(defn update-cache!
  "updates the `api-exiration-cache` with a new value for any given request.
  Will pick out the value from a full request, then re-emit that map."
  [request result]
  ((let [cache @api-expiration-cache]
     (->> (assoc cache request result)
          (swap! api-expiration-cache)))))

(defn- raw-http-get
  "Uses clj-http to send a GET request to the URL. Header-map optional,
  send with :headers. If you do not include headers, a default header
  mapping will be used. Updates expiration dates cache, but is not memoized
  itself. This seems counterintuitive until you realise that you shouild
  not be using this method, hence it being private. "
  [request-url & {headers :headers}]
  (if (nil? headers)
    (->> (client/get request-url (create-default-header-map))
         (update-cache! request-url))
    (->> (client/get request-url headers)
         (update-cache! request-url))))

(def memoized-raw-http-call (memoize raw-http-get))

(defn get-cached-time
  [request-url]
  (get api-expiration-cache request-url))

(defn cached-until
  "simply returns the cached value as a joda-time/Interval to joda-time/now."
  [request-url]
  (joda-time/minus (get-cached-time request-url) (joda-time/now)))

(defn is-expired?
  "compares local time to the expiration time given in the expiration cache"
  [previous-date]
  (let [now joda-time/now]
    (joda-time/after? now previous-date)))

;; TODO add caching to returning calls from expiration date in the XML
(defn api-request
  [request-url & {headers :headers}]
  (let [cache @api-expiration-cache]
    (if (is-expired? (get cache request-url))
      (do (memo/memo-clear! memoized-raw-http-call request-url)
          (memoized-raw-http-call request-url headers))
      (do (memoized-raw-http-call request-url headers)))))


;; high-level interface, the friendly part.

(defn get-asset-list
  "Grabs the corp-asset list for any given api key, if available. If not, an
  error will be thrown."
  [api-code v-key]
  (throw (IllegalStateException. "Function not properly implemented yet.")))

(defn get-sov-map
  "Grbas and returns the giant XML abomination known as the soverignty
  map. Deal with ti at your own peril. At least it's cached for you.
  And it's a clojure map. Should make it somehwat easier to deal with."
  (-> (make-request-url "eve/sovereignty")
      (api-request)
      (get :content)))
