(ns examples.feedforward.sin3
  (:require [clj-time.local  :as l]
            [incanter.core :refer [view]]
            [incanter.charts :refer [function-plot add-function set-stroke-color]]
            [prism.nn.feedforward :refer [network-output back-propagation update-model init-model]]))



(def training-sin3 (map (fn[x]{:x (float-array [x]) :y (float-array [(Math/sin x)])}) (range -3 3 0.2)))


(defn train-sgd [model training-list learning-rate & [option]]
  (loop [model model, training-list training-list]
    (if-let [training-pair (first training-list)]
      (let [{x :x y :y} training-pair
;;             x(println x y)
            delta-list (back-propagation model x y option)]
;;         (println ">> " delta-list)
        (recur (update-model model delta-list learning-rate) (rest training-list)))
      model)))

(defn sum-of-squares-error
  [model training-list]
  (loop [training-list training-list, acc 0]
    (let [{training-x :x training-y :y} (first training-list)]
      (if (and training-x training-y)
        (let [output-layer (:activation (last (network-output model training-x)))
              error (->> (mapv #(* 0.5 (- %1 %2) (- %1 %2)) output-layer training-y)
                         (reduce +))]
          (recur (rest training-list) (+ error acc)))
        acc))))

(defn train [model training-pair-list & [option]]
  (let [{:keys [optimizer learning-rate epoc loss-interval label label-interval]
         :or {optimizer :sgd learning-rate 0.1 epoc 1 loss-interval 1000 label-interval 1000}} option]
    (loop [model model, e 0]
      (if (< e epoc)
        (let [opt (condp = optimizer
                    :sgd train-sgd)
              updated-model (opt model (shuffle training-pair-list) learning-rate option)]
          (when (= 0 (rem e loss-interval))
            (let [error (sum-of-squares-error updated-model training-pair-list)]
              (println (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] epoc: " e

                            ", optimizer: " (.toUpperCase (name optimizer))
                            ", learning-rate: " learning-rate ", error: " error))))
          (when (zero? (rem e label-interval))
            (println (or label "Sin Approximation"))
            (println (apply str (->> (map #(:unit-num %) (:layers model))
                                     (cons (/ (count (:w (first (:layers model)))) (count (:bias (first (:layers model))))))
                                     (interpose " => ")))))
          (recur updated-model  (inc e)))
        model))))

(defn example-train []
  (let [model (train (init-model {:layers [{:unit-num 1 :layer-type :input}
                                           {:unit-num 3 :activate-fn :tanh :layer-type :hidden}
                                           {:unit-num 1 :activate-fn :linear :layer-type :output}]})
                     training-sin3
                     {:epoc 20000 :loss-interval 1000 :learning-rate 0.05 :label-interval 5000})]
    (-> (function-plot #(Math/sin %) -3 3)
        (add-function #(first (:activation (last (network-output model (float-array [%]))))) -3 3)
        (add-function #(nth (:activation (second (network-output model (float-array [%])))) 0) -3 3)
        (add-function #(nth (:activation (second (network-output model (float-array [%])))) 1) -3 3)
        (add-function #(nth (:activation (second (network-output model (float-array [%])))) 2) -3 3)
        (set-stroke-color java.awt.Color/gray :dataset 2)
        (set-stroke-color java.awt.Color/gray :dataset 3)
        (set-stroke-color java.awt.Color/gray :dataset 4)
        view)))

(defn -main []
  (example-train)
  (println "check out sin approximation plot with 3 units in hidden layer"))
