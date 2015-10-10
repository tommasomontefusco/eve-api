(ns siphonator.eve-xml-test
  (:require [clojure.test :refer :all]
            [siphonator.eve-xml :refer :all]))

(deftest append-api-string-test
  (testing "Appending an API key-value pair to an url"
    (is (=
         "foo?keyID=bar&vCode=baz"
         (append-api-string "foo" "bar" "baz")))
    (is (=
         "base-request?keyID=100&vCode=1337"
         (append-api-string "base-request" "100" "1337")))))
