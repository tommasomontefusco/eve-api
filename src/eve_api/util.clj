(ns eve-api.util)

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
