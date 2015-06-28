(ns chatapp.chat.state)

(def client-db (ref {}))
(def chatroom-db (ref {}))
(def nick-db (ref {}))

(defn uuid []
  (let [new-uuid (str (java.util.UUID/randomUUID))]
    (if (get @client-db new-uuid)
      (recur)
      new-uuid)))

(defn get-client
  [id]
  (get @client-db id))

(defn add-client
  [id nick & attrs]
  (dosync
    (if (get-client id)
      nil ; Client already exists
      (let [client (merge (apply hash-map attrs)
                          {:nick nick
                           :chatrooms #{}})]
        (alter nick-db assoc nick id)
        (alter client-db assoc id client)
        client))))

(defn get-client-attr
  [id attr]
  (get-in @client-db [id attr]))

(defn remove-client
  [id]
  (dosync
    (if-let [client (get @client-db id)]
      (do
        (doseq [room-name (:chatrooms client)]
          (alter chatroom-db update-in [room-name :members] disj id))
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
          chatroom (if-let [existing-chat (get @chatroom-db room-name)]
                     (update-in existing-chat [:members] conj id)
                     (mk-chatroom room-name id channel))]
      (alter chatroom-db assoc room-name chatroom)
      (alter client-db update-in [id :chatrooms] conj room-name))))

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

(defn find-nick
  [nick]
  (get @nick-db nick))
