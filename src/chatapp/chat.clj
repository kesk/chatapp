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
  [request]
  (if-not (:with-websocket? request)
    (render-chat)
    (server/web-socket request)))

(defroutes chat-routes
  (POST "/" req (render-chat req))
  (GET "/" req (render-chat req))
  (GET "/socket" req (server/web-socket req)))
