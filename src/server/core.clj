(ns server.core
  (:require [compojure.route :as route]
            [compojure.core :as compojure]
            [ring.util.response :as response]
            [ring.adapter.jetty :as jetty]))

(def root (str (System/getProperty "user.dir") "/pomodoro"))

(compojure/defroutes app
  (compojure/GET "/" request
    (response/file-response "index.html" {:root root}))
  (route/files "/pomodoro" {:root root}))

(defn -main []
  (prn "View the example at http://localhost:3000/")
  (jetty/run-jetty app {:join? true :port 3000}))

