(ns link-shortener.core
  (:require [org.httpkit.server :as server]
            [clojure.test :refer [deftest testing is are]]
            [ring.mock.request :as mock]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :as res]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [environ.core :refer [env]]
            [clojure.spec.alpha :as s]
            [spec-tools.spec :as spec]
            [compojure.api.sweet :refer [api context resource] :as sweet]
            [ring.util.http-response :refer [ok]]
            [cheshire.core :as cheshire]
    ;
            [link-shortener.storage :as st]
            [link-shortener.storage.in-memory :refer [in-memory-storage]]
            [link-shortener.validations :refer [is-valid-url? shorter-than]]
            [link-shortener.handlers :as handlers])
  (:gen-class))

(s/def ::url (s/and spec/string? is-valid-url?))
(s/def ::id spec/string?)
(s/def ::success-link spec/string?)
(s/def ::error-explanation spec/string?)
(s/def ::links spec/int?)
(s/def ::number spec/int?)

(def stg (in-memory-storage))

;; TODO not found
;; TODO more security with specs

(def app
  (api
    {:coercion :spec
     :swagger
               {:ui   "/"
                :spec "/swagger.json"
                :data {:info {:title       "Link shortener"
                              :description "Compojure Api example"}
                       :tags [{:name "api", :description "some apis"}]}}}
    (context "/links" []
      (context "/:id" [id]
        :path-params [id :- ::id]
        (resource {:coercion :spec
                   :get      {:handler (fn [param]
                                         (clojure.pprint/pprint param)
                                         (ok {:id id
                                              :text "get links/:id"}))}
                   :put {:parameters {:params (s/keys :req-un [::id ::url])}
                         :handler (fn [{{:keys [id url]} :params}]
                                    (ok {:id id
                                         :text "put links/:id"}))}
                   :delete {:parameters {:params (s/keys :req-un [::id])}
                            :handler (fn [_]
                                       (ok {:id id
                                            :text "get links/:id"}))}}))
      (resource
        {:coercion :spec
         :post     {:parameters {:form-params (s/keys :req-un [::id ::url])}
                    :responses  {200 {:schema ::success-link}
                                 404 {:schema ::error-explanation}}
                    :handler    (fn [params]
                                  (let [{{:keys [id url]} :params} params]
                                    (handlers/create-link stg id url)))}
         :get      {:handler (fn [_]
                               (handlers/list-links stg))}}))))

(defn get-resp [req]
  (-> (app req)
      :body
      slurp
      (cheshire/parse-string true)))

(defn get-resp1 [req]
  (-> (app req)
      :body))

(let [req1 (-> (mock/request :post "/links")
               (mock/body {:id "google" :url "http://www.google.com"})
               (mock/content-type "application/x-www-form-urlencoded"))
      req2 (-> (mock/request :get "/links"))
      req3 (-> (mock/request :get "/links/google"))]
  (do
    (println (get-resp req1))
    ;(println (get-resp req2))
    (println (get-resp req3))))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server [port]
  (server/run-server app
                     {:port port}))

(stop-server)
(reset! server (start-server 8080))

(defn -main
  [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (start-server port)))



