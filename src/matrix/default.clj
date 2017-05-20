(ns matrix.default
  (require [clojure.pprint :refer [pprint]]
           [clojure.core.matrix :refer [set-current-implementation array matrix esum dot shape add! emul emul! mmul
                                        matrix? vec? emap emap! outer-product transpose ecount]]
           [clojure.core.matrix.random :refer [sample-normal]]
           [clojure.core.matrix.operators :as o]
           [clojure.core.matrix.linear :refer [svd]]))

(set-current-implementation :vectorz)

(defn plus
  ([v1]
   v1)
  ([v1 v2]
   (o/+ v1 v2))
  ([v1 v2 & more]
   (reduce #(plus %1 %2) (plus v1 v2) more)))

(defn merger!
  "takes matrices or vectors and return sum of each element"
  [m m!]
  (add! m! m))

(defn minus
  ([v1 v2]
   (o/- v1 v2))
  ([^floats v1 ^floats v2 & more]
   (reduce #(minus %1 %2) (minus v1 v2) more)))

(defn scal
  [a v]
  (emap #(* a %) v))

(defn times
  ([v1 v2]
   (emul v1 v2))
  ([v1 v2 & more]
   (reduce #(times %1 %2) (times v1 v2) more)))

(defn outer
  [v1 v2]
  (outer-product v1 v2))

(defn clip!
  "v: vector, t: threshould(positive value)"
  [t v]
  (let [tmin (- t)]
    (emap! #(cond (> % t) t (< % tmin) tmin :else %) v)))

(defn rewrite!
  [alpha v! v2]
  (add! v! (emap! #(* alpha %) v2)))

(defn sigmoid [x] (float (/ 1 (+ 1 (Math/exp (-  (float x)))))))

(defn tanh [x] (float (Math/tanh (float x))))

(defn alter-vec
  "f should take unboxed value and return unboxed value to work faster"
  [v f]
  (emap f v))

(defn random-array [^Integer n]
  (emap #(* 0.08 %) (sample-normal n)))

(defn orthogonal-init [n]
  (let [{:keys [U]} (svd (->> (random-array (* n n)) (partition n) matrix))]
    U))


(def default-matrix-kit
  {:type :default
   :sum esum
   :plus plus
   :merger! merger!
   :minus minus
   :times times
   :scal scal
   :dot dot
   :outer outer
   :transpose transpose
   :gemv mmul
   :init-vector (fn [n] (array :vectorz (random-array n)))
   :init-matrix (fn [input-num hidden-num] (->> (random-array (* input-num hidden-num)) (partition input-num) matrix))
   :init-orthogonal-matrix orthogonal-init
   :make-vector (fn [v] (array :vectorz v))
   :make-matrix (fn [input-num hidden-num v] (->> v (partition input-num) matrix))
   :clip! clip!
   :rewrite-vector! rewrite!
   :rewrite-matrix! rewrite!
   :exp (fn[x](Math/exp x))
   :sigmoid sigmoid
   :sigmoid-derivative (fn [x] (let [s (sigmoid x)] (float (* s (- 1 s)))))
   :tanh tanh
   :tanh-derivative (fn [x] (let [it (Math/tanh x)] (float (- 1 (* it it)))))
   :linear-derivative-vector (fn [v] (array :vectorz (take (ecount v) (repeat 1))))
   :alter-vec alter-vec})

