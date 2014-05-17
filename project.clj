(defproject pomodoro "0.0.1"
  :license {:name "ISC"
            :url "http://opensource.org/licenses/ISC"
            :distribution :repo}

  :description "Pomodoro implemented with Om"
  :url "https://github.com/landau/cljs-pomodoro"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.6.2"]]

  :profiles {:dev {:dependencies [[ring/ring-jetty-adapter "1.1.1"]
                                  [compojure "1.1.0"]]}}

  :main server.core

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src"]

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src"]
                        :compiler {:output-to "public/js/pomodoro.js"
                                   :output-dir "public/js/dev"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true}}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {:output-to "public/js/main.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :externs ["public/js/react-0.9.0.js"
                                             "public/js/moment.min.js"]
                                   }}]})
