(ns chatapp.core-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def prop-no-42
  (prop/for-all [v (gen/vector gen/int)]
                (not (some #{42} v))))

(tc/quick-check 100 prop-no-42)

(defspec first-element-is-min-after-sorting ;; the name of the test
  100 ;; the number of iterations for test.check to test
  (prop/for-all [v (gen/not-empty (gen/vector gen/int))]
                (= (apply min v)
                   (first (sort v)))))
