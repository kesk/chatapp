(ns grooming.common
  (:require [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [ring.util.response :refer [response]]
            [selmer.parser :as selmer]))

(def json-header {"Content-Type" "application/json; charset=utf-8"})
(def text-header {"Content-Type" "text/plain; charset=utf-8"})
(def html-header {"Content-Type" "text/html; charset=utf-8"})

(defn render-template
  [tname args]
  (selmer/render-file (str "templates/" tname) args {:tag-open \[, :tag-close \]}))

(def json->edn (fn [s] (-> s (string/replace #"_" "-") keyword)))
(def edn->json (fn [k] (-> k name (string/replace #"-" "_"))))

(defn json-response
  [json-data]
  (-> (response (json/write-str json-data :key-fn edn->json))
      (assoc :headers json-header)))

(defn wrap-print-request
  [handler]
  (fn [request]
    (pprint request)
    (handler request)))

(defn- get-username
  [request]
    (get-in request [:session :username]))

(defn get-session-id
  [req]
  (get-in req [:cookies "ring-session" :value]))
