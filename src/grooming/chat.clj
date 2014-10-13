(ns grooming.chat
  (:use [grooming.common])
  (:require [org.httpkit.server :as httpkit]
            [compojure.core :refer [routes ANY GET POST]]
            [ring.util.response :as response]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [sandbar.stateful-session :refer [session-put! session-get]]
            [selmer.parser :as selmer]))

(def chatrooms (atom {}))
(def open-sockets (atom {}))

(defn broadcast
  [msg]
  (doseq [channel (keys @open-sockets)]
    (httpkit/send! channel {:status 200
                            :headers json-header
                            :body msg})))

(defn- add-channel
  [chatroom channel]
  (swap! chatrooms assoc
         chatroom
         (if-let [room (@chatrooms chatroom)]
           (conj room channel)
           [channel])))

(defn web-socket
  [request]
  (httpkit/with-channel request channel
    (swap! open-sockets assoc channel request)
    (add-channel (session-get :chatroom) channel)
    (log/info (str "Channel opened for chatroom: "
                   (session-get :chatroom "ERROR: No chat room in session")))
    (httpkit/on-close channel (fn [status]
                                (log/info "Channel closed: " status)
                                (swap! open-sockets dissoc channel)))
    (httpkit/on-receive channel (fn [data]
                                  (broadcast data)))))

(defn join-chat
  [request]
  (if-let [username (get-in request [:params "username"])]
    (do
      (session-put! :username username)
      (session-put! :chatroot "lobby")
      (render-template "chat.html" {}))
    (response/redirect "/")))

(defn chat-routes
  []
  (routes
   (POST "/" [] join-chat)
   (GET "/socket" [] web-socket)))
