(ns grooming.chat.chatroom)

(def ^{:private true} chatrooms (atom {}))

(defn- new-room
  [id]
  {:members #{id}
   :created-by id})

(defn- create
  "Create a chat room with only the user that crated it."
  [id room-name]
  (let [new-room {:members #{id}
                  :created-by id}]
    (swap! chatrooms assoc room-name new-room)
    new-room))

(defn list-rooms
  ([]
   (keys @chatrooms))
  ([id]
   (reduce-kv
    (fn [coll k v]
      (if (contains? (:members v) id)
        (conj coll k)
        coll))
    '() @chatrooms)))

(defn- join-or-create
  [id]
  (fn [rooms room-name]
    (if (room-name rooms)
      (update-in rooms [room-name :members] conj id)
      (assoc rooms room-name (new-room id)))))

(defn join
  "Join a chat room with the name room-name."
  [id & room-names]
  (swap! chatrooms #(reduce (join-or-create id) % room-names)))

(defn leave
  "Leave chat room with name room-name. Returns nil if
  the chat room does not exist."
  [id room-name]
  (if-let [chatroom (room-name @chatrooms)]
    (let [members (disj (:members chatroom) id)]
      ;Remove entire room if empty
      (if (empty? members)
        (swap! chatrooms dissoc room-name)
        (swap! chatrooms assoc-in [room-name :members] members)))))

(defn leave-all
  [id]
  (doseq [[room _] (list-rooms id)]
    (leave id room)))

(defn members
  "Give a list of all the members of a chat room."
  [room-name]
  (get-in @chatrooms [room-name :members]))

(defn member?
  "Checks if id is in the member list of room-name."
  [id room-name]
  (contains? (members room-name) id))

(defn clear-all []
  (reset! chatrooms {}))
