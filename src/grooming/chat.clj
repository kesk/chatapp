(ns grooming.chat
  (:use [grooming.common])
  (:require [org.httpkit.server :as httpkit]
            [compojure.core :refer [routes defroutes ANY GET POST]]
            [ring.util.response :as response]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [selmer.parser :as selmer]
            [clojure.data.json :as json]))

(def chatrooms (atom {}))
(def open-sockets (atom {}))

(defn- get-username
  [request]
    (get-in request [:session :username]))

(defn broadcast
  "Broadcast a message on all channels."
  [data]
  (doseq [channel (vals @open-sockets)]
    (httpkit/send! channel (json-response data))))

(defn- add-channel
  [chatroom channel]
  (swap! chatrooms assoc
         chatroom
         (if-let [room (@chatrooms chatroom)]
           (conj room channel)
           [channel])))

(defn- web-socket
  [request]
  (log/debug (with-out-str (pprint (:session request))))
  (httpkit/with-channel request channel
    (let [username (get-username request)]
      (swap! open-sockets assoc username channel))
    (httpkit/on-close channel (fn [status]
                                (log/info "Channel closed: " status)
                                (swap! open-sockets dissoc channel)))
    (httpkit/on-receive channel (fn [data]
                                  (let [data (json/read-str data)
                                        username (-> request :session :username)]
                                    (broadcast (assoc data :username username)))))))

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
