(ns link-shortener.core
  (:require [org.httpkit.server :as server]
            [clojure.test :refer [deftest testing is are]]
            [ring.mock.request :as mock]
            [ring.middleware.json :refer [wrap-json-response]]
            [link-shortener.storage :as st]
            [ring.util.response :as res]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [link-shortener.storage.in-memory :refer [in-memory-storage]]
            [link-shortener.validations :refer [is-valid-url?]]
            [environ.core :refer [env]]
            [clojure.spec.alpha :as s]
            [spec-tools.spec :as spec]
            [compojure.api.sweet :refer [api context resource] :as sweet]
            [ring.util.http-response :refer [ok]]
            [cheshire.core :as cheshire])
  (:gen-class))

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

(s/def ::a spec/int?)
(s/def ::b spec/int?)
(s/def ::c spec/int?)
(s/def ::d spec/int?)
(s/def ::total spec/int?)
(s/def ::total-body (s/keys ::req-un [::total]))

(s/def ::x spec/int?)
(s/def ::y spec/int?)

(s/def ::url spec/int?)
(s/def ::id spec/int?)
(s/def ::links spec/int?)

(def app
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
                                  (ok {:total (+ x y)}))}}))))

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

(println (-> {:request-method :get
              :uri            "/data-math"
              :query-params   {:x "1", :y "2"}}
             (app)
             :body
             (slurp)
             (cheshire/parse-string true)))



