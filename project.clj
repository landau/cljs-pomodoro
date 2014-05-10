(defproject pomodoro "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "https://github.com/landau/cljs-pomodoro"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.6.2"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :compiler {:output-to "pomodoro.js"
                                   :output-dir "out"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true}}
                       {:id "release"
                        :source-paths ["src"]
                        :compiler {:output-to "pomodoro.js"
                                   :output-dir "out"
                                   :optimizations :advanced}}]})
