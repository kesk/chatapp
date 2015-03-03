(ns chatapp.chat.chatroom-test
  (:require [chatapp.chat.chatroom :refer :all]
            [clojure.test :refer :all]))

(deftest join-chat-single-user
  (let [store (join empty-store "id" :foo-chat)]
    (is (member? store "id" :foo-chat)
        "'id' should be member of :foo-chat")
    (is (= #{"id"}
           (members store :foo-chat))
        "Member list should contain 'id'")))

(deftest join-chat-multiple-users
  (let [ids '("foo" "bar" "baz")
        store (reduce #(join %1 %2 :foo-chat) empty-store ids)]
    (doseq [id ids]
      (is (member? store id :foo-chat)))
    (is (= (set ids)
           (members store :foo-chat)))))

(deftest leave-chat
  (testing "One user"
    (let [store (-> empty-store
                    (join "user" :chat)
                    (leave "user" :chat))]
      (is (not (member? store "user" :chat)))
      (is (not (chat-exists? store :chat)))))
  (testing "Multiple users"
    (let [store (-> empty-store
                    (join "user1" :chat)
                    (join "user2" :chat)
                    (leave "user1" :chat))]
      (is (not (member? store "user1" :chat)))
      (is (member? store "user2" :chat))
      (is (chat-exists? store :chat)
          "Chat room still exists."))))
