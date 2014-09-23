(ns main.pomodoro
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [put! <! >! chan]]
            [clojure.string :as string]
            [cljs-time.format :refer [date-formatters]]))

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

(defn time-diff [start end] (- end start))

(defn expired? [stime etime] (<= (time-diff stime etime) 0))

(defn dec-time [t] (- t 1e3))

(defmulti pretty-date (fn [d] (type d)))
(defmethod pretty-date js/Number [t] (pretty-date (->date t)))
(defmethod pretty-date js/Date [d]
  (let [doy ((date-formatters "EEEE") d)
        month ((date-formatters "MMMM") d)
        day ((date-formatters "dd") d)]
    (str doy ", " month " " day)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sound
(def ^:constant sound-src "/sounds/bell.mp3")
(def sound (js/Audio. sound-src))
(defn play-sound [] (do (set! (.-src sound) sound-src) (.play sound)))

(def ^:constant images ["bg2.jpg"
                        "bg3.jpg"])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; History
(def history (atom []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App Setup

(defn time-map [time]
  "Generates a map with start/end/running time keys"
  (let [now (now)]
    {:time time
     :stime now
     :etime (+ time now)}))

(defn default-state
  ([]
   (default-state (:twenty-five presets)))

  ([time]
     (let [tmap (time-map time)]
       (merge tmap {:today (get-in tmap [:stime])
                    :bg (first images)
                    :on? false}))))

(def state (atom (default-state)))
(def bg-chan (chan))
(def end-chan (chan))

(defn get-state
  ([key & keys]
     (map get-state (conj keys key)))
  ([key]
     (get-in @state [key])))

(defn set-state!
  ([val-map]
     (swap! state merge val-map))

  ([key val]
     (swap! state assoc key val)))

(defn set-time! [time]
  (set-state! (time-map time)))

(defn next-time [time]
  (if (= (:twenty-five presets) time)
    (:five presets)
    (:twenty-five presets)))

(defn pomodoro? [five twenty-five]
  (and (= (presets :twenty-five) twenty-five)
       (= (presets :five) five)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timer logic
(def ^:constant time-speed 1)

(def timer-chan
  (let [c (chan)
        timer (js/setInterval
               #(when (get-state :on?) (put! c true))
               time-speed)]
    c))

(go (while true
      (<! timer-chan)
      (when-not (apply expired? (get-state :stime :etime))
        (set-state! :etime (dec-time (get-state :etime))))))


(add-watch state :end
           (fn [k r os ns]
             (when (and (get-state :on?) (apply expired? (get-state :stime :etime)))
               (put! end-chan true))))

(go (while true
      (<! end-chan)
      (>! bg-chan true)
      (play-sound) ;; ding!
      (set-state! (default-state (next-time (get-state :time)))))) ;; set next pomodoro session

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Background logic
(defn set-bg! [bg]
  (let [img (nth images (rand-int 2))]
    (set-state! :bg img)))

(go (while true
      (<! bg-chan)
      (swap! history conj (get-state :time))
      (when (apply pomodoro? (take-last 2 @history))
        (set-bg! (get-state :bg)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generic Views
(defn btn-lg
  "Creates a large button"
  ([body] (btn-lg {} body))

  ([{:keys [on-click disabled?]} & body]
     [:button {:class "btn btn-lg"
               :on-click on-click} body]))

(defn icon [name & body]
  [:i {:class (str "fa fa-" name)} body])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timer View
(defn time-view []
  [:div {:class "col-md-3 col-md-offset-4"}
   [:h1 (display-time (apply time-diff
                             (map get-state (list :stime :etime))))]])

(defn controls-view []
  (let [on25 #(when-not (get-state :on?) (set-time! (presets :twenty-five)))
        on5 #(when-not (get-state :on?) (set-time! (presets :five)))
        on-toggle #(set-state! :on? (not (get-state :on?)))
        on-reset #(when-not (get-state :on?)
                    (set-state! (default-state (get-state :time))))]

   [:div {:class "col-md-2 buttons"}
    [btn-lg {:on-click on25} 25]
    [btn-lg {:on-click on5} 5]
    [:br]
    ;; TODO togglify
    [btn-lg {:on-click on-toggle} [icon (if (get-state :on?) "pause" "play")]]
    [btn-lg {:on-click on-reset} [icon "refresh"]]]))

(defn timer-view []
  [:div {:class "row v-center"}
   [time-view]
   [controls-view]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Today view
(defn today-view []
  [:div {:class "col-md-12 text-center"}
   [:h2 (pretty-date (get-state :today))]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Style View
(defn bg->style [val] (str "background: url(/images/" val ") center center no-repeat; background-size: cover;"))

(defn bg-view []
  [:style (str "body {" (bg->style (get-state :bg)) "}")])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rendering
(reagent/render-component [timer-view] (. js/document (getElementById "timer")))
(reagent/render-component [today-view] (. js/document (getElementById "today")))
(reagent/render-component [bg-view] (. js/document (getElementById "style")))
