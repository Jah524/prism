(ns examples.lstm.sparse
  (:require
    [clojure.pprint :refer [pprint]]
    [nn.lstm :as lstm]
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

(def dataset-sparse
  [
    {:x [{"A" (float 1)}]                 :y (map float-array [[10]])}
    {:x [{"A" (float 1)} {"A" (float 1)}] :y (map float-array [[10] [100]])}
    {:x [{"A" (float 1)} {"B" (float 1)}] :y (map float-array [[10] [20]])}
    {:x [{"A" (float 1)} {"B" (float 1)} {"C" (float 1)} {"D" (float 1)}]
     :y [(float-array [10]) (float-array [20]) :pass (float-array [200])]}
    {:x [{"A" (float 1)} {"D" (float 1)} {"B" (float 1)} {"C" (float 1)}]
     :y [:pass (float-array [1]) (float-array [20]) (float-array [-1])]}
    ])

(defn sparse-demo
  "Sometimes success with ? lstm units"
  []
  (let [model (train (lstm/init-lstm-model {:model-type :stack
                                            :input-type :sparse
                                            :layers [{:unit-num 1 :layer-type :input}
                                                     {:unit-type  :lstm :unit-num 10 :layer-type :hidden}
                                                     {:unit-num 1 :activate-fn :linear  :layer-type :output}]}
                                           ["A" "B" "C" "D"])
                     dataset-sparse
                     sum-of-squares-error
                     {:loss-interval 100
                      :epoc 1000
                      :label "sparse demo"})
        demo-input1 [{"A" (float 1)}]
        demo-input2 [{"A" (float 1)} {"A" (float 1)}]
        demo-input3 [{"A" (float 1)} {"B" (float 1)} {"C" (float 1)} {"D" (float 1)}]
        demo-input4 [{"A" (float 1)} {"D" (float 1)} {"B" (float 1)}]
        demo-input5 [{"A" (float 1)} {"D" (float 1)} {"B" (float 1)} {"C" (float 1)}]]
    (println "*** dataset ***")
    (pprint dataset-sparse)
    (println "\n*** demo1 ***")
    (println demo-input1)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input1)))))
    (println "\n*** demo2 ***")
    (println demo-input2)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input2)))))
    (println "\n*** demo3 ***")
    (println demo-input3)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input3)))))
    (println "\n*** demo4 ***")
    (println demo-input4)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input4)))))
    (println "\n*** demo5 ***")
    (println demo-input5)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input5)))))
    (println)
    ))

(defn -main []
  (sparse-demo))
