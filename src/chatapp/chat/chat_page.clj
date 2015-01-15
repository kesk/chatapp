(ns chatapp.chat.chat-page
  (:require [chatapp.chat.chatroom :as chatroom]
            [chatapp.common :refer [get-session-id render-template]]
            [chatapp.web-socket :as socket]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST defroutes]]
            [ring.util.response :as response]))

(def chats (atom chatroom/empty-store))

;user [id username]
;[store user event]
(defmulti handle-event
  (fn [store user event]
    (keyword (:type event))))

(defmethod handle-event :message
  [store [_ username] {:keys [chat-room] :as event}]
  (let [ids (chatroom/members store (keyword chat-room))
        out-data (select-keys event [:type :chat-room :contents])]
    [[ids (assoc out-data :username username)]]))

(defmethod handle-event :join-chat
  [store [id _] {r :room-name}]
  (swap! store chatroom/join id r))

(defmethod handle-event :default
  [_ _ _]
  (log/warn "Unknown message type received!"))

(defn open-socket
  [socket-fn]
  (fn [req]
    (let [session-id (get-session-id req)
          on-close #(swap! chats chatroom/leave-all session-id)]
      (swap! chats chatroom/join session-id :lobby)
      (socket-fn (partial handle-event @chats) req
                 :on-close on-close))))

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
