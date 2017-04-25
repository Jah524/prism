(ns matrix.native
  (:require
    [uncomplicate.neanderthal.core :as n]
    [uncomplicate.neanderthal.native :refer [fv dv fge dge ftr]]
    [matrix.default :as default]
    [prism.unit :refer [model-rand]]))

(def b (fv (repeat 5 1)))
(def c (fv (repeat 5 0.1)))

(def x (fv (range 0 10 1)))
(def y (fv (range 0 100 10)))
(def z (fv (range 0 0.9 0.1)))

(def t (ftr 4 (range 10) {:order :row  :diag :unit}))
(def a (fge 5 10 (range 50)))


(defn sum
  ([v1]
   v1)
  ([v1 v2]
   (n/axpy v1 v2))
  ([v1 v2 & more]
   (reduce #(sum %1 %2) (sum v1 v2) more)))

(defn minus
  ([v1 v2]
   (n/axpy (float -1) v2 v1))
;;    (let [v3 (n/axpy! -1 (n/copy v2) (n/zero v2))]
;;      (n/axpy v1 v3)))
  ([v1 v2 & more]
   (reduce #(minus %1 %2) (minus v1 v2) more)))

(defn scal
  [a v]
  (n/scal a v))

(defn times
  "element-wise product
  this function need to get faster"
  ([v1 v2]
   (dv (vec (default/times (float-array v1) (float-array v2)))))
  ([v1 v2 & more]
   (reduce #(times %1 %2) (times v1 v2) more)))

(defn dot
  [v1 v2]
  (n/dot v1 v2))

(defn outer
  [v1 v2]
  (n/rk v1 v2))

(defn transpose
  [matrix]
  (n/trans matrix))

(defn gemv
  [matrix v]
  (n/mv matrix v))

(defn rewrite-vector!
  [alpha v! v2]
  (n/axpy! alpha v2 v!))

(def native-matrix-kit
  {:type :native
   :sum sum
   :minus minus
   :times times
   :scal scal
   :dot dot
   :outer outer
   :transpose transpose
   :gemv gemv
   :init-vector (fn [n] (dv (take n (repeatedly model-rand))))
   :init-matrix (fn [r c] (transpose (dge c r (vec (take (* r c) (repeatedly model-rand))))))
   :make-vector dv
   :rewrite-vector! rewrite-vector!})
