(ns link-shortener.handlers
  (:require [link-shortener.storage :as st]
            [ring.util.response :as res])
  (:gen-class))

(defn get-link
  [stg id]
  (if-let [url (st/get-link stg id)]
    (res/redirect url)
    (res/not-found "Sorry, that link doesn't exist.")))

(defn create-link
  [stg id url]
  (if (st/create-link stg id url)
    (res/response (str "/links/" id))
    (-> (format "The id %s is already in use." id)
        res/response
        (res/status 422))))

(defn update-link
  [stg id url]
  (if (st/update-link stg id url)
    (res/response (str "/links/" id))
    (res/not-found (format "There is no link with the id %s." id))))

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

