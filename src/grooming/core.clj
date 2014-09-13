(ns grooming.core
  (:gen-class)
  (:use [compojure.core :only [defroutes GET POST]]
        [clojure.pprint :only [pprint]]
        [environ.core :only [env]]
        [selmer.middleware :only [wrap-error-page]])
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.reload :as reload]
            [ring.util.response :as response]
            (compojure [handler :as handler]
                       [route :as route])
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [selmer.parser :as selmer]))

(def json-header {"Content-Type" "application/json; charset=utf-8"})
(def text-header {"Content-Type" "text/plain; charset=utf-8"})
(def html-header {"Content-Type" "text/html; charset=utf-8"})

(def open-channels (atom {}))

(def grooming (atom []))

(defn broadcast
  [msg]
  (doseq [channel (keys @open-channels)]
    (httpkit/send! channel {:status 200
                            :headers json-header
                            :body msg})))

(defn com-channel [request]
  (httpkit/with-channel request channel
    (swap! open-channels assoc channel request)
    (log/info "Channel opened: " request)
    (httpkit/on-close channel (fn [status]
                                (log/info "Channel closed: " status)
                                (swap! open-channels dissoc channel)))
    (httpkit/on-receive channel (fn [data]
                                  (broadcast data)))))

(defn session-handler
  [request]
  #_(pprint request)
  (let [session (:session (assoc-in request [:session :name] "foo"))]
    {:status 200
     :headers html-header
     :body (str "Counter: " (:counter session))
     :session session}))

(defn render-template
  [tname args]
  (selmer/render-file (str "templates/" tname) args {:tag-open \[, :tag-close \]}))

(defroutes app-routes
  (GET "/" [] (render-template "login.html" {}))
  (GET "/com-channel" [] com-channel)
  (GET "/selmer-test" [] (selmer/render-file "public/test.html" {:foobar "SELMER!!"}))
  (POST "/session" [user-name] (str "Hello " user-name "!"))
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
    (println request)
    (handler request)))

(def app (-> app-routes
             handler/site
             wrap-request-logging
             reload/wrap-reload
             #_wrap-print-request
             (cond-> (env :dev) wrap-error-page)))

(defn -main
  [& args]
  (if (env :dev) (log/warn "DEVELOPMENT ENVIRONMENT"))
  (log/info "Starting server...")
  (let [handler (if (env :dev)
                  (reload/wrap-reload #'app)
                  app)]
    (httpkit/run-server handler {:port 8080}))
  (log/info "Server started."))
