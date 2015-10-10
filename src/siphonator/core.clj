(ns siphonator.core
  (:gen-class))

(defn api-string
  "takes details for an API string and makes into a suitable API querystring"
  [api vcode]
  (str "&api=" api "&vcode=" vcode))

(defn make-fuelmap
  "Takes a container ID and the two subsequent seqs, then turns them into
  a clojure map"
  [container-id items quantities]
  (if-not (= (count items) (count quantities))
    (throw (IllegalArgumentException. "Two given lists are not equally long.")))
  {:container container-id
   :items items
   :quantities quantities})

;; Main function. Please keep at the last spot <3

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (make-fuelmap 231 '(1234 4) '(1 2)))
  (println (make-fuelmap 1234 '(81, 291) '(11293, 12)))
  (println (make-fuelmap 8123 '(12) '(2))))
