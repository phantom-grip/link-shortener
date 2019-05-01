(ns link-shortener.core
  (:require [org.httpkit.server :as s]
            [ataraxy.core :as core]
            [ataraxy.response :as response]))

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

(stop-server)
(reset! server (s/run-server app {:port 8080}))