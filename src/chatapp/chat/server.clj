(ns chatapp.chat.server
  (:require [chatapp.chat.state :refer [add-client client-db get-nick
                                        join-chatroom members remove-client]]
            [chatapp.common :refer [json->edn json-response]]
            [clojure.data.json :as json]
            [clojure.set :refer [difference]]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as httpkit]))

;[user-id event]
(defmulti handle-event
  (fn [user-id event]
    (keyword (:type event))))

(defmethod handle-event :message
  [user-id {:keys [chat-room] :as event}]
  (let [ids (members (keyword chat-room))
        out-data (select-keys event [:type :chat-room :contents])]
    [ids (assoc out-data :username (get-nick user-id))]))

(defmethod handle-event :join-chat
  [user-id {:keys [chatroom] :as event}]
  (join-chatroom user-id (keyword chatroom))
  (let [get-other-users (comp #(difference % #{user-id}) members)
        ids (get-other-users (keyword chatroom))]
    [ids {:type :user-joined
          :chat-room chatroom
          :user-id user-id}]))

(defmethod handle-event :default
  [_ _]
  (log/warn "Unknown message type received!"))

(defn random-str [length]
  (let [valid-chars (map char
                         (concat
                           (range (int \a) (int \z))
                           (range (int \A) (int \Z))
                           (range (int \0) (int \9))))]
    (apply str (repeatedly length #(rand-nth valid-chars)))))

(defn new-random-username
  []
  (let [rnd-name (str "usr-" (random-str 10))]
    (if (get-nick rnd-name)
      (recur)
      rnd-name)))

(defn send-event
  [ids data]
  (doseq [channel ids]
    (httpkit/send! channel (json-response data))))

(defn web-socket
  [request]
  (httpkit/with-channel request channel
    (log/debug "Client connected to chat server")
    (add-client channel (new-random-username))
    (join-chatroom channel :lobby)
    (httpkit/on-close channel
                      (fn [status]
                        (log/info "Channel closed: " status)
                        (remove-client channel)))
    (httpkit/on-receive channel
                        (fn [data]
                          (log/info "Received: " data)
                          (let [data (json/read-str data :key-fn json->edn)
                                event (handle-event channel data)]
                            (log/debug event)
                            (apply send-event event))))))

