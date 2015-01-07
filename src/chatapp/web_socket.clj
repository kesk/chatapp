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
  (let [send-to #(doseq [channel (vals %)]
                  (httpkit/send! channel (json-response data)))]
    (case ids
      :all (send-to @open-channels)
      (send-to (select-keys @open-channels ids)))))

(defn web-socket
  [on-event request & {:keys [on-close]}]
  (httpkit/with-channel request channel
    (let [session-id (get-in request [:cookies "ring-session" :value])]
      (add-channel session-id channel)
      (httpkit/on-close channel (fn [status]
                                  (log/info "Channel closed: " status)
                                  (remove-channel session-id)
                                  (if on-close (on-close))))
      (httpkit/on-receive channel
                          (fn [data]
                            (let [session (:session request)
                                  data (json/read-str data :key-fn json->edn)
                                  [ids response] (on-event session-id session data)]
                                (if (not (nil? response)) (send-event ids response))))))))
