(ns grooming.core
  #_(:gen-class)
  (:use [grooming.common]
        [compojure.core :only [defroutes GET POST]]
        [clojure.pprint :only [pprint]]
        [environ.core :only [env]]
        [selmer.middleware :only [wrap-error-page]])
  (:require [org.httpkit.server :as httpkit]
            (ring.middleware [reload :refer [wrap-reload]]
                             [params :refer [wrap-params]]
                             [keyword-params :refer [wrap-keyword-params]]
                             [session :refer [wrap-session]]
                             [cookies :refer [wrap-cookies]])
            [ring.util.response :as response]
            (compojure [core :as compojure]
                       [handler :as handler]
                       [route :as route])
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [selmer.parser :as selmer]
            [grooming.chat :as chat]))

(defn count-page-loads
  [{:keys [session]}]
  (let [count (:count session 0)
        session (assoc session :count (inc count))]
    (-> (response/response (str "You've accessed this page " count " times."))
        (assoc :session session))))

(defn- require-username
  "Route wrapper that checks for username in the session or in the
  request params. If a username is not present redirects to '/'."
  [handler]
  (fn [{:keys [session params] :as request}]
    (let [set-username #(assoc-in request [:session :username] %)]
      (cond
       (:username params)  (handler (set-username (:username params)))
       (:username session) (handler request)
       :else               (response/redirect "/")))))

(defroutes app-routes
  (GET "/" [] (render-template "login.html" {}))
  (compojure/context "/chat" [] (require-username chat/chat-routes))
  (GET "/count" request (count-page-loads request))
  (route/resources "/static")
  (route/not-found "<p>Page not found.</p>"))

(defn- wrap-request-logging
  [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [resp (handler req)]
      (log/info (name request-method) (:status resp)
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
