(ns server.core
  (:require [compojure.route :as route]
            [compojure.core :as compojure]
            [ring.util.response :as response]
            [ring.adapter.jetty :as jetty])
  (:import [java.io File]))

(def root (str (System/getProperty "user.dir") "/public"))

(compojure/defroutes app
  (compojure/GET "/" request
    (response/file-response "index.html" {:root root}))
  (route/files "/"))

(defn -main []
  (prn "View the example at http://localhost:3000/")
  (jetty/run-jetty app {:join? true :port 3000}))

