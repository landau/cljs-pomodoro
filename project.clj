(defproject pomodoro "0.0.4"
  :license {:name "ISC"
            :url "http://opensource.org/licenses/ISC"
            :distribution :repo}

  :description "Pomodoro implemented with Om"
  :url "https://github.com/landau/cljs-pomodoro"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [om "0.7.3"]
                 [org.clojure/tools.nrepl "0.2.5"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.4.0-SNAPSHOT"]]

  :profiles {:dev {:dependencies [[ring/ring-jetty-adapter "1.3.1"]
                                  [compojure "1.1.0"]
                                  [com.cemerick/piggieback "0.1.3"]]}}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  
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
                                   ;;:preamble ["public/js/react-0.9.0.min.js"
                                   ;;           "public/js/moment.min.js"]
                                   :externs ["public/js/react-0.9.0.js"
                                             "public/js/moment.min.js"]
                                   }}]})
