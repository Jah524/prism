(ns matrix.native
  (:require
    [uncomplicate.neanderthal.core :as c]
    [uncomplicate.neanderthal.native :refer [dv dge dtr]]))

(defn sum []:fixme)

(defn plus
  ([v1]
   v1)
  ([v1 v2]
   (c/axpy v1 v2))
  ([v1 v2 & more]
   (reduce #(plus %1 %2) (plus v1 v2) more)))

(defn merger!
  [m m!]
  (c/axpy! m m!))

(defn minus
  ([v1 v2]
   (c/axpy (double -1) v2 v1))
  ([v1 v2 & more]
   (reduce #(minus %1 %2) (minus v1 v2) more)))

(defn scal
  [^double a v]
  (c/scal a v))

(defn times
  "element-wise multiplication"
  ([v1 v2]
   (let [tmp (c/zero v1)]
     (c/copy! v2 tmp)
     (dotimes [x (c/dim v1)]
       (c/scal! (c/entry v1 x) (c/subvector tmp x (int 1))))
     tmp))
  ([v1 v2 & more]
   (reduce #(times %1 %2) (times v1 v2) more)))

(defn times!
  "element-wise multiplication
  vector v2 will be changed"
  ([v1 v2]
   (dotimes [x (c/dim v1)]
     (c/scal! (c/entry v1 x) (c/subvector v2 x (int 1))))
   v2)
  ([v1 v2 & more]
   (reduce #(times! %1 %2) (times! v1 v2) more)))

(defn dot
  [v1 v2]
  (c/dot v1 v2))

(defn outer
  [v1 v2]
  (c/rk v1 v2))

(defn transpose
  [matrix]
  (c/trans matrix))

(defn gemv
  [matrix v]
  (c/mv matrix v))

(defn rewrite-vector!
  [^double alpha v! v2]
  (c/axpy! alpha v2 v!))

(defn rewrite-matrix!
  [^double alpha a! a2]
  (dotimes [i (c/mrows a!)]
    (rewrite-vector! alpha (c/row a! i) (c/row a2 i)))
  a!)

(defn alter-vec
  [v f]
  (let [tmp (c/copy v)]
    (dotimes [i (c/dim tmp)]
      (c/alter! tmp i f))
    tmp))

(defn ^double model-rand []
  (double (/ (- (rand 16) 8) 1000)))

(def native-matrix-kit
  {:type :native
   :sum sum
   :plus plus
   :merger! merger!
   :minus minus
   :times times
   :times! times!
   :scal scal
   :dot dot
   :outer outer
   :transpose transpose
   :gemv gemv
   :init-vector (fn [n] (dv (take n (repeatedly model-rand))))
   :init-matrix (fn [input-num hidden-num] (dge hidden-num input-num (vec (take (* input-num hidden-num) (repeatedly model-rand)))))
   :make-vector dv
   :make-matrix (fn [input-num hidden-num v] (dge hidden-num input-num v))
   :rewrite-vector! rewrite-vector!
   :rewrite-matrix! rewrite-matrix!
   :sigmoid (fn ^double [^double x] (/ 1 (+ 1 (Math/exp (-  x)))))
   :sigmoid-derivative (fn ^double [^double x] (let [s (/ 1 (+ 1 (Math/exp (- x))))] (* s (- 1 s))))
   :tanh  (fn ^double [^double x] (Math/tanh x))
   :tanh-derivative (fn ^double [^double x] (let [it (Math/tanh x)] (- 1 (* it it))))
   :linear-derivative-vector (fn [v] (dv (take (c/dim v) (repeat 1))))
   :alter-vec alter-vec})
