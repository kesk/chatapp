(ns grooming.common
  (:require [selmer.parser :as selmer]))

(def json-header {"Content-Type" "application/json; charset=utf-8"})
(def text-header {"Content-Type" "text/plain; charset=utf-8"})
(def html-header {"Content-Type" "text/html; charset=utf-8"})

(defn render-template
  [tname args]
  (selmer/render-file (str "templates/" tname) args {:tag-open \[, :tag-close \]}))
