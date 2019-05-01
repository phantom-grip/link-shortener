(ns link-shortener.core
  (:require [org.httpkit.server :as s]
            [ataraxy.core :as core]
            [ataraxy.response :as response]
            [clojure.test :refer [deftest testing is]]
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
    (let [response (app (mock/request :get "/hello/yan"))]
      (is (= (response :status) 200))
      (is (= (response :body) "Hello yan")))))

(stop-server)
(reset! server (s/run-server app {:port 8080}))