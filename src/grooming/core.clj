(ns grooming.core
  #_(:gen-class)
  (:use [grooming.common]
        [compojure.core :only [defroutes GET POST]]
        [clojure.pprint :only [pprint]]
        [environ.core :only [env]]
        [selmer.middleware :only [wrap-error-page]])
  (:require [org.httpkit.server :as httpkit]
            (ring.middleware [reload :refer [wrap-reload]]
                             [flash :refer [wrap-flash]]
                             [cookies :refer [wrap-cookies]]
                             [multipart-params :refer [wrap-multipart-params]]
                             [params :refer [wrap-params]]
                             [nested-params :refer [wrap-nested-params]]
                             [keyword-params :refer [wrap-keyword-params]])
            [ring.util.response :as response]
            (compojure [core :as compojure]
                       [handler :as handler]
                       [route :as route])
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [selmer.parser :as selmer]
            [grooming.chat :as chat]
            [sandbar.stateful-session :refer [wrap-stateful-session]]))

(defn session-handler
  [request]
  #_(pprint request)
  (let [session (:session (assoc-in request [:session :name] "foo"))]
    {:status 200
     :headers html-header
     :body (str "Counter: " (:counter session))
     :session session}))

(defroutes app-routes
  (GET "/" [] (render-template "login.html" {}))
  (compojure/context "/chat" [] (chat/chat-routes))
  (route/resources "/static")
  (route/not-found "<p>Page not found.</p>"))

(defn in-dev? [& args] true)

(defn- wrap-request-logging
  [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [resp (handler req)]
      (log/info (name request-method) (:status resp)
            (if-let [qs (:query-string req)]
              (str uri "?" qs) uri))
      resp)))

(defn- wrap-print-request
  [handler]
  (fn [request]
    (pprint request)
    (handler request)))

(def app (-> app-routes
             wrap-stateful-session
             wrap-flash
             wrap-cookies
             wrap-multipart-params
             wrap-params
             wrap-nested-params
             wrap-keyword-params
             wrap-request-logging
             #_wrap-print-request
             (cond-> (env :dev) wrap-error-page)))

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
