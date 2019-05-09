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


(deftest create-link-test
  (let [url "http://www.example.com"
        stg (in-memory-storage)
        id "test"]
    (testing "When the ID doesn't exist"
      (let [response (handlers/create-link stg id url)]
        (testing "the response status is 200"
          (is (= (:status response) 200)))
        (testing "with the expected body"
          (is (= (:body response) "/links/test")))
        (testing "and the link is actual exists"
          (is (= url (st/get-link stg id))))))

    (testing "When the ID does exists"
      (let [response (handlers/create-link stg id url)]
        (testing "the response status is 422"
          (is (= (:status response) 422)))))))


(deftest update-link-test
  (let [url "http://www.example.com"
        stg (in-memory-storage)
        id "test"]
    (testing "When the ID doesn't exist"
      (let [response (handlers/update-link stg id url)]
        (testing "the response status is 404"
          (is (= (:status response) 404)))))

    (testing "When the ID does exists"
      (let [new-url "http://www.new-example.com"]
        (st/create-link stg id url)
        (let [response (handlers/update-link stg id new-url)]
          (testing "the response status is 200"
            (is (= (:status response) 200)))
          (testing "With the expected body"
            (is (= (:body response) "/links/test")))
          (testing "and the link actually exists"
            (is (= new-url (st/get-link stg id)))))))))



(deftest delete-link-test
  (let [url "http://www.example.com"
        stg (in-memory-storage)
        id "test"]
    (testing "When the link exists"
      (st/create-link stg id url)
      (let [response (handlers/delete-link stg id)]
        (testing "the response status is 204"
          (is (= (:status response) 204)))
        (testing "and the link is actually deleted"
          (is (nil? (st/get-link stg id))))))

    (testing "When the ID does exists"
      (let [new-id "new-test"
            response (handlers/delete-link stg new-id)]
        (testing "the response status is still 204"
          (is (= (:status response) 204)))))))