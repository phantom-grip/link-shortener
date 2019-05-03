(ns link-shortener.validations
  (:import (org.apache.commons.validator.routines UrlValidator)))

(defn is-valid-url? [url]
  (let [validator (UrlValidator.)]
    (try
      (.isValid validator url)
      (catch Exception e false))))