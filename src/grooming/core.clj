(ns grooming.core
  (:gen-class)
  (:use [compojure.core :only [defroutes GET POST]]
        [clojure.pprint :only [pprint]])
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.reload :as reload]
            [ring.util.response :as response]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.tools.logging :as log]))

(defn home-page
  [req]
  "Welcome!")

(defroutes app-routes
  (GET "/" [] (response/resource-response "index.html" {:root "public"}))
  (route/resources "")
  (route/not-found "<p>Page not found.</p>"))

(defn in-dev? [& args] true)

(def app (-> app-routes
             handler/site))

(defn -main
  [& args]
  (let [request-handler (if (in-dev? args)
                          (reload/wrap-reload #'app)
                          app)]
    (log/info "Starting server...")
    (httpkit/run-server request-handler {:port 8080})
    (log/info "Server started.")))
