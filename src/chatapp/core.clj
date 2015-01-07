(ns chatapp.core
  (:require [chatapp.chat.chat-page :as chat]
            [chatapp.common :refer [render-template]]
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
            [selmer.parser :as selmer]))

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
  (route/resources "/static")
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
