(ns chatapp.chat.user-store)

(def empty-store {})

(defn new-user
  [store id username]
  (assoc store id {:username username}))

(defn change-username
  [store id username]
  (assoc-in store [id :username] username))

(defn update
  [store id kvm]
  (update-in store [id] #(merge % kvm)))
