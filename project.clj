(defproject chatapp "0.1.0-SNAPSHOT"
  :description "Tool for estimating user story points."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.0"]
                 [ring/ring-devel "1.3.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [compojure "1.1.5"]
                 [http-kit "2.1.16"]
                 [org.clojure/tools.logging "0.3.0"]
                 [ch.qos.logback/logback-classic "1.1.1"]
                 [org.clojure/data.json "0.2.5"]
                 [selmer "0.6.9"]
                 [environ "1.0.0"]
                 [org.clojure/test.check "0.6.2"]
                 [digest "1.4.4"]]
  :main chatapp.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
