;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Canon Fodder - Chris Ford (ThoughtWorks)     ;;
;;                                              ;;
;; http://github.com/ctford/goldberg            ;;
;; http://github.com/overtone/overtone          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns goldberg.variations.canone-alla-quarta
  (:use [overtone.live :exclude [scale bpm run pitch shift sharp flat]]))

(defn play-on# [instrument# notes] 
  (let [play-at# (fn [[timing pitch duration]]
                   (let [id (at timing (instrument# pitch))]
                     (at (+ timing duration) (ctl id :gate 0))))]
    (->> notes (map play-at#) dorun)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Synth                                        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(definst sawish# [freq 440 depth 10]
  (let [envelope (env-gen (perc 0.1 0.9) :action FREE)]
    (*
      envelope
      (sin-osc freq)
      (saw (+ freq (* depth (lf-saw:kr 0.1 0.2)))))))

(definst harps# [freq 440]
  (let [duration 1]
    (* (line:kr 1 1 duration FREE)
       (pluck (* (white-noise) (env-gen (perc 0.001 5) :action FREE)) 1 1 (/ 1 freq) (* duration 2) 0.25))))

;(defn synth# [midi-note] (-> midi-note midi->hz sawish#))
(defn synth# [midi-note] (-> midi-note midi->hz harps#))
(def play# (partial play-on# synth#))

(defn even-melody# [pitches]
  (let [times (reductions + (cons (now) (repeat 400)))
        notes (map vector times pitches)]
    (play# notes)))

;(synth# 55)
;(even-melody# (range 60 73))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scale                                        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sum-n [series n] (reduce + (take n series)))
(defn => [value & fs] (reduce #(%2 %1) value fs))

(defn scale [intervals]
  #(if-not (neg? %)
     (sum-n (cycle intervals) %)
     (=> % - (scale (reverse intervals)) -)))

(def major (scale [2 2 1 2 2 2 1]))
(def blues (scale [3 2 1 1 3 2]))
(def pentatonic (scale [3 2 2 3 2]))
(def diatonic (scale [1]))

(defmacro defs [names values]
  `(do ~@(map
           (fn [name value] `(def ~name ~value))
           names (eval values))))

(defn start-from [base] (partial + base))
(defs [sharp flat] [inc dec])
(defs [C D E F G A B]
  (map
    (comp start-from (start-from 60) major)
    (range)))

;(even-melody# (map (comp A blues) (range 13)))
;(even-melody# (map (comp E flat pentatonic) (range 11)))
;(even-melody# (map (comp G major) (range 15)))
;(G 2)
;(major 2)
;((comp G major) 1) 
;((comp G sharp dorian) 2) 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modes                                        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mode [scale n] (comp #(- % (scale n)) scale (start-from n)))

(defs
  [ionian dorian phrygian lydian mixolydian aeolian locrian]
  (map (partial mode major) (range)))

(def minor aeolian)

;(even-melody#
;  (let [_ -100]
;    (map (comp D major) [0 1 2 0, 0 1 2 0, 2 3 4 _, 2 3 4 _]))
;)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstractions                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bpm [beats] (fn [beat] (-> beat (/ beats) (* 60) (* 1000))))
;((bpm 120) 3)

(defn run [[from & tos]]
  (if-let [to (first tos)]
    (let [up-or-down (if (<= from to)
                       (range from to)
                       (reverse (range (inc to) (inc from))))]
      (concat up-or-down (run tos)))
    [from]))

;(even-melody# (map (comp G major)
;            (run [0 4 -1 0 1 0])
;            ))

(defn accumulate [series] (map (partial sum-n series) (range (count series))))
(def repeats (partial mapcat #(apply repeat %)))
(def runs (partial mapcat run))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Melody                                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def melody 
  (let [theme
          [(repeats [[2 1/4] [1 1/2] [14 1/4] [1 3/2]])
          (runs [[0 -1 3 0] [4] [1 8]])]
        response
          [(repeats [[10 1/4] [1 1/2] [2 1/4] [1 9/4]])
          (runs [[7 -1 0] [0 -3]])]
        development
          [(repeats [[1 1] [11 1/4] [1 1/2] [1 1] [1 3/4] [11 1/4] [1 13/4]])
          (runs [[4] [2 -3] [-1 -2] [0] [3 5] [1] [1 2] [-1 1 -1] [5 0]])]
        interlude 
          [(repeats [[15 1/4] [1 10/4]])
          (runs [[-1 4] [6 -3]])]
        finale 
          [(repeats [[1 3/4] [7 1/4] [1 1/2] [2 1/4] [1 5/4] [11 1/4] [1 6/4] [5 1/2]
                     [1 6/4] [1 1/2] [2 1/4] [1 1] [3 1/4] [1 1/2] [1 1/4] [1 1]])
          (runs [[3 1 7] [0 -1 0] [2 -2 0 -1] [1 -2] [4 1] [6] [0 -2] [1 -2 -1] [4 3 4]])]
        [durations pitches] (map concat theme response development interlude finale)
        timings (map (partial + 1/2) (accumulate durations))]
    (map vector timings pitches durations)))

(def bass
  (let [triples (partial mapcat #(repeat 3 %))
        crotchets-a
          [(repeats [[8 1] [1 10/4]])
          (triples (runs [[-7 -9]]))]
        twiddle 
          [(repeats [[2 1/4] [2 1/2]])
          (runs [[-11 -13] [-11]])]
        crotchets-b
          [(repeats [[9 1]])
          (triples (runs [[-12 -10]]))]
        elaboration
          [(repeats [[1 3/4] [9 1/4] [1 1/2] [1 1] [2 1/4] [3 1/2] [1 1]])
          (runs [[-7] [-12] [-9 -11] [-9 -13 -12] [-14] [-7 -8 -7] [-9 -8] [-5]])]
        busy 
          [(repeats [[2 1/4] [2 1/2] [4 1/4] [4 1/2] [4 1/4] [3 1/2] [1 7/4]])
          (runs [[-12 -10] [-12] [-9 -7 -9 -8 -11 -9 -11] [-9] [-11] [-13]])]
        finale 
          [(repeats [[7 1/4] [1 1/2] [1 3/4] [23 1/4] [2 1/2] [1 3/4]])
          (runs [[-10 -6 -8 -7] [-14] [-9 -6] [-8 -10] [-5] [-12] [-9 -11] [-13]
                 [-10] [-7 -6] [-9] [-11] [-13] [-10 -9 -11 -10] [-13] [-17]])]
        [durations pitches] (map concat crotchets-a twiddle crotchets-b elaboration busy finale)]
    (map vector (accumulate durations) pitches durations)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Accidentals                                  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def accidentals 
  (let [leader
         {[(+ 3 3/4) 3] sharp, [(+ 7 1/2) 3] sharp, [14 -1] flat, [(+ 25 1/4) 3] sharp,
          [(+ 30 1/2) 3] sharp, [40 3] sharp, [(+ 46 3/4) 3] sharp}
        follower
         {[(+ 27 3/4) -4] sharp, [30 -4] sharp, [(+ 34 1/2) -4] sharp, [(+ 38 1/2) -4] sharp,
          [(+ 40 1/4) -4] sharp, [44 -4] sharp, [(+ 47 1/4) -4] sharp}
        bass
         {[8 -9] sharp, [(+ 28 3/4) -11] sharp, [33 -11] sharp, [43 -11] sharp,
          [(+ 45 3/4) -11] sharp}]
    (merge bass leader follower)))

(defn refine [scale targets [timing pitch duration :as note]]
  (if-let [refinement (targets note)] 
    [timing (-> pitch scale refinement) duration]
    [timing (-> pitch scale) duration]))

(defn with-accidentals [scale accidentals] (partial map (partial refine scale accidentals)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Canone alla quarta - Johann Sebastian Bach   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canon [f] (fn [notes] (concat notes (f notes))))

(defs [timing pitch duration] [0 1 2])
(defn skew [k f] (fn [points] (map #(update-in % [k] f) points))) 
(defn shift [point] (fn [points] (map #(->> % (map + point) vec) points)))

(defn simple [wait] (shift [wait 0 0]))
(defn interval [interval] (shift [0 interval 0]))
(def mirror (skew pitch -))
(def crab (skew timing -))
(def table (comp mirror crab))

(defn truncate [n] (partial drop-last n))

(def canone-alla-quarta (canon (comp (interval -3) mirror (truncate 6) (simple 3))))

(defn canon# [start tempo scale]
  (let [in-time (comp (shift [start 0 0]) (skew timing tempo) (skew duration tempo))
        in-key (with-accidentals scale accidentals)
        play-now# (comp play# in-time in-key)]

   (-> bass play-now#)
   (-> melody canone-alla-quarta play-now#)))

;(canon# (now) (bpm 90) (comp G major))
