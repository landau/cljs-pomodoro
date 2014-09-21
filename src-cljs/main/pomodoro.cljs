(ns main.pomodoro
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [put! <! chan]]
            [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Debugging
(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constants

(def ^:constant one-min (* 1e3 60))
(def ^:constant presets {:five (* one-min 5)
                         :twenty-five (* one-min 25)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Compatability
;; Must extend number for js/Number since time is represented as milliseconds
(extend-type number
  ICloneable
  (-clone [n] (js/Number. n)))

(extend-type boolean
  ICloneable
  (-clone [b] (js/Boolean. b)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Date conviences
(defn now [] (.now js/Date))
(defn ->date
  "Creates a date object"
  ([] (->date (now)))
  ([t] (js/Date. t)))

(defn format-time
  "Format time as min:sec"
  [d]
  (let [min (.getMinutes d)
        sec (.getSeconds d)
        formatted (map #(if (< % 10) (str "0" %) %) [min sec])]
    (string/join ":" formatted)))

;; Converts numbers to dates to be passed to `format-time`
(defmulti display-time (fn [d] (type d)))
(defmethod display-time js/Number [t] (format-time (->date t)))
(defmethod display-time js/Date [d] (format-time d))

(defn expired? [stime etime] (<= (- etime stime) 0))
(defn sub-sec [t] (- t one-min))

(defn time-diff [start end] (- end start))

(defn can-update [rtime etime on?]
  (and on? (> (time-diff rtime etime) 0)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sound
(def ^:constant sound-src "/sounds/bell.mp3")
(def sound (js/Audio. sound-src))
(defn play-sound [] (do (set! (.-src sound) sound-src) (.play sound)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App Setup

(defn time-map [time]
  "Generates a map with start/end/running time keys"
  (let [now (now)]
    (hash-map :stime now
              :etime (+ time now)
              :rtime now)))

(defn default-state
  ([]
   (default-state (:twenty-five presets)))

  ([time]
     (let [tmap (time-map time)]
       (merge tmap {:today (get-in tmap [:stime])
                    :on? false}))))

(def state (atom (default-state)))

(defn get-state [key]
  (get-in @state [key]))

(defn set-state!
  ([val-map]
     (swap! state merge val-map))

  ([key val]
     (print key val)
     (swap! state assoc key val)))

(defn set-time! [time]
  (set-state! (time-map time)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generic Views
(defn btn-lg
  "Creates a large button"
  ([body] (btn-lg {} body))

  ([{:keys [on-click]} & body]
     [:button {:class "btn btn-lg"
               :on-click on-click} body]))

(defn icon [name & body]
  [:i {:class (str "fa fa-" name)} body])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timer View
(defn time-view []
  [:div {:class "col-md-3 col-md-offset-4"}
   [:h1 (display-time (apply time-diff
                             (map get-state (list :rtime :etime))))]])

(defn controls-view []
  (let [on25 #(when-not (get-state :on?) (set-time! (presets :twenty-five)))
        on5 #(when-not (get-state :on?) (set-time! (presets :five)))
        on-toggle #(set-state! :on? (not (get-state :on?)))
        on-reset #(when-not (get-state :on?)
                    (set-state! (default-state (get-state :stime))))]

   [:div {:class "col-md-2 buttons"}
    [btn-lg {:on-click on25} 25]
    [btn-lg {:on-click on5} 5]
    [:br]
    ;; TODO togglify
    [btn-lg {:on-click on-toggle} [icon "play"]]
    [btn-lg {:on-click on-reset} [icon "refresh"]]]))

(defn timer-view []
  [:div {:class "row v-center"}
   [time-view]
   [controls-view]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Today view
(defn today-view []
  [:div {:class "col-md-12 text-center"}
   [:h2 (get-state :today)]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rendering
(reagent/render-component [timer-view] (. js/document (getElementById "timer")))
(reagent/render-component [today-view] (. js/document (getElementById "today")))
