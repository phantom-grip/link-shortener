(ns link-shortener.storage.redis
  [:require [link-shortener.storage :refer [Storage]]
            [taoensso.carmine :as car :refer (wcar)]
            [environ.core :refer [env]]])

(if-not (env :redis-url)
  (throw (Exception. "Specify redis url")))

(defonce server1-conn {:pool {} :spec {:uri (env :redis-url)}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(def HASH_NAME "links")

(defn create-link*
  [id url]
  (when (= 0 (wcar* (car/hexists HASH_NAME id)))
    (wcar* (car/hset HASH_NAME id url))
    id))

(defn get-link*
  [id]
  (wcar* (car/hget HASH_NAME id)))

(defn update-link*
  [id new-url]
  (when (= 1 (wcar* (car/hexists HASH_NAME id)))
    (wcar* (car/hset HASH_NAME id new-url))
    id))

(defn delete-link*
  [id]
  (wcar* (car/hdel HASH_NAME id))
  nil)

(defn list-links*
  []
  (->> (wcar* (car/hgetall HASH_NAME))
       (partition 2)
       (reduce (fn [res [k v]]
                 (assoc res k v))
               {})))

(defn redis-storage
  []
  (reify Storage
    (create-link [_ id url] (create-link* id url))
    (get-link [_ id] (get-link* id))
    (update-link [_ id url] (update-link* id url))
    (delete-link [_ id] (delete-link* id))
    (list-links [_] (list-links*))))
