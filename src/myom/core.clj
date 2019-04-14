(ns myom.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(set! *warn-on-reflection* true)

(deftype complex [^double r ^double i])

(def ^:const WIDTH  512)
(def ^:const HEIGHT 512)

; arbitrary divergence threshold
(def ^:const THRESHOLD 4.0)
(def ^:const ITERATIONS 40)

(defn setup []

  (q/frame-rate 1)

  {:bottom -1.5
   :left   -2.5
   :top     1.5
   :right   1.5})

(defn update-state [state]
  state)


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

(defn iterations-before-divergence [^complex c]
  (loop [i 0
         z (complex. 0 0)]
    ;; (println "(diverges loop: " i z c)
    (if (= i ITERATIONS)
      i
      (if (> (magnitude z) THRESHOLD)
        i
        (recur (inc i) (step z c))))))

(defn get-pixel-colour [bottom top left right x y]
  ;; (println "(get-pixel-colour " bottom top left right x y ")")
  (let [width (- right left)
        height (- top bottom)
        wstep (/ width WIDTH)
        hstep (/ height HEIGHT)
        a (+ left (* x wstep))
        b (- top (* y hstep))]
    (let [i (iterations-before-divergence (complex. a b))
          intensity (* i (/ 255 ITERATIONS))]
      [intensity intensity intensity])))

(defn draw-state [state]
  (let [start (System/currentTimeMillis)
        i (q/create-image WIDTH HEIGHT :rgb)
        {:keys [bottom left top right]} state]
    (dotimes [x WIDTH]
      (dotimes [y HEIGHT]
               (q/set-pixel i x y (apply q/color (get-pixel-colour bottom top left right x y)))))
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
