(ns examples.lstm.sparse
  (:require
    [clojure.pprint :refer [pprint]]
    [prism.nn.lstm :as lstm]
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
            (println (apply str "[ Sparse Model ]: "
                            (->> (map #(:unit-num %) (:layers model))
                                 (cons (count (first (:x (first training-list)))))
                                 (interpose " => ")))))
          (recur updated-model  (inc e)))
        model))))

;;;; ;;;; ;;;; ;;;; ;;;; ;;;; ;;;;

(def dataset-sparse
  [
    {:x [{"A" (float 1)}]                 :y [{"prediction" 10}]}
    {:x [{"A" (float 1)} {"A" (float 1)}] :y [{"prediction" 10} {"prediction" 100}]}
    {:x [{"A" (float 1)} {"B" (float 1)}] :y [{"prediction" 10} {"prediction" 20}]}
    {:x [{"A" (float 1)} {"B" (float 1)} {"C" (float 1)} {"D" (float 1)}]
     :y [{"prediction" 10} {"prediction" 20} :skip {"prediction" 200}]}
    {:x [{"A" (float 1)} {"D" (float 1)} {"B" (float 1)} {"C" (float 1)}]
     :y [:skip {"prediction" 1} {"prediction" 20} {"prediction" -1}]}
    ])

(defn sparse-demo
  "success with 10 lstm units"
  []
  (let [model (train-with-demo-dataset (lstm/init-model  #{"A" "B" "C" "D"} #{"prediction"} :sparse nil 10 :prediction)
                                       dataset-sparse
                                       sum-of-squares-error
                                       {:loss-interval 100
                                        :epoc 2000
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
    (pprint (:output (:activation (last (lstm/sequential-output model demo-input1 [#{"prediction"}])))))
    (println "\n*** demo2 ***")
    (println demo-input2)
    (pprint (:output (:activation (last (lstm/sequential-output model demo-input2 [:skip #{"prediction"}])))))
    (println "\n*** demo3 ***")
    (println demo-input3)
    (pprint (:output (:activation (last (lstm/sequential-output model demo-input3 [:skip :skip :skip #{"prediction"}])))))
    (println "\n*** demo4 ***")
    (println demo-input4)
    (pprint (:output (:activation (last (lstm/sequential-output model demo-input4 [:skip :skip #{"prediction"}])))))
    (println "\n*** demo5 ***")
    (println demo-input5)
    (pprint (:output (:activation (last (lstm/sequential-output model demo-input5 [:skip :skip :skip #{"prediction"}])))))
    (println)
    ))

(defn -main []
  (sparse-demo))
