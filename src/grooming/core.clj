(ns grooming.core
  (:gen-class)
  (:use [compojure.core :only [defroutes GET POST]]
        [clojure.pprint :only [pprint]])
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.reload :as reload]
            [ring.util.response :as response]
            (compojure [handler :as handler]
                       [route :as route])
            [clojure.tools.logging :as log]))

(def json-header {"Content-Type" "application/json; charset=utf-8"})
(def text-header {"Content-Type" "text/plain; charset=utf-8"})

(def open-channels (atom {}))

(defn com-channel [request]
  (httpkit/with-channel request channel
    (swap! open-channels assoc channel request)
    (log/info "Channel opened.")
    (httpkit/on-close channel (fn [status]
                                (log/info "Channel closed: " status)
                                (swap! open-channels dissoc channel)))
    (httpkit/on-receive channel (fn [data]
                                  #_(httpkit/send! channel data)
                                  (broadcast data)))))

(defn broadcast
  [msg]
  (doseq [channel (keys @open-channels)]
    (httpkit/send! channel {:status 200
                            :headers text-header
                            :body msg})))

(defroutes app-routes
  (GET "/" [] (response/resource-response "index.html" {:root "public"}))
  (GET "/com-channel" [] com-channel)
  (GET "/test" [] (do
                    (broadcast "test")
                    "sent message!"))
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
