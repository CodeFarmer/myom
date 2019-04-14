(ns myom.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(set! *warn-on-reflection* true)

(deftype complex [^double r ^double i])

(def ^:const WIDTH  800)
(def ^:const HEIGHT 600)

; arbitrary divergence threshold
(def ^:const THRESHOLD 4.0)

;; 10 is visibly not enough at 512x512
(def ^:const ITERATIONS 80)

;; When zooming, expand this portion of the original image to fill
;; smaller is zoomier
(def ^:const ZOOM_RATIO 0.5)

(defn setup []

  (q/frame-rate 0.5)

  {:bottom -1.5
   :left   -2.5
   :top     1.5
   :right   1.5})

(defn update-state [state]
  (if  (q/mouse-pressed?) ;; zoom and recenter
    (let [{:keys [bottom left top right]} state
          slice-width (- right left)
          slice-height (- top bottom)
          mx-abs (/ (q/mouse-x) WIDTH)
          my-abs (/ (q/mouse-y) HEIGHT)
          new-center-x (+ left (* slice-width mx-abs))
          new-center-y (- top (* slice-height my-abs))
          new-slice-width (* ZOOM_RATIO slice-width)
          new-slice-height (* ZOOM_RATIO slice-height)
          new-left (- new-center-x (/ new-slice-width 2))
          new-right (+ new-center-x (/ new-slice-width 2))
          new-top (+ new-center-y (/ new-slice-height 2))
          new-bottom (- new-center-y (/ new-slice-height 2))]

      {:bottom new-bottom
       :left new-left
       :top new-top
       :right new-right})
    state))


(defn cmul [^complex a ^complex b]
  (let [ra (double (.r a))
        ia (double (.i a))
        rb (double (.r b))
        ib (double (.i b))]
    (complex. (- (* ra rb) (* ia ib)) (+ (* ra ib) (* ia rb)))))

(defn cadd [^complex a ^complex b]
  (let [ra (double (.r a))
        ia (double (.i a))
        rb (double (.r b))
        ib (double (.i b))]
    (complex. (+ ra rb) (+ ia ib))))

(defn magnitude [^complex a]
  (Math/sqrt (+ (* (.r a) (.r a)) (* (.i a) (.i a)))))

(defn step [^complex z ^complex c]
  (cadd (cmul z z) c))

(defn iterations-before-divergence [^complex c threshold max-iterations]
  (loop [i 0
         z (complex. 0 0)]
    (if (= i max-iterations)
      i
      (if (> (magnitude z) threshold)
        i
        (recur (inc i) (step z c))))))

(defn get-pixel-colour [bottom top left right x y image-width image-height threshold max-iterations]
  (let [width (- right left)
        height (- top bottom)
        wstep (/ width image-width)
        hstep (/ height image-height)
        a (+ left (* x wstep))
        b (- top (* y hstep))]
    (let [i (iterations-before-divergence (complex. a b) threshold max-iterations)
          intensity (if (= i max-iterations)
                      0
                      (* i (/ 255 max-iterations)))]
      (q/color intensity intensity intensity))))

(defn draw-state [state]
  (println "draw-state " state)
  (let [start (System/currentTimeMillis)
        i (q/create-image WIDTH HEIGHT :rgb)
        {:keys [bottom left top right]} state]
    (dotimes [x WIDTH]
      (dotimes [y HEIGHT]
        (q/set-pixel i x y (get-pixel-colour bottom top left right x y WIDTH HEIGHT THRESHOLD ITERATIONS))))
    (q/image i 0 0)
    (println "render time: " (- (System/currentTimeMillis) start) "ms"))
)


(q/defsketch myom
  :title "You're a Rorschach test on fire"
  :size [WIDTH HEIGHT]
  ; setup function called only once, during sketch initialization.
  :setup setup
  ; update-state is called on each iteration before draw-state.
  :update update-state
  :draw draw-state
  :features [:keep-on-top]
  ; This sketch uses functional-mode middleware.
  ; Check quil wiki for more info about middlewares and particularly
  ; fun-mode.
  :middleware [m/fun-mode])
