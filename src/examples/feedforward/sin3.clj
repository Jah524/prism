(ns examples.feedforward.sin3
  (:require [clj-time.local  :as l]
            [incanter.core :refer [view]]
            [incanter.charts :refer [function-plot add-function set-stroke-color]]
            [prism.nn.feedforward :as ff]))



(def training-sin3 (map (fn [x] {:x (float-array [x]) :y {"sin-prediction" (Math/sin x)}}) (range -3.0 3.0 0.1)))


(defn train-sgd [model training-list learning-rate]
  (loop [model model,
         training-list training-list,
         n 0,
         acc-loss 0]
    (if-let [training-pair (first training-list)]
      (let [{x :x y :y} training-pair
            forward (ff/network-output model x (keys y))
            delta-list (ff/back-propagation model forward y)]
        (let [diff (aget ^floats (-> delta-list :output-delta (get "sin-prediction") :bias-delta) 0)
              loss (* diff diff 0.5)]
          (recur (ff/update-model! model delta-list learning-rate)
                 (rest training-list)
                 (inc n)
                 (+ acc-loss loss))))
      {:loss (/ acc-loss n) :model model})))

(defn train [model training-pair-list & [option]]
  (let [{:keys [optimizer learning-rate epoc loss-interval label label-interval]
         :or {optimizer :sgd learning-rate 0.1 epoc 10000 loss-interval 1000}} option]
    (loop [model model, e 0]
      (if (<= e epoc)
        (let [opt (condp = optimizer
                    :sgd train-sgd)
              {loss :loss updated-model :model} (opt model (shuffle training-pair-list) learning-rate)]
          (when (= 0 (rem e loss-interval))
            (println (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] epoc: " e
                          ", optimizer: " (.toUpperCase (name optimizer))
                          ", learning-rate: " learning-rate ", loss: " loss)))
          (recur updated-model  (inc e)))
        model))))

(defn example-train []
  (let [model (train (ff/init-model {:input-type :dense
                                     :input-items nil
                                     :input-size 1
                                     :hidden-size 3
                                     :output-type :prediction
                                     :output-items #{"sin-prediction"}
                                     :activation :tanh})
                     training-sin3
                     {:epoc 10000 :loss-interval 1000 :learning-rate 0.05})]
    (-> (function-plot #(Math/sin %) -3 3)
        (add-function #(get (:output (:activation (ff/network-output model (float-array [%]) #{"sin-prediction"}))) "sin-prediction") -3 3)
        (add-function #(nth (:hidden (:activation (ff/network-output model (float-array [%]) #{"sin-prediction"}))) 0) -3 3)
        (add-function #(nth (:hidden (:activation (ff/network-output model (float-array [%]) #{"sin-prediction"}))) 1) -3 3)
        (add-function #(nth (:hidden (:activation (ff/network-output model (float-array [%]) #{"sin-prediction"}))) 2) -3 3)
        (set-stroke-color java.awt.Color/gray :dataset 2)
        (set-stroke-color java.awt.Color/gray :dataset 3)
        (set-stroke-color java.awt.Color/gray :dataset 4)
        view)))

(defn -main []
  (example-train)
  (println "check out sin approximation plot with 3 units in hidden layer"))
