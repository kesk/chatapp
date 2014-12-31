(ns grooming.chat.chat-page
  (:require [compojure.core :refer [GET POST defroutes]]
            [grooming.chat.web-socket :as socket]
            [grooming.common :refer :all]
            [ring.util.response :as response]))

(defn- render-chat
  [request]
  (let [session (:session request)
        username (:username session)]
    (-> (response/response (render-template "chat.html" {:username username}))
        (response/content-type "text/html; charset=utf-8")
        (assoc :session session))))

(defroutes chat-routes
  (POST "/" [] render-chat)
  (GET "/" [] render-chat)
  (GET "/socket" [] socket/web-socket))
