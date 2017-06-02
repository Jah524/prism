(ns examples.rnn.multi-class
  (:require
    [clojure.pprint :refer [pprint]]
    [clj-time.local  :as l]
    [prism.nn.rnn :as rnn]))


(defn train-sgd [model training-list learning-rate]
  (loop [model model,
         training-list training-list,
         n 0,
         acc-loss 0]
    (if-let [training-pair (first training-list)]
      (let [{x-seq :x y-seq :y} training-pair
            forward (rnn/forward model x-seq (map #(if (= :skip %) :skip (keys %)) y-seq))
            {:keys [loss param-loss]} (rnn/bptt model
                                                forward
                                                y-seq)
            {:strs [Spring Summer Autumn Winter]} (last loss)
            loss (+ (Math/abs Spring) (Math/abs Summer) (Math/abs Autumn) (Math/abs Winter))]
        (recur (rnn/update-model! model param-loss learning-rate)
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
          (recur updated-model  (inc e)))
        model))))

;;;; ;;;; ;;;; ;;;; ;;;; ;;;; ;;;;

(def dataset-season
  [
    {:x [#{"quarter"}]                 :y ["Spring"]}
    {:x [#{"quarter"} #{"quarter"}]    :y [:skip "Summer"]}
    {:x [#{"half"} #{"quarter"}]       :y ["Summer" "Autumn"]}
    {:x [#{"half"} #{"half"}]          :y [:skip "Winter"]}
    {:x [#{"quarter"} #{"half"}]
     :y [:skip "Autumn"]}
    {:x [#{"quarter"} #{"half"} #{"quarter"}]
     :y [:skip :skip "Winter"]}
    {:x [#{"quarter"} #{"quarter"} #{"quarter"} #{"quarter"}]
     :y [:skip :skip :skip "Winter"]}
    {:x [#{"quarter"} #{"quarter"} #{"quarter"} #{"quarter"}]
     :y [:skip :skip :skip "Winter"]}
    ;
    {:x [#{"half"} #{"half"} #{"quarter"} #{"quarter"}]
     :y [:skip :skip :skip "Summer"]}
    {:x [#{"half"} #{"quarter"} #{"quarter"} #{"quarter"}]
     :y [:skip :skip :skip "Spring"]}
    {:x [#{"half"} #{"half"} #{"half"} #{"quarter"}]
     :y [:skip :skip "Summer" "Autumn"]}
    {:x [#{"half"} #{"quarter"} #{"half"} #{"half"}]
     :y [:skip :skip :skip "Autumn"]}
    {:x [#{"quarter"} #{"quarter"} #{"half"} #{"quarter"}]
     :y [:skip :skip :skip "Spring"]}
    {:x [#{"half"} #{"half"} #{"half"} #{"half"} #{"half"}]
     :y [:skip :skip :skip :skip "Summer"]}
    {:x [#{"half"} #{"half"} #{"quarter"} #{"half"} #{"half"}]
     :y [:skip :skip "Spring" :skip "Spring"]}
    ])

(defn multiclass-demo
  [rnn-type]
  (let [model (train-with-demo-dataset (rnn/init-model {:input-items #{"quarter" "half"}
                                                        :output-items #{"Spring" "Summer" "Autumn" "Winter"}
                                                        :inupt-size nil
                                                        :hidden-size 4
                                                        :output-type :multi-class-classification
                                                        :activation :sigmoid ;;works only standard rnn
                                                        :rnn-type rnn-type})
                                       dataset-season
                                       {:loss-interval 500
                                        :epoc 5000
                                        :learning-rate 0.01})
        demo-input1 [#{"half"}]
        demo-input2 [#{"half"} #{"quarter"}]
        demo-input3 [#{"half"} #{"half"} #{"quarter"}]
        demo-input4 [#{"half"} #{"half"} #{"half"}]
        demo-input5 [#{"half"} #{"half"} #{"quarter"} #{"half"} #{"half"}]]
    (println "*** dataset ***")
    (pprint dataset-season)
    (println "\n*** demo1 expects Summer ***")
    (println demo-input1)
    (pprint (:output (:activation (last (rnn/forward model demo-input1 (repeat (count demo-input1) nil))))))
    (println "\n*** demo2 expects Autumn ***")
    (println demo-input2)
    (pprint (:output (:activation (last (rnn/forward model demo-input2 (repeat (count demo-input2) nil))))))
    (println "\n*** demo3 expects Spring ***")
    (println demo-input3)
    (pprint (:output (:activation (last (rnn/forward model demo-input3 (repeat (count demo-input3) nil))))))
    (println "\n*** demo4 expects Summer ***")
    (println demo-input4)
    (pprint (:output (:activation (last (rnn/forward model demo-input4 (repeat (count demo-input4) nil))))))
    (println "\n*** demo5 expects Spring ***")
    (println demo-input5)
    (pprint (:output (:activation (last (rnn/forward model demo-input5 (repeat (count demo-input5) nil))))))
    (println)
    ))

(defn -main [& more]
  (multiclass-demo (keyword (first more))))
