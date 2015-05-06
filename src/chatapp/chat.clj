(ns chatapp.chat
  (:require [chatapp.chat.server :as server]
            [chatapp.common :refer [render-template]]
            [compojure.core :refer [GET POST defroutes]]
            [ring.util.response :as response]))

(defn- render-chat
  [request]
  (-> (response/response (render-template "chat.html" {}))
      (response/content-type "text/html; charset=utf-8")))

(defn chatroom-handler
  "Shows either the chat page or starts a websocket depending on the request made."
  [request]
  (if-not (:websocket? request)
    (render-chat request)
    (server/web-socket request)))

(defroutes chat-routes
  (POST "/" req (chatroom-handler req))
  (GET "/" req (chatroom-handler req)))
