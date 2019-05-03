(ns link-shortener.storage)

(defprotocol Storage
  (create-link [this id url])
  (get-link [this id])
  (update-link [this id new-url])
  (delete-link [this id])
  (list-links [this]))

