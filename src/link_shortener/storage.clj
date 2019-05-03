(ns link-shortener.storage
  (:require [clojure.test :refer [deftest testing is are]]))

(defprotocol Storage
  (create-link [this id url])
  (get-link [this id])
  (update-link [this id new-url])
  (delete-link [this id])
  (list-links [this]))

(defn is-valid-storage
  [stg]
  (let [url "http://example.com"
        id "book"]
    (testing "can store and retrieve a link"
      (testing "create-link returns the id"
        (is (= id (create-link stg id url)))

        (testing "and it won't overwrite an existing id"
          (is (nil? (create-link stg id "bogus")))
          (is (= url (get-link stg id))))))

    (testing "can update a link"
      (let [new-url "http://new.example.com"]
        (update-link stg id new-url)
        (is (= new-url (get-link stg id)))))

    (testing "can delete a link"
      (delete-link stg id)
      (is (nil? (get-link stg id))))

    (testing "can list all links"
      (let [id-urls {"a" "http://example.com/a"
                     "b" "http://example.com/b"
                     "c" "http://example.com/c"}
            ids (doseq [[id url] id-urls]
                  (create-link stg id url))
            links (list-links stg)]

        (testing "in a map"
          (is (map? links))

          (testing "equal to the links we created"
            (is (= id-urls links))))))))