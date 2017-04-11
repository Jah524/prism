(ns examples.lstm.simple-prediction
  (:require
    [prism.nn.lstm :as lstm]
    [clojure.pprint :refer [pprint]]
    [clj-time.local  :as l]))

(defn sum-of-squares-error
  [model training-list]
  (loop [training-list training-list, acc 0]
    (let [{training-x-seq :x training-y-seq :y} (first training-list)
          training-y-seq-keys (map #(if (= :skip %) :skip (keys %)) training-y-seq)]
      (if (and training-x-seq training-y-seq)
        (let [error (->> (mapv #(if (= :skip %2)
                                  0
                                  (let [it1 (-> %1 :activation :output vals first)
                                        it2 (-> %2 vals first)]
                                    (* 0.5 (- it1 it2) (- it1 it2))))
                               (lstm/sequential-output model training-x-seq training-y-seq-keys)
                               training-y-seq)
                         (reduce +))]
          (recur (rest training-list) (+ error acc)))
        acc))))

(defn train-sgd [model training-list learning-rate & [lstm-option]]
  (loop [model model, training-list training-list]
    (if-let [training-pair (first training-list)]
      (let [{x-seq :x y-seq :y} training-pair
            delta-list (lstm/bptt model
                                  x-seq
                                  y-seq
                                  lstm-option)]
        (recur (lstm/update-model! model delta-list learning-rate) (rest training-list)))
      model)))


(defn train-with-demo-dataset [model training-list loss-fn lstm-option]
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
            (println (apply str "[ Simple Prediction Model ]: "
                            (->> (map #(:unit-num %) (:layers model))
                                 (cons (count (first (:x (first training-list)))))
                                 (interpose " => ")))))
          (recur updated-model  (inc e)))
        model))))

;;;; ;;;; ;;;; ;;;; ;;;; ;;;; ;;;;

(def dataset
  [
    {:x (map float-array [[1]])                 :y [{"prediction" 10}]}
    {:x (map float-array [[1] [1] [1]])         :y [{"prediction" 10} {"prediction" 20} {"prediction" 30}]}
    {:x (map float-array [[1] [2]])             :y [{"prediction" 10} {"prediction" 40}]}
    {:x (map float-array [[1] [1] [1] [1] [1]]) :y [:skip :skip {"prediction" 30} :skip {"prediction" 50}]}
    ])


(defn demo
  "captures demo model with 2 lstm units"
  []
  (let [model (train-with-demo-dataset (lstm/init-model nil #{"prediction"} :dense 1 2 :prediction)
                                       dataset
                                       sum-of-squares-error
                                       {:loss-interval 200
                                        :epoc 2000
                                        :label "LSTM demo"})
        demo-input1 (map float-array [[1]])
        demo-input2 (map float-array [[1] [1] [1]])
        demo-input3 (map float-array [[1] [1] [1] [1] [1]])]
    (println "*** dataset ***")
    (pprint dataset)
    (println "\n*** demo1 ***")
    (pprint demo-input1)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input1 [#{"prediction"}])))))
    (println "\n*** demo2 ***")
    (pprint demo-input2)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input2 [:skip :skip #{"prediction"}])))))
    (println "\n*** demo3 ***")
    (pprint demo-input3)
    (pprint (last (:activation (last (lstm/sequential-output model demo-input3 [:skip :skip :skip :skip #{"prediction"}])))))
    (println)))


(defn -main []
  (demo))
