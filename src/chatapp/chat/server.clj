(ns chatapp.chat.server
  (:require [chatapp.chat.state :refer [add-client client-db join-chatroom
                                        members remove-client]]
            [chatapp.common :refer [json->edn json-response]]
            [clojure.data.json :as json]
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

(defn new-random-username
  []
  (let [rnd-name (str "usr-" (random-str 10))]
    (if (get @client-db rnd-name)
      (recur)
      rnd-name)))

(defn send-event
  [ids data]
  (doseq [[_ client] (select-keys @client-db ids)]
    (httpkit/send! (:channel client) (json-response data))))

(defn web-socket
  [request]
  (httpkit/with-channel request channel
    (let [id (new-random-username)]
      (add-client id channel)
      (join-chatroom id :lobby)
      (httpkit/on-close channel
                        (fn [status]
                          (log/info "Channel closed: " status)
                          (remove-client id)))
      (httpkit/on-receive channel
                          (fn [data]
                            (log/info "Received: " data)
                            (let [data (json/read-str data :key-fn json->edn)
                                  event (handle-event id data)]
                              (apply send-event event)))))))

