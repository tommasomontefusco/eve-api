(ns siphonator.eve-xml
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clj-http.client :as client])
  (:import (java.io ByteArrayInputStream)))

(defn xml-to-map [s]
  (zip/xml-zip
    (xml/parse (ByteArrayInputStream. (.getBytes s)))))

(defn append-api-string
  "Simple helper method to append the API verification string at the end of
  an XML-API call when needed."
  [base-request api-number v-code]
  (str base-request "?keyID=" api-number "&vCode=" v-code))

(defn make-request-url
  "Another simple helper function to create the basic request url"
  [xml-api]
  (str "https://api.eveonline.com/" xml-api ".xml.aspx"))

(defn raw-http-get
  "Uses clj-http to send a GET request to the URL. Header-map optional,
  send with :headers."
;; TODO Implement cached API call with (memoize foo) and an atom to store the
;; expiration in per API function, then compare and do stuff on call. Clear
;; expired caches with (memo clear args). Specifying args only clears memoisation
;; for current arg vector, making it ideal for clearing individual calls. :3

(def cached-raw-api-call (memoize raw-http-get))
(def api-cache (atom {}))

(defn api-request
  [request-url & {headers :headers}]
  (let [cache @api-cache]
    ))


;; high-level interface, the friendly part.

(defn get-asset-list
  "Grabs the corp-asset list for any given api key, if available. If not, an
  error will be thrown."
  [api-code v-key]
  (throw (IllegalStateException. "Function not properly implemented yet.")))
