(ns grooming.chat.web-socket
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [grooming.chat.chatroom :as chatroom]
            [grooming.common :refer [json-response json->edn]]
            [org.httpkit.server :as httpkit]))

(def open-channels (atom {}))

(defn- add-channel
  "Save channel in channel atom"
  [id channel]
  ;Close any already open channels
  (if-let [channel (get @open-channels id)]
    (httpkit/close channel))
  (swap! open-channels assoc id channel))

(defn- remove-channel
  [id]
  (swap! dissoc @open-channels id))

(defn broadcast
  "Broadcast a message on all sockets."
  [data]
  (doseq [channel (vals @open-channels)]
    (httpkit/send! channel (json-response data))))

(defn send-event
  [ids data]
  (let [send-to #(doseq [channel (vals %)]
                  (httpkit/send! channel (json-response data)))]
    (case ids
      :all (send-to @open-channels)
      (send-to (select-keys @open-channels ids)))))

(defmulti handle-event
  (fn [session data]
    (keyword (:type data))))

(defmethod handle-event :message
  [{u :username} {:keys [chat-room contents] :as data}]
  (let [ids (chatroom/members (keyword chat-room))]
    (send-event ids (assoc data :username u))))

(defmethod handle-event :default
  [session data]
  (log/warn "Unknown message type received!"))

(defn web-socket
  [request]
  (httpkit/with-channel request channel
    (let [session-id (get-in request [:cookies "ring-session" :value])]
      (add-channel session-id channel)
      (chatroom/join session-id :lobby)
      (httpkit/on-close channel (fn [status]
                                  (log/info "Channel closed: " status)
                                  (swap! open-channels dissoc session-id)))
      (httpkit/on-receive channel (fn [data] (handle-event
                                              (:session request)
                                              (json/read-str data :key-fn json->edn)))))))
