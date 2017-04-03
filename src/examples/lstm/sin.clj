(ns examples.lstm.sin
  (:require [nn.lstm :as lstm]
            [clj-time.local  :as l]))


(defn sum-of-squares-error
  [model training-list]
  (loop [training-list training-list, acc 0]
    (let [{training-x-seq :x training-y-seq :y} (first training-list)]
      (if (and training-x-seq training-y-seq)
        (let [last-time-output-layer (last (:activation (last (lstm/sequential-output model training-x-seq))))
              error (->> (mapv #(* 0.5 (- %1 %2) (- %1 %2)) last-time-output-layer (last training-y-seq))
                         (reduce +))]
          (recur (rest training-list) (+ error acc)))
        acc))))

(defn train-sgd [model training-list learning-rate & [lstm-option]]
  (loop [model model, training-list training-list]
    (if-let [training-pair (first training-list)]
      (let [{x-seq :x y-seq :y} training-pair
            delta-list (lstm/bptt model
                                  x-seq
                                  (mapv #(if (= :pass %) :pass (float-array %)) y-seq) lstm-option)]
        (recur (lstm/update-model model delta-list learning-rate) (rest training-list)))
      model)))


(defn train [model training-list loss-fn lstm-option]
  (let [{:keys [optimizer learning-rate epoc loss-interval label label-interval]
         :or {optimizer :sgd learning-rate 0.01 epoc 10000 loss-interval 100 label-interval 1000}} lstm-option]
    (loop [model model, e 0]
      (if (< e epoc)
        (let [opt (condp = optimizer :sgd train-sgd),
              updated-model (opt model (shuffle training-list) learning-rate lstm-option)]
          (when (= 0 (rem e loss-interval))
            (let [error (loss-fn updated-model training-list)]
              (println (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] epoc: " e

                            ", optimizer: " (.toUpperCase (name optimizer))
                            ", learning-rate: " learning-rate ", error: " error))))
          (when (and label (= 0 (rem e label-interval)))
            (println label))
          (when (= 0 (rem e 1000))
            (println (apply str "[ " (.toUpperCase (name (:model-type model))) " Model ]: "
                            (->> (map #(:unit-num %) (:layers model))
                                 (cons (count (first (:x (first training-list)))))
                                 (interpose " => ")))))
          (recur updated-model  (inc e)))
        model))))

;;;; ;;;; ;;;; ;;;; ;;;; ;;;; ;;;;

(defn sin-dataset [seq-num]
  (loop [coll (map #(Math/sin (* 2 %)) (range -3.2 3 0.1)), acc []]
    (if (> (count coll) (dec seq-num))
      (let [x-seq (map (fn [x] [x]) (take seq-num coll))
            y-seq (map (fn [y] [y]) (take seq-num (rest coll)))]
        (recur (rest coll) (cons {:x (map float-array x-seq) :y (map float-array y-seq)} acc)))
      (reverse acc))))


(defn sin-demo
  "20でerrorが0.02ほどまで"
  ([] (sin-demo (lstm/init-lstm-model {:model-type :stack
                                       :layers [{:unit-num 1 :layer-type :input}
                                                {:unit-num 50 :unit-type :lstm :layer-type :hidden}
                                                {:unit-num 1 :activate-fn :linear  :layer-type :output}]})))
  ([model]
   (train model
          (sin-dataset 5)
          sum-of-squares-error
          {:epoc 2000
           :loss-interval 10
           :label "Sin approximation with LSTM (x-seq = 5)"})))


(defn -main []
  (sin-demo))
