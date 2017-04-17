(ns examples.lstm.sparse
  (:require
    [clojure.pprint :refer [pprint]]
    [prism.nn.lstm :as lstm]
    [clj-time.local  :as l]))


(defn train-sgd [model training-list learning-rate]
  (loop [model model,
         training-list training-list,
         n 0,
         acc-loss 0]
    (if-let [training-pair (first training-list)]
      (let [{x-seq :x y-seq :y} training-pair
            forward (lstm/sequential-output model x-seq (map #(if (= :skip %) :skip (keys %)) y-seq))
            delta-list (lstm/bptt model
                                  forward
                                  y-seq)
            diff (aget ^floats (-> delta-list :output-delta (get "prediction") :bias-delta) 0)
            loss (* diff diff 0.5)] ; sum-of-squares-error
        (recur (lstm/update-model! model delta-list learning-rate)
               (rest training-list)
               (inc n)
               (+ acc-loss loss)))
      {:loss acc-loss :model model})))

(defn train-with-demo-dataset [model training-list & [option]]
  (let [{:keys [optimizer learning-rate epoc loss-interval label label-interval]} option]
    (loop [model model, e 0]
      (if (< e epoc)
        (let [{loss :loss updated-model :model} (train-sgd model (shuffle training-list) learning-rate)]
          (when (= 0 (rem e loss-interval))
            (println (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] epoc: " e
                              ", optimizer: SGD"
                              ", learning-rate: " learning-rate ", loss: " loss)))
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
  (let [model (train-with-demo-dataset (lstm/init-model {:input-items #{"A" "B" "C" "D"}
                                                         :output-items #{"prediction"}
                                                         :input-type :sparse
                                                         :inupt-size nil
                                                         :hidden-size 10
                                                         :output-type :prediction})
                                       dataset-sparse
                                       {:loss-interval 100
                                        :epoc 2000
                                        :learning-rate 0.01})
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
