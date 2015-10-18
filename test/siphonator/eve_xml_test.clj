(ns siphonator.eve-xml-test
  (:require [clojure.test :refer :all]
            [siphonator.eve-xml :refer :all]))

(deftest making-urls-test
  (testing "Making a base Request Url. Just for completeness' sake."
    (is (=
          "https://api.eveonline.com/test/request.xml.aspx"
          (make-request-url "test/request"))))
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
          (create-character-authenticated-url "test/request" 1 2 3)))))

(deftest xml-to-map-test
  (testing "does it parse XML as expected?"
    (is (=
          [{:tag :parent,
            :attrs nil,
            :content
            [{:tag :child,
              :attrs nil,
              :content
              [{:tag :row, :attrs {:name "test attr 1"}, :content nil}
               {:tag :row, :attrs {:name "test attr 2"}, :content nil}]}]}
           nil]
          (xml-to-map
            "<parent>
              <child>
                <row name='test attr 1' />
                <row name='test attr 2' />
              </child>
            </parent>")))))

(deftest expiration-cache-test
  (testing "Does the cache get updated correctly if we throw values at it?"
    ))