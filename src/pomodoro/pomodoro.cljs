(ns pomodoro.pomodoro
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]))

(enable-console-print!)

; START time intervals
(def ^:constant one-min (* 1e3 60))
(def ^:constant presets {:five (* one-min 5)
                         :twenty-five (* one-min 25)})
; END time intervals

;; Must extend number for js/Number since time
;; is represented as milliseconds
(extend-type number
  ICloneable
  (-clone [n] (js/Number. n)))

(extend-type boolean
  ICloneable
  (-clone [b] (js/Boolean. b)))

(defn now [] (.now js/Date))

(defn default-state []
  {:stime (now)
   :etime (+ (:twenty-five presets) (now))
   :on false})

; START app-state
;; FIXME remove assoc
(def app-state (atom (assoc (default-state) :on true)))
; END app-state

; START format-time
(defn format-time
  "Format time as min:sec"
  [d]
  (let [min (.getMinutes d)
        sec (.getSeconds d)
        formatted (map #(if (< % 10) (str "0" %) %)
                       [min sec])]
    (string/join ":" formatted)))
; END format-time

; START display-time multi method
(defmulti display-time (fn [d] (type d)))
(defmethod display-time js/Number [d] (format-time (js/Date. d)))
(defmethod display-time js/Date [d] (format-time d))
; END display-time

; START timer-top
(defn timer-top [app _]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "timer-top"}
               (dom/div #js {:className "timer-gems"}
                        (dom/div #js {:className "gem gem-red"} "")
                        (dom/div #js {:className "gem gem-yellow"} "")
                        (dom/div #js {:className "gem gem-green"} ""))
           (dom/div #js {:className "timer-title"} "pOModoro")))))
; END timer-top

; START timer-controls
(defn timer-controls [on _]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "timer-controls"}
        ;; play button
        (dom/div #js {:className "play-control-outer control-outer"}
                 (dom/div #js {:className "control-inner"}
                          (dom/div #js {:className "control-icon control-icon-play"
                                        :onClick #(om/transact! on (fn [_] true))}
                                   (dom/i #js {:className "icon-play"}))))

        ;; Stop button
        (dom/div #js {:className "stop-control-outer control-outer"}
                 (dom/div #js {:className "control-inner"}
                          (dom/div #js {:className "control-icon control-icon-stop"
                                        :onClick #(om/transact! on (fn [_] false))}
                                   (dom/i #js {:className "icon-stop"}))))
        ;; Reset button
        (dom/div #js {:className "reset-control-outer control-outer"}
                 (dom/div #js {:className "control-inner"}
                          (dom/div #js {:className "control-icon"}
                                   (dom/i #js {:className "icon-refresh"}))))))))
; END timer-controls

; START timer-view
(defn timer-view [{:keys [etime stime]} _]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "timer-numbers-block"}
        (dom/div #js {:className "timer_numbers"} (display-time (- etime stime)))
        (dom/div #js {:className "timer_number_titles"}
                 (dom/div #js {:className "timer_number_min"} "min")
                 (dom/div #js {:className "timer_number_sec"} "sec"))))))
; END timer-view

; START timer-middle
(defn timer-middle [cursor _]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "timer-middle"}
        (om/build timer-controls (:on cursor))
        (om/build timer-view cursor)))))
; END timer-middle

; START timer-bottom
;; TODO implement current time
(defn timer-bottom [cursor _]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "timer-bottom"}
        (dom/div #js {:className "timer-current-title"} "Current Time:")
        (dom/div #js {:className "timer-current-time"} "00:00:00")))))
; END timer-bottom

; START can-update
(defn can-update [{:keys [stime etime on]}]
  (and (.valueOf on) (> (- etime stime) 0)))
; END can-update


; START pom-view
(defn pom-view [{:keys [stime on] :as app} owner]
  (reify
    om/IDisplayName
    (display-name [_] "pom-view")

    om/IWillMount
    (will-mount [_]
      (js/setInterval
        (fn []
          (om/transact! app #(if (can-update %)
                               (assoc % :stime (+ 1e3 (:stime %)))
                               %)))
        1e3))

    om/IRender
    (render [_]
      (dom/div
        #js {:className "timer-body"}
        (om/build timer-top app)
        (om/build timer-middle app)
        (om/build timer-bottom app)))))
; END pom-view

(om/root pom-view app-state
  {:target (. js/document (getElementById "app"))})
