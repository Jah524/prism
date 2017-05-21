(ns examples.feedforward.sin3
  (:require [clj-time.local  :as l]
            [incanter.core :refer [view]]
            [incanter.charts :refer [function-plot add-function set-stroke-color]]
            [prism.nn.feedforward :as ff]
            [matrix.default :refer [default-matrix-kit]]))



(defn training-sin3
  [matrix-kit]
  (map (fn [x] {:x ((:make-vector matrix-kit) [x])
                :y {"sin-prediction" (Math/sin x)}}) (range -3.0 3.0 0.1)))


(defn train-sgd [model training-list learning-rate]
  (loop [training-list training-list,
         n 0,
         acc-loss 0]
    (if-let [training-pair (first training-list)]
      (let [{x :x y :y} training-pair
            forward (ff/network-output model x (keys y))
            {:keys [param-loss loss]} (ff/back-propagation model forward y)
            diff (get loss "sin-prediction")
            loss (* diff diff 0.5)] ; sum-of-squares-error
        (ff/update-model! model param-loss learning-rate)
        (recur (rest training-list)
               (inc n)
               (+ acc-loss loss)))
      {:loss (/ acc-loss n) :model model})))

(defn train [model training-pair-list & [option]]
  (let [{:keys [learning-rate epoc loss-interval label label-interval]
         :or {learning-rate 0.1 epoc 10000 loss-interval 1000}} option]
    (loop [model model, e 0]
      (if (<= e epoc)
        (let [{loss :loss updated-model :model} (train-sgd model (shuffle training-pair-list) learning-rate)]
          (when (= 0 (rem e loss-interval))
            (println (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] epoc: " e
                          ", learning-rate: " learning-rate ", loss: " loss)))
          (recur updated-model  (inc e)))
        model))))

(defn example-train [matrix-kit]
  (let [mk (or matrix-kit default-matrix-kit)
        model (train (ff/init-model {:input-type :dense
                                     :input-items nil
                                     :input-size 1
                                     :hidden-size 3
                                     :output-type :prediction
                                     :output-items #{"sin-prediction"}
                                     :activation :tanh
                                     :matrix-kit mk
                                     })
                     (training-sin3 mk)
                     {:epoc 10000 :loss-interval 1000 :learning-rate 0.01})]
    (-> (function-plot #(Math/sin %) -3 3)
        (add-function #(get (:output (:activation (ff/network-output model ((:make-vector mk) [%]) #{"sin-prediction"}))) "sin-prediction") -3 3)
        (add-function #(nth (seq (:hidden (:activation (ff/network-output model ((:make-vector mk) [%]) #{"sin-prediction"})))) 0) -3 3)
        (add-function #(nth (seq (:hidden (:activation (ff/network-output model ((:make-vector mk) [%]) #{"sin-prediction"})))) 1) -3 3)
        (add-function #(nth (seq (:hidden (:activation (ff/network-output model ((:make-vector mk) [%]) #{"sin-prediction"})))) 2) -3 3)
        (set-stroke-color java.awt.Color/gray :dataset 2)
        (set-stroke-color java.awt.Color/gray :dataset 3)
        (set-stroke-color java.awt.Color/gray :dataset 4)
        view)))

(defn -main []
  (example-train nil)
  (println "check out sin approximation plot with 3 units in hidden layer"))
