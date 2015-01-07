(ns grooming.chat-page-test
  (:require [clojure.test :refer :all]
            [grooming.chat.chat-page :refer :all]
            [grooming.chat.chatroom :as chatroom]))

(defn mock-fn
  "Returns list of arguments given to it"
  [& args]
  args)

(deftest open-socket-test
  (let [f (open-socket mock-fn)
        req {:cookies {"ring-session" {:value "id"}}}
        [_ _ & {:keys [on-close]}] (f req)
        on-close-exists (not (nil? on-close))]

    (is (chatroom/member? @chats "id" :lobby)
        "The user should be member of :lobby after opening socket")

    (testing "on-close function"
      (is on-close-exists "Missing on close function")
      (if on-close-exists
        (do
          (on-close)
          (is (not (chatroom/user-exists? @chats "id"))
              "The user should not exist after closing socket"))))))
