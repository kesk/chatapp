(ns grooming.chat.chat-page
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST defroutes]]
            [grooming.chat.chatroom :as chatroom]
            [grooming.common :refer :all]
            [grooming.web-socket :as socket]
            [ring.util.response :as response]))

(def chats (atom chatroom/empty-store))

(defmulti handle-event
  (fn [id session data]
    (keyword (:type data))))

(defmethod handle-event :message
  [_ {u :username} {:keys [chat-room] :as data}]
  (let [ids (chatroom/members @chats (keyword chat-room))]
    (list ids (assoc data :username u))))

(defmethod handle-event :join-chat
  [id _ {r :room-name}]
  (swap! chats chatroom/join id r))

(defmethod handle-event :default
  [_ _ _]
  (log/warn "Unknown message type received!"))

(defn open-socket
  [socket-fn]
  (fn [req]
    (let [session-id (get-session-id req)
          on-close #(swap! chats chatroom/leave-all session-id)]
      (swap! chats chatroom/join session-id :lobby)
      (socket-fn handle-event req :on-close on-close))))

(defn- render-chat
  [request]
  (let [session (:session request)
        username (:username session)]
    (-> (response/response (render-template "chat.html" {:username username}))
        (response/content-type "text/html; charset=utf-8")
        (assoc :session session))))

(defroutes chat-routes
  (POST "/" [] render-chat)
  (GET "/" [] render-chat)
  (GET "/socket" [] (open-socket socket/web-socket)))
