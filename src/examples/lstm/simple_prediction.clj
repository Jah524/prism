(ns examples.lstm.simple-prediction
  (:require
    [nn.lstm :as lstm]
    [clojure.pprint :refer [pprint]]
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

(def dataset
  [
    {:x (map float-array [[1]])         :y (map float-array [[10]])}
    {:x (map float-array [[1] [1] [1]]) :y (map float-array [[10] [20] [30]])}
    {:x (map float-array [[1] [2]])     :y (map float-array [[10] [40]])}
    {:x (map float-array [[1] [1] [1] [1] [1]]) :y [:pass :pass (float-array [30]) :pass (float-array [50])]}
    ])


(defn demo
  "Sometimes success with ? lstm units"
  []
  (let [model (train (lstm/init-lstm-model {:model-type :deep ;or :stack nil (for single hidden layer)
                                            :layers [{:unit-num 1 :layer-type :input}
                                                     {:unit-type  :lstm :unit-num 6 :layer-type :hidden}
                                                     {:unit-num 1 :activate-fn :linear  :layer-type :output}]})
                     dataset
                     sum-of-squares-error
                     {:loss-interval 100
                      :epoc 2000
                      :label "LSTM demo"})
        demo-input1 (map float-array [[1]])
        demo-input2 (map float-array [[1] [1] [1]])
        demo-input3 (map float-array [[1] [1] [1] [1] [1]])]
    (println "*** dataset ***")
    (pprint dataset)
    (println "\n*** demo1 ***")
    (pprint demo-input1)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input1)))))
    (println "\n*** demo2 ***")
    (pprint demo-input2)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input2)))))
    (println "\n*** demo3 ***")
    (pprint demo-input3)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input3)))))
    (println)))



(defn -main []
  (demo))
