(ns grooming.chatroom-test
  (:require [clojure.test :refer :all]
            [grooming.chat.chatroom :refer :all]))

(defn clear-all-chat-rooms [f]
  (f)
  (clear-all))

(use-fixtures :each clear-all-chat-rooms)

(deftest join-chat-single-user
  (join "id" :foo-chat)
  (is (member? "id" :foo-chat)
      "'id' should be member of :foo-chat")
  (is (= #{"id"}
         (members :foo-chat))
      "Member list should contain 'id'"))

(deftest join-chat-multiple-users
  (let [ids '("foo" "bar" "baz")]
    (doseq [id ids]
      (join id :foo-chat)
      (is (member? id :foo-chat)))
    (is (= (set ids)
           (members :foo-chat)))))
