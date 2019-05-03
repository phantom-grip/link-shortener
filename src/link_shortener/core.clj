(ns link-shortener.core
  (:require [org.httpkit.server :as s]
            [ataraxy.core :as core]
            [ataraxy.response :as response]
            [clojure.test :refer [deftest testing is are]]
            [ring.mock.request :as mock]
            [ring.middleware.json :refer [wrap-json-response]]
            [link-shortener.storage :refer [Storage is-valid-storage]]))

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

(defonce server (atom nil))

(defn hello [{[_ name] :ataraxy/result}]
  [::response/ok (str "Hello " name)])

(defn get-link
  [stg id]
  (if-let [url (get-link stg id)]
    [::response/found url]
    [::response/not-found "Sorry, that link doesn't exist"]))

(defn create-link
  [stg id {url :body}]
  (if (create-link stg id url)
    [::response/ok (str "/links/" id)]
    [::response/bad-request (format "The id %s is already in use." id)]))

(defn update-link
  [stg id {url :body}]
  (if (update-link stg id url)
    [::response/ok (str "/links/" id)]
    [::response/not-found "There is no link with the id %s." id]))

(defn delete-link
  [stg id]
  (delete-link stg id)
  [::response/no-content])

(defn list-links
  [stg]
  (wrap-json-response
    (fn []
      [::response/ok (list-links stg)])))

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

(stop-server)
(reset! server (s/run-server app {:port 8080}))