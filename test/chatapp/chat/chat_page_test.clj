(ns chatapp.chat.chat-page-test
  (:require [chatapp.chat.chat-page :refer :all]
            [chatapp.chat.chatroom :as chatroom]
            [clojure.test :refer :all]))

(defn clear-chatrooms [f]
  (reset! chats chatroom/empty-store)
  (f))

(use-fixtures :each clear-chatrooms)

(defn mock-fn
  "Returns list of arguments given to it"
  [& args]
  args)

(deftest open-socket-test
  (let [f (open-socket mock-fn)
        req {:cookies {"ring-session" {:value "id"}}}
        [_ _ & {:keys [on-close]}] (f req)]

    (is (chatroom/member? @chats "id" :lobby)
        "The user should be member of :lobby after opening socket")

    (testing "on-close function"
      (is on-close "Missing on close function")
      (if on-close
        (do
          (on-close)
          (is (not (chatroom/user-exists? @chats "id"))
              "The user should not exist after closing socket"))))))

(deftest handle-event-message
  (swap! chats chatroom/join "id" :foochat)
  (swap! chats chatroom/join "other-user-1" :foochat)
  (let [user ["id" "name"]
        event {:type "message"
               :chat-room "foochat"
               :contents "Hello friends!"
               :foo "bar"}
        [[ids ret-event]] (handle-event @chats user event)]
    (is (= #{"id" "other-user-1"} ids))
    (is (= (-> event
               (dissoc :foo)
               (assoc :username (second user)))) ret-event)))
