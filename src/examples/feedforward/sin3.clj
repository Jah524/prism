(ns examples.feedforward.sin3
  (:require [clj-time.local  :as l]
            [incanter.core :refer [view]]
            [incanter.charts :refer [function-plot add-function set-stroke-color]]
            [prism.nn.feedforward :as ff]))



(def training-sin3 (map (fn [x] {:x (float-array [x]) :y {"sin-prediction" (Math/sin x)}}) (range -3.0 3.0 0.1)))


(defn train-sgd [model training-list learning-rate & [option]]
  (loop [model model, training-list training-list]
    (if-let [training-pair (first training-list)]
      (let [{x :x y :y} training-pair
            delta-list (ff/back-propagation model x y option)]
        (recur (ff/update-model! model delta-list learning-rate) (rest training-list)))
      model)))

(defn sum-of-squares-error
  [model training-list]
  (loop [training-list training-list, acc 0]
    (let [{training-x :x training-y :y} (first training-list)]
      (if (and training-x training-y)
        (let [output (:output (:activation (ff/network-output model training-x (keys training-y))))
              o (get output "sin-prediction")
              t (get training-y "sin-prediction")
              error (* 0.5 (- o t) (- o t))]
          (recur (rest training-list) (+ error acc)))
        acc))))

(defn train [model training-pair-list & [option]]
  (let [{:keys [optimizer learning-rate epoc loss-interval label label-interval]
         :or {optimizer :sgd learning-rate 0.1 epoc 10000 loss-interval 1000}} option]
    (loop [model model, e 0]
      (if (<= e epoc)
        (let [opt (condp = optimizer
                    :sgd train-sgd)
              updated-model (opt model (shuffle training-pair-list) learning-rate option)]
          (when (= 0 (rem e loss-interval))
            (let [error (sum-of-squares-error updated-model training-pair-list)]
              (println (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] epoc: " e
                            ", optimizer: " (.toUpperCase (name optimizer))
                            ", learning-rate: " learning-rate ", error: " error))))
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
