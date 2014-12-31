(ns grooming.chat.chatroom)

(def ^{:private true} chatrooms (atom {}))

(defn- create
  "Create a chat room with only the user that crated it."
  [id room-name]
  (swap! chatrooms assoc room-name
         {:members #{id}
          :created-by id}))

(defn join
  "Join a chat room with the name room-name."
  [id room-name]
  (if-let [chatroom (room-name @chatrooms)]
    (let [members (conj (:members chatroom) id)]
      (swap! chatrooms assoc-in [room-name :members] members))
      (create id room-name)))

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

(defn members
  "Give a list of all the members of a chat room."
  [room-name]
  (get-in [room-name :members] @chatrooms))

(defn member?
  "Checks if id is in the member list of room-name."
  [id room-name]
  (let [members (get-in [room-name :members] @chatrooms)]
    (contains? members id)))
