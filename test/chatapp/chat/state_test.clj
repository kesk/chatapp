(ns chatapp.chat.state-test
  (:require [chatapp.chat.state :refer :all]
            [clojure.test :refer :all]))

(deftest user-joins-and-leaves
  (add-client  "nick" :comm-channel)
  (is (get @client-db "nick"))
  (join-chatroom "nick" :the-chat)
  (is (= #{"nick"} (members :the-chat)))
  (is (member? "nick" :the-chat))
  (leave-chatroom "nick" :the-chat)
  (is (not (member? "nick" :the-chat)))
  (is (= #{} (members :the-chat)))
  (remove-client :comm-channel)
  (is (nil? (get @client-db :comm-channel))))
