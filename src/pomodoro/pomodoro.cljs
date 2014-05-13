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
;; TODO remove assoc
(def app-state (atom (assoc (default-state) :on true)))
; END app-state

; START format-time
(defn format-time
  "Format time as min:sec"
  [d]
  (str (.getMinutes d) ":" (.getSeconds d)))
; END format-time

; START display-time multi method
(defmulti display-time (fn [d] (type d)))
(defmethod display-time js/Number [d] (format-time (js/Date. d)))
(defmethod display-time js/Date [d] (format-time d))
; END display-time

; START timer-view
(defn timer-view [{:keys [stime etime on]} _]
  (reify
    om/IDisplayName
    (display-name [_] "timer-view")

    om/IRender
    (render [_]
      (dom/div
        #js {:className "col-lg-6"}
        (dom/h1 nil
               (display-time (- etime stime)))
        (dom/button #js {:className "btn btn-lg"
                         :onClick #(om/transact! on (fn [v] (not v)))}
                    (if on "Pause" "Resume"))))))
; END timer-view

; START controls-view
(defn controls-view [{:keys [stime etime on]}]
  (reify
    om/IDisplayName
    (display-name [_] "controls-view")

    om/IRender
    (render [_]
      (dom/div #js {:className "col-lg-6"} "hi"))))
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
