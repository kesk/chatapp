(ns chatapp.chat.state-test
  (:require [chatapp.chat.state :refer :all]
            [clojure.test :refer :all]))

(deftest user-joins-and-leaves
  (add-client :client-id "nickname")
  (is (get @client-db :client-id))
  (join-chatroom :client-id :the-chat)
  (is (= #{:client-id} (members :the-chat)))
  (is (member? :client-id :the-chat))
  (leave-chatroom :client-id :the-chat)
  (is (not (member? :client-id :the-chat)))
  (is (= #{} (members :the-chat)))
  (remove-client :client-id)
  (is (nil? (get @client-db :client-id))))

(defn add-clients
  [num-clients & chatrooms]
  (doseq [n (range num-clients)]
    (let [id (keyword (str "client-" n))]
      (add-client id (str "nick-" n))
      (doseq [chatroom chatrooms]
        (join-chatroom id chatroom)))))

(deftest add-two-clients
  (add-clients 2 :chatroom)
  (is (member? :client-0 :chatroom))
  (is (member? :client-1 :chatroom))
  (is (= #{:client-0 :client-1} (members :chatroom))))
