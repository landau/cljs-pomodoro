(ns pomodoro.pomodoro
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def ^:constant one-min (* 1e3 60))

(defn now [] (.now js/Date))

(def app-state (atom {:start (now)
                      :end (+ (* 5 one-min) (now))
                      :on false}))

(defn to-min [start end]
  (/ (js/Date. (- end start))
     one-min))

(defn timer-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil (to-min (:start app) (:end app))))))

(om/root timer-view app-state
  {:target (. js/document (getElementById "app"))})
