(ns link-shortener.core
  (:require [org.httpkit.server :as s]
            [clojure.test :refer [deftest testing is are]]
            [ring.mock.request :as mock]
            [ring.middleware.json :refer [wrap-json-response]]
            [link-shortener.storage :as st]
            [ring.util.response :as res]
            [compojure.core :refer [routes GET POST PUT DELETE]]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [link-shortener.storage.in-memory :refer [in-memory-storage]]
            [link-shortener.validations :refer [is-valid-url?]]
            [environ.core :refer [env]])
  (:gen-class))

(defonce server (atom nil))

(defn get-link
  [stg id]
  (if-let [url (st/get-link stg id)]
    (res/redirect url)
    (res/not-found "Sorry, that link doesn't exist.")))

(defn create-link
  [stg id url]
  (cond
    (not (is-valid-url? url))
    (res/bad-request "Url is not valid")
    :else
    (if (st/create-link stg id url)
      (res/response (str "/links/" id))
      (-> (format "The id %s is already in use." id)
          res/response
          (res/status 422)))))

(defn update-link
  [stg id url]
  (cond
    (not (is-valid-url? url))
    (res/bad-request "Url is not valid")
    :else
    (if (st/update-link stg id url)
      (res/response (str "/links/" id))
      (res/not-found (format "There is no link with the id %s." id)))))

(defn delete-link
  [stg id]
  (st/delete-link stg id)
  (-> (res/response "")
      (res/status 204)))

(defn list-links
  [stg]
  (fn [_]
    (-> (st/list-links stg)
        res/response)))

(defn create-app []
  (let [stg (in-memory-storage)]
    (routes
      (POST "/links" {{id "id" url "url"} :params} (create-link stg id url))
      (GET "/links/:id" [id] (get-link stg id))
      (PUT "/links/:id" [id :as {{url "url"} :params}] (update-link stg id url))
      (DELETE "/links/:id" [id] (delete-link stg id))
      (GET "/links" [] (-> (list-links stg)
                           wrap-json-response))
      (route/not-found "Not Found"))))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server [port]
  (s/run-server (-> (create-app)
                    wrap-params)
                {:port port}))

(defn -main
  [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (start-server port)))