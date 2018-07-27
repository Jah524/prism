(ns prism.optimizer
  (:require
    [clojure.core.matrix :refer [esum sqrt emul! emap! add!]]
    [clojure.core.matrix.operators :as o]))


(defn l2-norm [v]
  (sqrt (esum (o/* v v))))

(defn clip!
  "g: gradients, t: threshould(positive value)"
  [t g]
  (let [tmin (- t)
        n (l2-norm g)]
    (if (>= n t)
      (emul! g (/ t n))
      g)))


(defn sgd!
  [learning-rate model-param! delta]
  (->> (emap! #(* learning-rate %) delta)
       (clip! 1)
       (add! model-param!)))

(defn update-param!
  [optimizer learning-rate model-param! delta]
  (case optimizer
    :sgd (sgd! learning-rate model-param! delta)))
