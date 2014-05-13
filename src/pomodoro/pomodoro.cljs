(ns pomodoro.pomodoro
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

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
   :etime (+ (:five presets) (now))
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
        sec (.getSeconds d)]
    (str min ":" (if (< sec 10) (str "0" sec) sec))))
; END format-time

; START display-time multi method
(defmulti display-time (fn [d] (type d)))
(defmethod display-time js/Number [d] (format-time (js/Date. d)))
(defmethod display-time js/Date [d] (format-time d))
; END display-time

; START timer-view
(defn timer-view [{:keys [stime etime on] :as app} owner]
  (reify
    om/IDisplayName
    (display-name [_] "timer-view")

    om/IInitState
    (init-state [_]
      {:btn-text (if (.valueOf on) "Pause" "Resume")})

    om/IWillUpdate
    (will-update [_ {:keys [on]} _]
      (om/set-state! owner :btn-text (if (.valueOf on) "Pause" "Resume")))

    om/IRenderState
    (render-state [_ state]
      (dom/div
        #js {:className "row"}
        (dom/h1 nil
               (display-time (- etime stime)))

        (dom/button #js {:className "btn btn-lg"
                         :onClick #(om/transact! on (fn [v] (not v)))}
                    (:btn-text state))))))
; END timer-view

; START control-btn
(defn control-btn [{:keys [stime etime on] :as cursor}]
  (reify
    om/IDisplayName (display-name [_] "control-btn")

    om/IRenderState
    (render-state [_ state]
      (dom/button
        #js {:className "btn btn-default"
             :onClick (fn [_]
                        (when (not (.valueOf on))
                          (om/transact! cursor #(assoc %
                                                    :stime (now)
                                                    :etime (+ (:time state) (now))))))}
        (/ (:time state) 1e3 60)))))
; END control-btn

; START controls-view
(defn controls-view [{:keys [stime etime on] :as cursor}]
  (reify
    om/IDisplayName (display-name [_] "controls-view")

    om/IRender
    (render [_]
      (dom/div
        #js {:className "row"}
        (dom/div #js {:className "btn-group"}
                 (om/build control-btn cursor {:init-state {:time (:five presets)}})
                 (om/build control-btn cursor {:init-state {:time (:twenty-five presets)}}))))))
; END controls-view

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
(defn timer-controls [app _]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "timer-controls"}
        ;; play button
        (dom/div #js {:className "play-control-outer control-outer"}
                 (dom/div #js {:className "control-inner"}
                          (dom/div #js {:className "control-icon control-icon-play"}
                                   (dom/i #js {:className "icon-play"}))))

        ;; Stop button
        (dom/div #js {:className "stop-control-outer control-outer"}
                 (dom/div #js {:className "control-inner"}
                          (dom/div #js {:className "control-icon control-icon-stop"}
                                   (dom/i #js {:className "icon-stop"}))))
        ;; Reset button
        (dom/div #js {:className "reset-control-outer control-outer"}
                 (dom/div #js {:className "control-inner"}
                          (dom/div #js {:className "control-icon"}
                                   (dom/i #js {:className "icon-refresh"}))))))))
; END timer-controls

; START timer-view
(defn timer-view [app _]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "timer-numbers-block"}
        (dom/div #js {:className "timer_numbers"} "00:00:00")
        (dom/div #js {:className "timer_number_titles"}
                 (dom/div #js {:className "timer_number_hour"} "hour")
                 (dom/div #js {:className "timer_number_min"} "min")
                 (dom/div #js {:className "timer_number_sec"} "sec"))))))
; END timer-view

; START timer-middle
(defn timer-middle [app _]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "timer-middle"}
        (om/build timer-controls app)
        (om/build timer-view app)))))
; END timer-middle

; START timer-bottom
(defn timer-bottom [app _]
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
