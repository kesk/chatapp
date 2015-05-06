(ns chatapp.chat.state)

(def client-db (ref {}))
(def chatroom-db (ref {}))

(defn add-client
  [id channel & attrs]
  (let [client {:channel channel}]
    (dosync
      (alter client-db assoc id (merge attrs client)))))

(defn remove-client
  [id]
  (dosync
    (if-let [client (get @client-db id)]
      (do
        (doseq [room-name (keys (:chatrooms client))]
          (alter chatroom-db update-in [room-name :members] dissoc id))
        (alter client-db dissoc id))
      nil)))

(defn mk-chatroom
  [room-name id channel]
  {:members #{id}
   :created-by id})

(defn join-chatroom
  [id room-name]
  (dosync
    (let [channel (get-in @client-db [id :channel])
          chatroom (if-let [existing-chat (get room-name @chatroom-db)]
                     (update-in existing-chat [:members] conj id)
                     (mk-chatroom room-name id channel))]
      (alter chatroom-db assoc room-name chatroom)
      (alter client-db update-in [id :chatrooms] assoc room-name chatroom))))

(defn member?
  [id room-name]
  (if (get-in @client-db [id :chatrooms room-name])
    true
    false))

(defn leave-chatroom
  [id room-name]
  (if (member? id room-name)
    (dosync (alter chatroom-db update-in [room-name :members] disj id)
            (alter client-db update-in [id :chatrooms] dissoc room-name))))

(defn members
  [room-name]
  (get-in @chatroom-db [room-name :members]))
