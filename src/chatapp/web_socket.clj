(ns chatapp.web-socket
  (:require [chatapp.common :refer [json->edn json-response]]
            [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [org.httpkit.client :refer [request]]
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

(defn hashmap-to-string [m]
  (with-out-str (pprint m)))

(defn web-socket
  [handler reques-t & {:keys [on-close]}]
  (httpkit/with-channel request channel
    (let [id (-> request
                 hashmap-to-string
                 digest/md5)]
      (add-channel id channel)
      (httpkit/on-close channel (fn [status]
                                  (log/info "Channel closed: " status)
                                  (remove-channel id)
                                  (if on-close (on-close))))
      (httpkit/on-receive channel
                          (fn [data]
                            (log/debug (str "Received: " data))
                            (let [username (get-in request [:session :username])
                                  data (json/read-str data :key-fn json->edn)
                                  events (handler [id username] data)]
                              (if events (apply send-events events))))))))

