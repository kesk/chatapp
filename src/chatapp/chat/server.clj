(ns chatapp.chat.server
  (:require [chatapp.chat.chatroom :as chatroom]
            [chatapp.common :refer [json->edn json-response]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as httpkit]))

(def user-db (ref {}))
(def chat-db (atom chatroom/empty-store))

;[user-id event]
(defmulti handle-event
  (fn [user-id event]
    (keyword (:type event))))

(defmethod handle-event :message
  [user-id {:keys [chat-room] :as event}]
  (let [ids (chatroom/members @chat-db (keyword chat-room))
        out-data (select-keys event [:type :chat-room :contents])]
    [ids (assoc out-data :username user-id)]))

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

(defn new-user
  ([store]
   (dosync
     (let [rnd-name (str "usr-" (random-str 10))]
       (if (get @store rnd-name)
         (recur)
         (do
           (alter store assoc rnd-name {})
           rnd-name)))))
  ([store username]
  (dosync
    (if (get @store username)
      (new-user store)
      (do
        (alter store assoc username {})
        username)))))

(defn send-event
  [ids data]
  (doseq [[_ {:keys [channel]}] (select-keys @user-db ids)]
    (httpkit/send! channel (json-response data))))

(defn web-socket
  [request]
  (httpkit/with-channel request channel
    (let [id (new-user user-db)]
      (dosync (alter user-db assoc-in [id :channel] channel))
      (swap! chat-db chatroom/join id :lobby)
      (httpkit/on-close channel
                        (fn [status]
                          (log/info "Channel closed: " status)
                          (dosync alter user-db dissoc id)))
      (httpkit/on-receive channel
                          (fn [data]
                            (log/info "Received: " data)
                            (let [data (json/read-str data :key-fn json->edn)
                                  event (handle-event id data)]
                              (apply send-event event)))))))
