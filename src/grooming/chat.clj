(ns grooming.chat
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST defroutes]]
            [grooming.common :refer :all]
            [org.httpkit.server :as httpkit]
            [ring.util.response :as response]))

(def chatrooms (atom {}))
(def open-channels (atom {}))

(defn- get-username
  [request]
    (get-in request [:session :username]))

(defn broadcast
  "Broadcast a message on all sockets."
  [data]
  (doseq [channel (vals @open-channels)]
    (httpkit/send! channel (json-response data))))

(defn- add-channel
  "Save channel in channel atom"
  [session channel]
  ;Close any already open channels
  (if-let [channel (get @open-channels session)]
    (httpkit/close channel))
  (swap! open-channels assoc session channel))

(defmulti handle-input
  (fn [session data]
    (keyword (:type data))))

(defmethod handle-input :message
  [{username :username} data]
  (broadcast (assoc data :username username)))

(defmethod handle-input :default
  [session data]
  (log/warn "Unknown message type received!"))

(defn- web-socket
  [request]
  (httpkit/with-channel request channel
    (let [session (get-in request [:cookies "ring-session" :value])]
      (add-channel session channel)
      (httpkit/on-close channel (fn [status]
                                  (log/info "Channel closed: " status)
                                  (swap! open-channels dissoc session)))
      (httpkit/on-receive channel (fn [data] (handle-input
                                              (:session request)
                                              (json/read-str data :key-fn keyword)))))))

(defn- join-chat
  [request]
  (let [session (:session request)
        username (:username session)]
    (-> (response/response (render-template "chat.html" {:username username}))
        (response/content-type "text/html; charset=utf-8")
        (assoc :session session))))

(defroutes chat-routes
  (POST "/" request (join-chat request))
  (GET "/" request (join-chat request))
  (GET "/socket" [] web-socket))
