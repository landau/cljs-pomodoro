(defproject pomodoro "0.0.4"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"
            :distribution :repo}

  :description "A simple pomodoro timer"
  :url "https://github.com/landau/cljs-pomodoro"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [com.andrewmcveigh/cljs-time "0.1.6"]
                 [reagent "0.4.2"]]

  :plugins [[lein-ring "0.8.11"]
            [lein-cljsbuild "1.0.3"]
            [lein-environ "0.5.0"]]

  :ring {:handler server.core/app}

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.3.0"]
                                  [compojure "1.1.9"]]
                   :env {:dev true}}

             :release {:ring {:open-browser? false
                              :stacktraces?  false
                              :auto-reload?  false}}}

  :source-paths ["src"]

  :main server.core

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src-cljs"]
                        :compiler {:output-to "public/js/pomodoro.js"
                                   :output-dir "public/js/dev"
                                   :optimizations :none
                                   :pretty-print tru
                                   :source-map true}}
                       {:id "prod"
                        :source-paths ["src-cljs"]
                        :compiler {:output-to "public/js/main.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :externs ["public/js/react-min-0.11.2.js"]
                                   }}]})
