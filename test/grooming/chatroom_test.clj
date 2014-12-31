(ns grooming.chatroom-test
  (:require [clojure.test :refer :all]
            [grooming.chat.chatroom :refer :all]))

(deftest join-and-leave-chat
  (is (= {:foobar {:members #{"id"}
                   :created-by "id"}}
         (join "id" :foobar))
      "Join chat")
  (is (= {}
         (leave "id" :foobar))
      "Leave chat"))
