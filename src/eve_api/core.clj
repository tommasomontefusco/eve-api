(ns eve-api.core
  (:gen-class)
  (:require [eve-api.xml :as ex]))

(defn make-fuelmap
  "Takes a container ID and the two subsequent seqs, then turns them into
  a clojure map"
  [container-id items quantities]
  (if-not (= (count items) (count quantities))
    (throw (IllegalArgumentException. "Two given lists are not equally long.")))
  {:container  container-id
   :items      items
   :quantities quantities})

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [sov-map       (future (ex/get-sov-map))
        server-status (future (ex/get-server-status))
        call-list     (future (ex/get-call-list))]
    (println @sov-map)
    (println @call-list)
    (println @server-status)
    (println @ex/api-expiration-cache)))
