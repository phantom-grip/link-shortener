(ns link-shortener.core
  (:require [org.httpkit.server :as s]))

(defonce server (atom nil))

(defn app [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello Clojure"})

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(stop-server)
(reset! server (s/run-server app {:port 8080}))