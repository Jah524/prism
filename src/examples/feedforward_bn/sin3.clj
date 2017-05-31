(ns examples.feedforward-bn.sin3
  (:require [clj-time.local  :as l]
            [incanter.core :refer [view]]
            [incanter.charts :refer [function-plot add-function set-stroke-color]]
            [prism.nn.feedforward-bn :as ffbn]
            [matrix.default :refer [default-matrix-kit]]))


(defn training-sin3
  [matrix-kit]
  (map (fn [x] {:x ((:make-vector matrix-kit) [x])
                :y {"sin-prediction" (Math/sin x)}}) (range -3.0 3.0 0.1)))


(defn train-minibatch [model training-list learning-rate batch-size]
  (loop [training-list training-list,
         n 0,
         acc-loss 0]
    (if-let [training-pair (first training-list)]
      (let [mini-batch (take batch-size training-list)
            x-batch (map :x mini-batch)
            y-batch (map :y mini-batch)
            forward (ffbn/network-output-batch model x-batch  (map keys y-batch))
            {:keys [param-loss loss-seq]} (ffbn/back-propagation model forward y-batch)
            diff-seq (->> loss-seq (mapv (fn [loss] (get loss "sin-prediction"))))
            loss-sum (->> diff-seq (mapv (fn [diff] (* diff diff 0.5))) (apply +))] ; sum-of-squares-error
        (ffbn/update-model! model param-loss learning-rate)
        (recur (drop batch-size training-list)
               (inc n)
               (+ acc-loss (/ loss-sum batch-size))))
      {:loss (/ acc-loss n) :model model})))

(defn train [model training-pair-list & [option]]
  (let [{:keys [learning-rate epoc loss-interval label label-interval batch-size]
         :or {learning-rate 0.1 epoc 10000 loss-interval 1000 batch-size (count training-pair-list)}} option]
    (loop [model model, e 0]
      (if (<= e epoc)
        (let [{loss :loss updated-model :model} (train-minibatch model (shuffle training-pair-list) learning-rate batch-size)]
          (when (= 0 (rem e loss-interval))
            (println (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] epoc: " e
                          ", learning-rate: " learning-rate ", loss: " loss)))
          (recur updated-model  (inc e)))
        model))))

(defn example-train [matrix-kit]
  (let [mk (or matrix-kit default-matrix-kit)
        model (train (ffbn/init-model {:input-items nil
                                       :input-size 1
                                       :hidden-size 3
                                       :output-type :prediction
                                       :output-items #{"sin-prediction"}
                                       :activation :tanh
                                       :matrix-kit mk
                                       })
                     (training-sin3 mk)
                     {:epoc 10000 :loss-interval 500 :learning-rate 0.01})
        full-batch (mapv :x (training-sin3 mk))
        {:keys [pop-mean pop-variance]} (ffbn/population model full-batch)]
    (-> (function-plot #(Math/sin %) -3 3)
        (add-function #(get (:output (:activation (ffbn/network-output model pop-mean pop-variance ((:make-vector mk) [%]) #{"sin-prediction"}))) "sin-prediction") -3 3)
        (add-function #(nth (seq (:hidden (:activation (ffbn/network-output model pop-mean pop-variance ((:make-vector mk) [%]) #{"sin-prediction"})))) 0) -3 3)
        (add-function #(nth (seq (:hidden (:activation (ffbn/network-output model pop-mean pop-variance ((:make-vector mk) [%]) #{"sin-prediction"})))) 1) -3 3)
        (add-function #(nth (seq (:hidden (:activation (ffbn/network-output model pop-mean pop-variance ((:make-vector mk) [%]) #{"sin-prediction"})))) 2) -3 3)
        (set-stroke-color java.awt.Color/gray :dataset 2)
        (set-stroke-color java.awt.Color/gray :dataset 3)
        (set-stroke-color java.awt.Color/gray :dataset 4)
        view)))

(defn -main []
  (example-train nil)
  (println "check out sin approximation plot with 3 units in hidden layer"))
