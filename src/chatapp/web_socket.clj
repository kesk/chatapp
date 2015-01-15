(ns chatapp.web-socket
  (:require [chatapp.common :refer [json->edn json-response]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
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
  (swap! open-channels dissoc id))

(defn broadcast
  "Broadcast a message on all sockets."
  [data]
  (doseq [channel (vals @open-channels)]
    (httpkit/send! channel (json-response data))))

(defn send-event
  [ids data]
  (let [channels (select-keys @open-channels ids)]
    (doseq [[_ ch] channels]
      (httpkit/send! ch (json-response data)))))

(defn send-events
  [& events]
  (doseq [[ids data] events]
    (send-event ids data)))

(defn web-socket
  [handler request & {:keys [on-close]}]
  (httpkit/with-channel request channel
    (let [session-id (get-in request [:cookies "ring-session" :value])]
      (add-channel session-id channel)
      (httpkit/on-close channel (fn [status]
                                  (log/info "Channel closed: " status)
                                  (remove-channel session-id)
                                  (if on-close (on-close))))
      (httpkit/on-receive channel
                          (fn [data]
                            (let [username (get-in request [:session :username])
                                  data (json/read-str data :key-fn json->edn)
                                  events (handler [session-id username] data)]
                              (if events (apply send-events events))))))))
