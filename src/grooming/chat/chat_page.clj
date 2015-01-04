(ns grooming.chat.chat-page
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST defroutes]]
            [grooming.chat.chatroom :as chatroom]
            [grooming.common :refer :all]
            [grooming.web-socket :as socket :refer [handle-event]]
            [ring.util.response :as response]))

(defmethod handle-event :message
  [{u :username} {:keys [chat-room contents] :as data}]
  (let [ids (chatroom/members (keyword chat-room))]
    (socket/send-event ids (assoc data :username u))))

(defmethod handle-event :join-chat
  [{id :id} {r :room-name}]
  (chatroom/join id r))

(defn- render-chat
  [request]
  (let [session (:session request)
        username (:username session)]
    (-> (response/response (render-template "chat.html" {:username username}))
        (response/content-type "text/html; charset=utf-8")
        (assoc :session session))))

(defn open-socket
  [req]
  (let [session-id (get-session-id req)]
    (chatroom/join session-id :lobby)
    (socket/web-socket req
                       :on-close #(chatroom/leave-all session-id))))

(defroutes chat-routes
  (POST "/" [] render-chat)
  (GET "/" [] render-chat)
  (GET "/socket" [] open-socket))
