(ns eve-api.util
  (:gen-class))


;; Header stuff, for HTTP calls.
;; ===========================================================================

(def default-headers {:client-params
                       {"http.useragent" "github.com/az4reus/eve-api"}})

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
