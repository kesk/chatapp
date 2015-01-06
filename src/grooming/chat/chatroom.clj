(ns grooming.chat.chatroom)

(def empty-store {:chats {}
                  :users {}})

(defn- new-room
  [store id room-name]
  (assoc-in store [:chats room-name] {:members #{}
                                      :created-by id}))

(defn- new-user
  [store id]
  (assoc-in store [:users id] #{}))

(defn members
  [store room-name]
  (get-in store [:chats room-name :members]))

(defn member?
  [store id room-name]
  (not (nil? (get (members store room-name) id))))

(defn- chat-exists?
  [store room-name]
  (not (nil? (get-in store [:chats room-name]))))

(defn- user-exists?
  [store id]
  (not (nil? (get-in store [:users id]))))

(defn- update-chat
  [store room-name f & args]
  (let [channel (apply f (get-in store [:chats room-name]) args)]
    (if (empty? (:members channel))
      (update-in store [:chats] dissoc room-name)
      (assoc-in store [:chats room-name] channel))))

(defn- update-user
  [store id f & args]
  (let [user (apply f (get-in store [:users id]) args)]
    (if (empty? user)
      (update-in store [:users] dissoc id)
      (assoc-in store [:users id] user))))

(defn join
  "Join a chat room with the name room-name."
  [store id room-name]
  (-> store
      ;new chat?
      (cond-> (not (chat-exists? store room-name))
              (new-room id room-name))
      ;new user?
      (cond-> (not (user-exists? store id))
              (new-user id))
      (update-chat room-name update-in [:members] conj id)
      (update-user id conj room-name)))

(defn leave
  "Leave chat room with name room-name. Returns nil if
  the chat room does not exist."
  [store id room-name]
  (-> store
      (update-chat room-name update-in [:members] disj id)
      (update-user id disj room-name)))

(defn leave-all
  [store id]
  (let [chats (get-in store [:users id])]
    (reduce #(leave %1 id %2) store chats)))
