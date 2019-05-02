(ns link-shortener.core
  (:require [org.httpkit.server :as s]
            [ataraxy.core :as core]
            [ataraxy.response :as response]
            [clojure.test :refer [deftest testing is are]]
            [ring.mock.request :as mock]))

(defonce server (atom nil))

(defn hello [{[_ name] :ataraxy/result}]
  [::response/ok (str "Hello " name)])

(def app
  (core/handler
    {:routes   '{[:get "/hello/" name] [:hello name]}
     :handlers {:hello hello}}))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(deftest test-app
  (testing "hello route"
    (are [f res] (= (f (app (mock/request :get "/hello/yan")))
                    res)
                 :status 200
                 :body "Hello yan")))

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

(defn create-link*
  [!stg id url]
  (when-not (contains? @!stg id)
    (swap! !stg assoc id url)
    id))

(defn get-link*
  [!stg id]
  (get @!stg id))

(defn update-link*
  [!stg id new-url]
  (when (contains? @!stg id)
    (swap! !stg assoc id new-url)
    id))

(defn delete-link*
  [!stg id]
  (swap! !stg dissoc id)
  nil)

(defn list-links*
  [!stg]
  @!stg)

(defn in-memory-storage
  []
  (let [!stg (atom {})]
    (reify Storage
      (create-link [_ id url] (create-link* !stg id url))
      (get-link [_ id] (get-link* !stg id))
      (update-link [_ id url] (update-link* !stg id url))
      (delete-link [_ id] (delete-link* !stg id))
      (list-links [_] (list-links* !stg)))))

(deftest in-memory-storage-test
  (let [stg (in-memory-storage)]
    (is-valid-storage stg)))

(stop-server)
(reset! server (s/run-server app {:port 8080}))