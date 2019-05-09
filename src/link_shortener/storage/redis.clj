(ns link-shortener.storage.redis
  [:require [link-shortener.storage :refer [Storage]]
            [taoensso.carmine :as car :refer (wcar)]])

(defonce server1-conn {:pool {} :spec {:uri "redis://h:pa5e05adb66117c0dbf17f0a7b43ee903a64e0abe83926c903d81315fdd64ef94@ec2-34-252-60-59.eu-west-1.compute.amazonaws.com:26419"}})
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
