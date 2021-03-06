(ns chatapp.core
  (:require [chatapp.chat :as chat]
            [clojure.tools.logging :as log]
            [compojure.core :as compojure :refer [GET defroutes]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [org.httpkit.server :as httpkit]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :as response]
            [selmer.parser :as selmer])
  (:gen-class))

(defroutes app-routes
  (GET "/" [] (response/resource-response "index.html" {:root "public"}))
  (route/files "/bower_components" {:root "./resources/bower_components"})
  (route/resources "/")
  (compojure/context "/chat" [] chat/chat-routes)
  (route/not-found "<p>Page not found.</p>"))

(defn- wrap-request-logging
  [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [resp (handler req)]
      (log/info
       (name request-method)
       (:status resp)
       (if-let [qs (:query-string req)]
         (str uri "?" qs) uri))
      resp)))

(def app (-> app-routes
             #_wrap-print-request
             wrap-request-logging
             wrap-cookies
             wrap-session
             wrap-keyword-params
             wrap-params
             #_(cond-> (env :dev) wrap-error-page)))

(defn -main
  [& args]
  (if (env :dev) (do
                   (log/warn "DEVELOPMENT ENVIRONMENT")
                   (selmer/cache-off!)))
  (log/info "Starting server...")
  (let [handler (if (env :dev)
                  (wrap-reload #'app)
                  app)]
    (httpkit/run-server handler {:port 8080}))
  (log/info "Server started."))
