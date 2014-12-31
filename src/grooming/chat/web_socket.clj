(ns grooming.chat.web-socket
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [grooming.chat.chatroom :as chatroom]
            [grooming.common :refer [json-response]]
            [org.httpkit.server :as httpkit]))

(def open-channels (atom {}))

(defn broadcast
  "Broadcast a message on all sockets."
  [data]
  (doseq [channel (vals @open-channels)]
    (httpkit/send! channel (json-response data))))

(defn send-data
  [room-name data]
  (let [ids (chatroom/members room-name)
        data (assoc data :chatroom room-name)]
    (doseq [[id channel] (select-keys @open-channels ids)]
      (httpkit/send! channel (json-response data)))))

(defn- add-channel
  "Save channel in channel atom"
  [session channel]
  ;Close any already open channels
  (if-let [channel (get @open-channels session)]
    (httpkit/close channel))
  (swap! open-channels assoc session channel))

(defmulti handle-input
  (fn [session data]
    (keyword (:type data))))

(defmethod handle-input :message
  [{username :username} data]
  (broadcast (assoc data :username username)))

(defmethod handle-input :default
  [session data]
  (log/warn "Unknown message type received!"))

(defn web-socket
  [request]
  (httpkit/with-channel request channel
    (let [session (get-in request [:cookies "ring-session" :value])]
      (add-channel session channel)
      (chatroom/join session :lobby)
      (httpkit/on-close channel (fn [status]
                                  (log/info "Channel closed: " status)
                                  (swap! open-channels dissoc session)))
      (httpkit/on-receive channel (fn [data] (handle-input
                                              (:session request)
                                              (json/read-str data :key-fn keyword)))))))
