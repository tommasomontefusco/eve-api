(ns eve-api.core-test
  (:require [clojure.test :refer :all]
            [eve-api.core :refer :all]))

(deftest sanity
  (testing "CTHULHU FTAGHN?"
    (is (= 1 1))))
