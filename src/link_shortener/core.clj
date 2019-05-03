(ns link-shortener.core
  (:require [org.httpkit.server :as s]
            [clojure.test :refer [deftest testing is are]]
            [ring.mock.request :as mock]
            [ring.middleware.json :refer [wrap-json-response]]
            [link-shortener.storage :refer [Storage is-valid-storage]]
            [ring.util.response :as res]
            [compojure.core :refer [defroutes GET POST]]
            [ring.middleware.params :refer [wrap-params]]))

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

(defn hello [name]
  {:status 200
   :body   (format "Hello %s" name)})

(defn get-link
  [stg id]
  (if-let [url (get-link stg id)]
    (res/redirect url)
    (res/not-found "Sorry, that link doesn't exist.")))

(defn create-link
  [stg id {url :body}]
  (if (create-link stg id url)
    (res/response (str "/links/" id))
    (-> (format "The id %s is already in use." id)
        res/response
        (res/status 422))))

(defn update-link
  [stg id {url :body}]
  (if (update-link stg id url)
    (res/response (str "/links/" id))
    (res/not-found (format "There is no link with the id %s." id))))

(defn delete-link
  [stg id]
  (delete-link stg id)
  (-> (res/response "")
      (res/status 204)))

(defn list-links
  [stg]
  (wrap-json-response
    (fn []
      (res/response (list-links stg)))))

(defroutes app
           (GET "/hello/:name" [name] (hello name))
           (POST "/links" {{surname "surname"} :params} (do (println surname) {:body (str "Hi :)" surname)})))

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
(reset! server (s/run-server (-> app
                                 wrap-params)
                             {:port 8080}))