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

(comment (defn create-app []
           (let [stg (in-memory-storage)]
             (routes
               (POST "/links" {{id "id" url "url"} :params} (create-link stg id url))
               (GET "/links/:id" [id] (get-link stg id))
               (PUT "/links/:id" [id :as {{url "url"} :params}] (update-link stg id url))
               (DELETE "/links/:id" [id] (delete-link stg id))
               (GET "/links" [] (-> (list-links stg)
                                    wrap-json-response))
               (route/not-found "Not Found")))))


(s/def ::url (s/and spec/string? is-valid-url?))
(s/def ::id spec/string?)
(s/def ::success-link spec/string?)
(s/def ::error-explanation spec/string?)
(s/def ::links spec/int?)

(def stg (in-memory-storage))

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
        (resource {:coercion :spec
                   :get      {:handler   (fn [_]
                                           (println "OK")
                                           (ok id))}}))
      (resource
        {:coercion :spec
         :post     {:parameters {:form-params (s/keys :req-un [::id ::url])}
                    :responses  {200 {:schema ::success-link}
                                 404 {:schema ::error-explanation}}
                    :handler    (fn [params]
                                  (let [{{:keys [id url]} :params} params
                                        _ (clojure.pprint/pprint params)]
                                    (handlers/create-link stg id url)))}
         :get      {:handler   (fn [_]
                                (handlers/list-links stg))}}))))

(def app* (wrap-json-response app))

(comment (def old-app
           (api
             {:coercion :spec
              :swagger
                        {:ui   "/"
                         :spec "/swagger.json"
                         :data {:info {:title       "Link shortener"
                                       :description "Compojure Api example"}
                                :tags [{:name "api", :description "some apis"}]}}}

             (context "/math/:a" []
               :path-params [a :- ::a]

               (sweet/POST "/plus" []
                 :query-params [b :- ::b, {c :- ::c 0}]
                 :body [numbers (s/keys :req-un [::d])]
                 :return (s/keys :req-un [::total])
                 (ok {:total (+ a b c (:d numbers))})))

             (context "/data-math" []
               (resource
                 ;; to make coercion explicit
                 {:coercion :spec
                  :get      {:parameters {:query-params (s/keys :req-un [::x ::y])}
                             :responses  {200 {:schema ::total-body}}
                             :handler    (fn [{{:keys [x y]} :query-params}]
                                           (ok {:total (+ x y)}))}})))))

(defn get-resp [req]
  (-> (app req)
      :body
      slurp
      (cheshire/parse-string true)))

(let [req1 (-> (mock/request :post "/links")
               (mock/body {:id "google" :url "http://www.google.com"})
               (mock/content-type "application/x-www-form-urlencoded"))
      req2 (-> (mock/request :get "/links"))
      req3 (-> (mock/request :get "/links/123"))]
  (do
    (println (get-resp req1))
    (println (get-resp req2))
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



