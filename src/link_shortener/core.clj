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

(s/def ::a spec/int?)
(s/def ::b spec/int?)
(s/def ::c spec/int?)
(s/def ::d spec/int?)
(s/def ::total spec/int?)
(s/def ::total-body (s/keys ::req-un [::total]))

(s/def ::x spec/int?)
(s/def ::y spec/int?)

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

(println (-> {:request-method :get
              :uri            "/data-math"
              :query-params   {:x "1", :y "2"}}
             (app)
             :body
             (slurp)
             (cheshire/parse-string true)))



