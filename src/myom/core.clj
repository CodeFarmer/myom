(ns myom.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))


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


(defn cmul [[a b] [c d]]
  [(- (* a c) (* b d)) (+ (* a d) (* b c))])

(defn cadd [[a b] [c d]]
  [(+ a c) (+ b d)])

(defn magnitude [[a b]]
  (Math/sqrt (+ (* a a) (* b b))))

(defn step [z c]
  (cadd (cmul z z) c))

(defn iterations-before-divergence [c]
  (loop [i 0
         z [0 0]]
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
    (let [i (iterations-before-divergence [a b])
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
