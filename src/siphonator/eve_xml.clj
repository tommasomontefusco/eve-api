(ns siphonator.eve-xml
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clj-http.client :as client]
            [clojure.core.memoize :as memo]
            [clj-time.core :as joda-time]
            [clj-time.format :as joda-format])
  (:import (java.io ByteArrayInputStream)))

;; I don't really need to test this, do I?
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
  (str base-request "?characterID=" character-id))

(defn make-request-url
  "Another simple helper function to create the basic request url, eg
  'eve/assets'"
  [xml-api]
  (str "https://api.eveonline.com/" xml-api ".xml.aspx"))

(defn create-api-authenticated-call
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

(defn raw-http-get
  "Uses clj-http to send a GET request to the URL. Header-map optional,
  send with :headers. If you do not include headers, a default header
  mapping will be used."
  [request-url & {headers :headers}]
  (if (nil? headers)
    (client/get request-url (create-default-header-map))
    (client/get request-url headers)))

;; TODO Implement cached API call with (memoize foo) and an atom to store the
;; expiration in per API function, then compare and do stuff on call. Clear
;; expired caches with (memo clear args). Specifying args only clears memoisation
;; for current arg vector, making it ideal for clearing individual calls. :3

(def cached-raw-api-call (memoize raw-http-get))
(def api-expiration-cache (atom {}))

(defn update-cache
  "updates the `api-exiration-cache` with a new value for any given request.
  Will pick out the value from a full request, then re-emit that map."
  [request]
  (throw (IllegalStateException. "Not implemented properly yet.")))

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
      (do (memo/memo-clear! cached-raw-api-call request-url)
          (cached-raw-api-call request-url headers))
      (cached-raw-api-call request-url headers))))


;; high-level interface, the friendly part.

(defn get-asset-list
  "Grabs the corp-asset list for any given api key, if available. If not, an
  error will be thrown."
  [api-code v-key]
  (throw (IllegalStateException. "Function not properly implemented yet.")))
