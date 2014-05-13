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
      {:btn-text (if on "Pause" "Resume")})

    om/IDidUpdate
    (did-update [_ {:keys [on]} _]
      ;; FIXME Why isn't this updating?
      (om/set-state! owner :btn-text (if on "Pause" "Resume")))

    om/IRenderState
    (render-state [_ state]
      (dom/div
        #js {:className "col-lg-6"}
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
        #js {:className "btn"
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
        #js {:className "col-lg-6"}
        (om/build control-btn cursor {:init-state {:time (:five presets)}})
        (om/build control-btn cursor {:init-state {:time (:twenty-five presets)}})))))
; END controls-view


; START can-update
(defn can-update [{:keys [stime etime on]}]
  (and on (> (- etime stime) 0)))
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
        nil
        (om/build timer-view app)
        (om/build controls-view app)))))
; END pom-view

(om/root pom-view app-state
  {:target (. js/document (getElementById "app"))})
