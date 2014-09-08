(ns grooming.core
  (:gen-class)
  (:use [compojure.core :only [defroutes GET POST]]
        [clojure.pprint :only [pprint]])
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

(defroutes app-routes
  (GET "/" [] (response/resource-response "index.html" {:root "public"}))
  (GET "/com-channel" [] com-channel)
  (GET "/selmer-test" [] (selmer/render-file "public/test.html" {:foobar "SELMER!!"}))
  (POST "/session" [user-name] (str "Hello " user-name "!"))
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
