(ns chatapp.chat.server
  (:require [chatapp.chat.state :refer [add-client find-nick get-client-attr
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
    [ids (assoc out-data :username (get-client-attr user-id :nick))]))

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
    (if (find-nick rnd-name)
      (recur)
      rnd-name)))

(defn send-event
  [ids data]
  (doseq [id ids]
    (httpkit/send! (get-client-attr id :channel) (json-response data))))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn web-socket
  [request]
  (httpkit/with-channel request channel
    (let [user-id (uuid)]
      (log/debug "Client connected to chat server")
      (add-client user-id (new-random-username) :channel channel)
      (join-chatroom user-id :lobby)
      (httpkit/on-close channel
                        (fn [status]
                          (log/info "Channel closed: " status)
                          (remove-client user-id)))
      (httpkit/on-receive channel
                          (fn [data]
                            (log/info "Received: " data)
                            (let [data (json/read-str data :key-fn json->edn)
                                  event (handle-event user-id data)]
                              (log/debug event)
                              (apply send-event event)))))))

