(ns eve-api.eve-xml-test
  (:require [clojure.test :refer :all]
            [eve-api.xml :refer :all]))

(deftest xml-to-map-test
  (testing "does it parse XML as expected?"
    (is (=
          [{:tag   :parent,
            :attrs nil,
            :content
                   [{:tag   :child,
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
    (cache-timestamp! "request" "timestamp")
    (is
      (= "timestamp"
         (get @api-expiration-cache "request")))))

(deftest extracting-xml-timestamp
  (let [test-xml
        "<root>
          <serverTime> foo bar </serverTime>
          <result>
            <foo> skjdfoas </foo>
          </result>
          <cachedUntil>
            2015-10-18 14:03:05
          </cachedUntil>
        </root>"]
    (testing "Does it parse the timestamp correctly?"
      (is (=
            "2015-10-18 14:03:05"
            (extract-xml-timestamp test-xml))))))

(deftest expiration-test
  (testing "Does time get parsed correctly from the stored timestamp?")
  (is (= true
         (is-expired? "2015-10-18 14:03:05"))))

