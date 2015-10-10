(ns siphonator.eve-xml
  (:gen-class)
  (:require [clj-http.client :as client]))

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
  [request-url & {headers :headers}]
  (client/get request-url headers))

(defn get-asset-list
  "Grabs the corp-asset list for any given api key, if available. If not, an
  error will be thrown."
  [api-code v-key]
  (throw (IllegalStateException. "Function not properly implemented yet.")))
