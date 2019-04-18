(ns myom.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(set! *warn-on-reflection* true)

(deftype complex [^double r ^double i])

(def ^:const WIDTH  800)
(def ^:const HEIGHT 600)

(def ^:const FRAME_RATE 10)
; arbitrary divergence threshold
(def ^:const THRESHOLD 4.0)

;; 10 is visibly not enough at 512x512
(def ^:const START_ITERATIONS 200)

;; When zooming, expand this portion of the original image to fill
;; smaller is zoomier
(def ^:const ZOOM_RATIO 0.5)


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

(defn iterations-before-divergence

  ([^complex z ^complex c threshold max-iterations]
   (iterations-before-divergence 0 z c threshold max-iterations))
  
  ([i ^complex z ^complex c threshold max-iterations]
   (if (= i max-iterations)
     0
     (if (> (magnitude z) threshold)
       i
       (recur (inc i) (step z c) c threshold max-iterations)))))

(defn pixel-coords
  "Get the point in the complex plane specified by the pixel (x, y)"
  [bottom top left right x y image-width image-height]
  (let [width (- right left)
        height (- top bottom)
        wstep (/ width image-width)
        hstep (/ height image-height)]
    (complex. (+ left (* x wstep)) (- top (* y hstep)))))

(defn get-pixel-colour-mandelbrot [bottom top left right x y image-width image-height threshold max-iterations]
  (let [i (iterations-before-divergence (complex. 0 0) (pixel-coords bottom top left right x y image-width image-height) threshold max-iterations)
        intensity (* i (/ 255 max-iterations))]
    (q/color intensity intensity intensity)))

(defn get-pixel-colour-julia [c bottom top left right x y image-width image-height threshold max-iterations]
  (let [i (iterations-before-divergence (pixel-coords bottom top left right x y image-width image-height) c threshold max-iterations)
        intensity (* i (/ 255 max-iterations))]
    (q/color intensity intensity intensity)))

;; Quil sketch callbacks

(defn setup []

  (q/frame-rate FRAME_RATE)

  {:view {:bottom -1.5
          :left   -2.5
          :top     1.5
          :right   1.5}

   :iterations-buffer (apply (partial vector-of :int) (repeat (* WIDTH HEIGHT) 0))

   :iterations START_ITERATIONS})


(defn update-state [state]

  (if (q/key-pressed?)
    (case (q/key-as-keyword)
      :s (let [filename (str "myom-" (System/currentTimeMillis) ".png")]
           (println "Saving " filename)
           (q/save filename)
           state)
      :+ (assoc state :iterations (* (:iterations state) 2))
      :- (assoc state :iterations (/ (:iterations state) 2))
      
      (do (println "Key pressed: " (q/key-as-keyword))
          state))
    
    (if  (q/mouse-pressed?) ;; zoom and recenter
      (let [{:keys [bottom left top right]} (:view state)
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
        
        (merge state {:view {:bottom new-bottom
                             :left new-left
                             :top new-top
                             :right new-right}
                      
                      :prev-state (dissoc state :prev-state)}))
      
      (assoc state :prev-state (dissoc state :prev-state)))))


(defn draw-state [pixel-renderer state]
  (println "draw-state " (:view state))
  (let [start (System/currentTimeMillis)
        i (q/create-image WIDTH HEIGHT :rgb)
        {:keys [bottom left top right]} (:view state)]

    (if (not (= (:prev-state state) (dissoc state :prev-state)))
      (do (dotimes [x WIDTH]
            (dotimes [y HEIGHT]
              (q/set-pixel i x y (pixel-renderer bottom top left right x y WIDTH HEIGHT THRESHOLD (:iterations state)))))
          (q/image i 0 0)))
    (println "render time: " (- (System/currentTimeMillis) start) "ms"))
)


(def julia-draw (partial draw-state (partial get-pixel-colour-julia (complex. -0.35, 0.65))))
(def mandelbrot-draw (partial draw-state get-pixel-colour-mandelbrot))

(q/defsketch myom
  :title "You're a Rorschach test on fire"
  :size [WIDTH HEIGHT]
  ; setup function called only once, during sketch initialization.
  :setup setup
  ; update-state is called on each iteration before draw-state.
  :update update-state
  :draw mandelbrot-draw
  :features [:keep-on-top]
  ; This sketch uses functional-mode middleware.
  ; Check quil wiki for more info about middlewares and particularly
  ; fun-mode.
  :middleware [m/fun-mode])
