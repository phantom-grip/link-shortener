(ns link-shortener.handlers-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [link-shortener.handlers :as handlers]
            [link-shortener.storage.in-memory :refer [in-memory-storage]]
            [link-shortener.storage :as st]))


(deftest get-link-test
  (let [id "test"
        url "http://www.example.com"
        stg (in-memory-storage)]
    (st/create-link stg id url)

    (testing "When the ID exists"
      (let [response (handlers/get-link stg id)]
        (testing "the esponse status is 302"
          (is (= (:status response) 302))
          (testing "and the expected URL is in the Location header"
            (is (= url (get-in response [:headers "Location"])))))))

    (testing "When the ID is not found"
      (let [response (handlers/get-link stg "not-found")]
        (testing "the result is 404"
          (is (= (:status response) 404)))))
    ))


