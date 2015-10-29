(ns eve-api.util-test
  (:require [clojure.test :refer :all]
            [eve-api.util :refer :all]))

(deftest making-urls-test
  (testing "Making a base Request Url. Just for completeness' sake."
    (is (=
          "https://api.eveonline.com/test/request.xml.aspx"
          (create-basic-request-url "test/request"))))
  (testing "Appending an API key-value pair to an url"
    (is (=
          "foo?keyID=bar&vCode=baz"
          (append-api-string "foo" "bar" "baz")))
    (is (=
          "test/request?keyID=100&vCode=1337"
          (append-api-string "test/request" "100" "1337"))))
  (testing "Appending the Character-string."
    (is (=
          "test?characterID=1337"
          (append-character-id "test" "1337"))))
  (testing "Testing authenticated call composition"
    (is (=
          "https://api.eveonline.com/test/request.xml.aspx?keyID=1&vCode=2"
          (create-authenticated-url "test/request" 1 2))))
  (testing "Testing character authenticated call composition"
    (is (=
          "https://api.eveonline.com/test/request.xml.aspx?keyID=1&vCode=2&characterID=3"
          (create-char-authenticated-url "test/request" 1 2 3)))))