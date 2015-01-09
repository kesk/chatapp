(ns chatapp.chat.chat-page-test
  (:require [chatapp.chat.chat-page :refer :all]
            [chatapp.chat.chatroom :as chatroom]
            [clojure.test :refer :all]))

(defn clear-chatrooms [f]
  (f)
  (reset! chats chatroom/empty-store))

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

(deftest handle-event-test
  (chatroom/join @chats "id" :foochat)
  (testing "Chat message"
    (let [session {:username "username"}
          data {:type :message
                :chat-room "foochat"
                :contents "Hello, World!"
                :foo "Mallicious"}
          expected (-> data
                    (dissoc :foo)
                    (assoc :username "username"))
          [id d] (handle-event "id" session data)]
      (is (= expected d))
      (is (= (-> d keys set) #{:type :username :chat-room :contents}))
      (is (= (:username d) "username")))))
