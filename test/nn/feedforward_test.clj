(ns nn.feedforward-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.core.matrix :refer [mget array matrix ecount]]
            [prism.nn.feedforward :refer :all]))


(def sample-model ;1->3->1
  {:input-size 1
   :hidden-size 3
   :input-type  :dense
   :output-type :prediction
   :hidden {:w (matrix [[1] [1] [1]])
            :bias (array [1 1 1])
            :activation :sigmoid}
   :output {"prediction" {:w (array (take 3 (repeat 0.1)))
                          :bias (array [1])}}})


(def sample-model2 ;3->2->3
  {:input-type  :dense
   :output-type :prediction
   :input-size 3
   :hidden-size 2
   :hidden {:w (matrix [[0.1 0.1 0.1] [0.2 0.2 0.2]])
            :bias (array [1 1])
            :activation :sigmoid}
   :output {"prediction1" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction2" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction3" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}}})


(def sample-model2:sparse ;3->2->3
  {:input-type  :sparse
   :output-type :prediction
   :input-size 3
   :hidden-size 2
   :hidden {:sparses {"natural" (array [0.1 0.2]) "language" (array [0.1 0.2]) "processing" (array [0.1 0.2])}
            :bias (array [1 1])
            :activation :sigmoid}
   :output {"prediction1" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction2" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction3" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}}})

(deftest feedforward-test
  (testing "hidden-state-by-sparse"
    (are [arg expect] (= (vec (hidden-state-by-sparse
                                {:hidden {:sparses {"natural"    (array [0 1])
                                                    "language"   (array [2 3])
                                                    "processing" (array [4 5])}}}
                                arg
                                (array (repeat 2 0))))
                         expect)
         {"natural"    1.0} [0.0 1.0]
         {"natural"   -2.0} [0.0 -2.0]
         {"language"   1.0} [2.0 3.0]
         {"language"  -2.0} [-4.0 -6.0]
         {"processing" 1.0} [4.0 5.0]
         {"natural" 2 "processing" -1} [-4.0 -3.0]))

  (testing "forward with dense input"
    (let [result (forward sample-model (array [2]) #{"prediction"})
          {:keys [activation state]} result
          {:keys [input hidden output]} activation]
      (is (= (vec input) [(float 2)]))
      (is (= (vec hidden) (map double (take 3 (repeat 0.9525741268224334)))))
      (is (= (vec (:hidden state)) [3.0 3.0 3.0]))
      (is (= (reduce (fn[acc [k v]](assoc acc k (float v))) {} output) {"prediction" (float 1.2857722491025925)}))))

  (testing "forward with sparse input"
    (let [result (forward sample-model2:sparse {"language" 1} #{"prediction1" "prediction3"})
          {:keys [activation state]} result
          {:keys [input hidden output]} activation]
      (is (= input {"language" 1}))
      (is (= (vec hidden) (map double [0.7502601055951177 0.7685247834990175])))
      (is (= (mapv float (:hidden state)) (map float [1.1 1.2])))
      (is (= (reduce (fn [acc [k v]](assoc acc k (float v))) {} output)
             {"prediction1" (float 2.518785) "prediction3" (float 2.518785)})))

    (let [a (:activation (forward sample-model2 (float-array [0 1 0]) #{"prediction1" "prediction3"}))
          b (:activation (forward sample-model2:sparse {"language" 1} #{"prediction1" "prediction3"}))]
      (is (= (:output a) (:output b)))))


  (testing "param-delta"
    (let [r (param-delta (float-array (range 4)) (float-array (range 4)))]
      (is (= (map vec (:w-delta r)) [[0.0 0.0 0.0 0.0], [0.0 1.0 2.0 3.0], [0.0 2.0 4.0 6.0], [0.0 3.0 6.0 9.0]]))
      (is (= (vec (:bias-delta r)) [0.0 1.0 2.0 3.0]))))

  (testing "back-propagation with dense"
    (let [{:keys [param-loss loss]} (back-propagation sample-model
                                                      (forward sample-model (float-array [2]) #{"prediction"})
                                                      {"prediction" 2})
          {:keys [output-delta hidden-delta]} param-loss
          {hidden-w-delta :w-delta hidden-bias-delta :bias-delta} hidden-delta
          {output-w-delta :w-delta output-bias-delta :bias-delta} (get output-delta "prediction")]
      (is (= loss {"prediction" (double 0.71422776195327)}))
      (is (= (map #(map float %) hidden-w-delta)
             (take 3 (repeat [(float 0.006453285)]))))
      (is (= (vec hidden-bias-delta)
             (take 3 (repeat (double 0.0032266423892525057)))))
      (is (= (vec output-w-delta)
             (take 3 (repeat (double 0.6803548866949769)))))
      (is (= (vec output-bias-delta)
             [(double 0.71422776195327)]))))

  (testing "back-propagation with sparse vector"
    (let [{:keys [param-loss loss]} (back-propagation sample-model2:sparse
                                                      (forward sample-model2:sparse {"language" 1}  #{"prediction1" "prediction2" "prediction3"})
                                                      {"prediction1" 2 "prediction2" 1 "prediction3" 2})
          {:keys [output-delta hidden-delta]} param-loss
          {w-delta1 :w-delta bias-delta1 :bias-delta} (get output-delta "prediction1")
          {w-delta2 :w-delta bias-delta2 :bias-delta} (get output-delta "prediction2")
          {w-delta3 :w-delta bias-delta3 :bias-delta} (get output-delta "prediction3")]
      (is (= loss {"prediction1" (double -0.5187848890941353), "prediction2" (double -1.5187848890941353), "prediction3" (double -0.5187848890941353)}))
      (is (= (vec (get (:sparses-delta hidden-delta) "language"))
             (map double [-0.4789838750697536 -0.4547612903459302])))
      (is (= (vec (:bias-delta hidden-delta))
             (map double [-0.4789838750697536 -0.4547612903459302])))
      (is (= (map float w-delta1)
             (map float w-delta3)
             (map float [-0.3892236 -0.39869905])))
      (is (= (vec w-delta2) (map double [-1.1394837112680352 -1.1672238280726497])))
      (is (= (map float bias-delta1)
             (map float bias-delta3)
             (map float [-0.5187848890941353])))
      (is (= (vec bias-delta2) (map double [-1.5187848890941353])))
      (let [a (:param-loss (back-propagation sample-model2:sparse
                                             (forward sample-model2:sparse {"language" 1} #{"prediction1" "prediction2" "prediction3"})
                                             {"prediction1" 2 "prediction2" 1 "prediction3" 2}))
            b (:param-loss (back-propagation sample-model2
                                             (forward sample-model2 (float-array [0 1 0]) #{"prediction1" "prediction2" "prediction3"})
                                             {"prediction1" 2 "prediction2" 1 "prediction3" 2}))]
        (is (= (vec (get (:sparses-delta (:hidden-delta a)) "language"))
               (let [it (:w-delta (:hidden-delta b))]
                 [(mget it 0 1) (mget it 1 1)])))
        (is (= (vec (:bias-delta (:hidden-delta a)))
               (vec (:bias-delta (:hidden-delta b)))))
        (is (= (vec (:w-delta (:output-delta a)))
               (vec (:w-delta (:output-delta b)))))1
        (is (= (vec (:bias-delta (:output-delta a)))
               (vec (:bias-delta (:output-delta b))))))))

  (testing "update-model!"
    (let [{:keys [hidden output]} (update-model! sample-model
                                                 {:hidden-delta {:w-delta (matrix [[0.1] [0.2] [0.3]])
                                                                 :bias-delta (array [0.1 0.2 0.3])}
                                                  :output-delta {"prediction" {:w-delta (array [2 2 2])
                                                                               :bias-delta (array [2])}}}
                                                 0.1)]
      (is (= (:activation hidden) :sigmoid))
      (is (= (map vec (:w hidden)) (->> [1.01 1.02 1.03] (map double) (map (fn[x] [x])))))
      (is (= (vec (:bias hidden)) (map double [1.01 1.02 1.03])))
      (let [{:keys [w bias]} (get output "prediction")]
        (is (= (mapv float w) (map float [0.3 0.3 0.3])))
        (is (= (mapv float bias) [(float 1.2)]))))
    (let [{:keys [hidden output]} (update-model! sample-model2:sparse
                                                 {:hidden-delta {:sparses-delta {"natural" (array [0.1 0.1])
                                                                                 "language" (array [0.1 0.1])
                                                                                 "processing" (array [0.1 0.1])}
                                                                 :bias-delta (array [0.1 0.1])}
                                                  :output-delta {"prediction1" {:w-delta (array (take 2 (repeat 0.2)))
                                                                                :bias-delta (array [0.2])}
                                                                 "prediction2" {:w-delta (array (take 2 (repeat 0.2)))
                                                                                :bias-delta (array [0.2])}
                                                                 "prediction3" {:w-delta (array (take 2 (repeat 0.2)))
                                                                                :bias-delta (array [0.2])}}}
                                                 0.1)]
      (is (= (:activation hidden) :sigmoid))
      (is (= (mapv float (get (:sparses hidden) "natural")) (map float [0.11 0.21])))
      (is (= (mapv float (get (:sparses hidden) "language")) (map float [0.11 0.21])))
      (is (= (mapv float (get (:sparses hidden) "processing")) (map float [0.11 0.21])))
      (is (= (mapv float (:bias hidden)) (map float (take 2 (repeat 1.01)))))
      (let [{:keys [w bias]} (get output "prediction1")]
        (is (= (mapv float w)
               (map float (take 2 (repeat 1.02)))))
        (is (= (mapv float bias)
               (map float [1.02]))))))

  (testing "init-model"
    (let [{:keys [hidden output input-type output-type hidden-size]}
          (init-model {:input-type :dense
                       :input-items nil
                       :input-size 1
                       :hidden-size 3
                       :output-type :prediction
                       :output-items #{"prediction"}
                       :activation :sigmoid})]
      (is (= output-type :prediction))
      (is (= hidden-size 3))
      (is (= (ecount (:w hidden)) 3))
      (is (= (ecount (:bias hidden)) 3))
      (let [{:keys [w bias]} (get output "prediction")]
        (is (= (ecount w) 3))
        (is (= (ecount  bias) 1)))))

  (testing "init-model with sparse"
    (let [{:keys [hidden output input-type output-type hidden-size]}
          (init-model {:input-items #{"natural" "language" "processing"}
                       :input-size nil
                       :hidden-size 3
                       :output-type :binary-classification
                       :output-items #{"prediction"}
                       :activation :sigmoid})]
      (is (= output-type :binary-classification))
      (is (= hidden-size 3))
      (is (= (ecount (get (:sparses hidden) "natural")) 3))
      (is (= (ecount (get (:sparses hidden) "language")) 3))
      (is (= (ecount (get (:sparses hidden) "processing")) 3))
      (is (= (ecount (get (:sparses hidden) "?")) 1))
      (is (= (ecount (:bias hidden)) 3))
      (let [{:keys [w bias]} (get output "prediction")]
        (is (= (ecount w) 3))
        (is (= (ecount bias) 1)))))
  )
