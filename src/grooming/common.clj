(ns grooming.common
  (:require [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [ring.util.response :refer [response]]
            [selmer.parser :as selmer]))

(def json-header {"Content-Type" "application/json; charset=utf-8"})
(def text-header {"Content-Type" "text/plain; charset=utf-8"})
(def html-header {"Content-Type" "text/html; charset=utf-8"})

(defn render-template
  [tname args]
  (selmer/render-file (str "templates/" tname) args {:tag-open \[, :tag-close \]}))

(defn json-response
  [json-data]
  (-> (response (json/write-str json-data))
      (assoc :headers json-header)))

(defn wrap-print-request
  [handler]
  (fn [request]
    (pprint request)
    (handler request)))
