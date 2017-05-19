(ns nn.feedforward-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.core.matrix :refer [mget array matrix ecount]]
            [matrix.default :refer [default-matrix-kit]]
            [prism.nn.feedforward :refer :all]))

(def sample-model ;1->3->1
  {:matrix-kit default-matrix-kit
   :input-type  :dense
   :output-type :prediction
   :hidden {:unit-num 3
            :activation :sigmoid
            :w (matrix [[1] [1] [1]])
            :bias (array [1 1 1])}
   :output {"prediction" {:w (array (take 3 (repeat 0.1)))
                          :bias (array [1])}}})


(def sample-model2 ;3->2->3
  {:matrix-kit default-matrix-kit
   :input-type  :dense
   :output-type :prediction
   :hidden {:unit-num 2
            :activation :sigmoid
            :w (matrix [[0.1 0.1 0.1] [0.2 0.2 0.2]])
            :bias (array [1 1])}
   :output {"prediction1" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction2" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction3" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}}})


(def sample-model2:sparse ;3->2->3
  {:matrix-kit default-matrix-kit
   :input-type  :sparse
   :output-type :prediction
   :hidden {:unit-num 2
            :activation :sigmoid
            :w {"natural" (array [0.1 0.2]) "language" (array [0.1 0.2]) "processing" (array [0.1 0.2])}
            :bias (array [1 1])}
   :output {"prediction1" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction2" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction3" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}}})

(deftest feedforward-test
  (testing "hidden-state-by-sparse"
    (are [arg expect] (= (vec (hidden-state-by-sparse
                                {:matrix-kit default-matrix-kit
                                 :hidden {:w {"natural"    (float-array [0 1])
                                              "language"   (float-array [2 3])
                                              "processing" (float-array [4 5])}
                                          :unit-num 2}}
                                arg
                                (float-array (repeat 2 0))))
                         expect)
         {"natural"    1.0} [0.0 1.0]
         {"natural"   -2.0} [0.0 -2.0]
         {"language"   1.0} [2.0 3.0]
         {"language"  -2.0} [-4.0 -6.0]
         {"processing" 1.0} [4.0 5.0]
         {"natural" 2 "processing" -1} [-4.0 -3.0]))

  (testing "network-output with dense model"
    (let [result (network-output sample-model (float-array [2]) #{"prediction"})
          {:keys [activation state]} result
          {:keys [input hidden output]} activation]
      (is (= (vec input) [(float 2)]))
      (is (= (vec hidden) (map float (take 3 (repeat 0.95257413)))))
      (is (= (vec (:hidden state)) [3.0 3.0 3.0]))
      (is (= (reduce (fn[acc [k v]](assoc acc k (float v))) {} output) {"prediction" (float 1.2857722491025925)}))))

  (testing "network-output with sparse model"
    (let [result (network-output sample-model2:sparse {"language" 1} #{"prediction1" "prediction3"})
          {:keys [activation state]} result
          {:keys [input hidden output]} activation]
      (is (= input {"language" 1}))
      (is (= (vec hidden) (map float [0.7502601 0.76852477])))
      (is (= (mapv float (:hidden state)) (map float [1.1 1.2])))
      (is (= (reduce (fn [acc [k v]](assoc acc k (float v))) {} output)
             {"prediction1" (float 2.518785) "prediction3" (float 2.518785)})))

    (let [a (:activation (network-output sample-model2 (float-array [0 1 0]) #{"prediction1" "prediction3"}))
          b (:activation (network-output sample-model2:sparse {"language" 1} #{"prediction1" "prediction3"}))]
      (is (= (:output a) (:output b)))))


  (testing "param-delta"
    (let [r (param-delta {:matrix-kit default-matrix-kit} (float-array (range 4)) (float-array (range 4)))]
      (is (= (map vec (:w-delta r)) [[0.0 0.0 0.0 0.0], [0.0 1.0 2.0 3.0], [0.0 2.0 4.0 6.0], [0.0 3.0 6.0 9.0]]))
      (is (= (vec (:bias-delta r)) [0.0 1.0 2.0 3.0]))))

  (testing "back-propagation with dense"
    (let [{:keys [param-loss loss]} (back-propagation sample-model
                                                      (network-output sample-model (float-array [2]) #{"prediction"})
                                                      {"prediction" 2})
          {:keys [output-delta hidden-delta]} param-loss
          {hidden-w-delta :w-delta hidden-bias-delta :bias-delta} hidden-delta
          {output-w-delta :w-delta output-bias-delta :bias-delta} (get output-delta "prediction")]
      (is (= loss {"prediction" (float 0.71422774)}))
      (is (= (map #(map float %) hidden-w-delta)
             (take 3 (repeat [(float 0.006453284)]))))
      (is (= (vec hidden-bias-delta)
             (take 3 (repeat (double 0.00322664200591678)))))
      (is (= (vec output-w-delta)
             (take 3 (repeat (double 0.680354867004688)))))
      (is (= (vec output-bias-delta)
             [(float 0.71422774)]))))

  (testing "back-propagation with sparse vector"
    (let [{:keys [param-loss loss]} (back-propagation sample-model2:sparse
                                                      (network-output sample-model2:sparse {"language" 1}  #{"prediction1" "prediction2" "prediction3"})
                                                      {"prediction1" 2 "prediction2" 1 "prediction3" 2})
          {:keys [output-delta hidden-delta]} param-loss
          {w-delta1 :w-delta bias-delta1 :bias-delta} (get output-delta "prediction1")
          {w-delta2 :w-delta bias-delta2 :bias-delta} (get output-delta "prediction2")
          {w-delta3 :w-delta bias-delta3 :bias-delta} (get output-delta "prediction3")]
      (is (= loss {"prediction1" (float -0.5187849), "prediction2" (float -1.5187849), "prediction3" (float -0.5187849)}))
      (is (= (vec (get (:w-delta hidden-delta) "language"))
             (map double [-0.4789838322238946 -0.45476128583309716])))
      (is (= (vec (:bias-delta hidden-delta))
             (map double [-0.4789838322238946 -0.45476128583309716])))
      (is (= (vec w-delta1)
             (vec w-delta3)
             (map double [-0.38922360403651624 -0.3986990289803174])))
      (is (= (vec w-delta2) (map double [-1.139483718706316 -1.1672237949486401])))
      (is (= (vec bias-delta1)
             (vec bias-delta3)
             (map float [-0.5187848806381226])))
      (is (= (vec bias-delta2) (map float [-1.5187849])))
      (let [a (:param-loss (back-propagation sample-model2:sparse
                                             (network-output sample-model2:sparse {"language" 1} #{"prediction1" "prediction2" "prediction3"})
                                             {"prediction1" 2 "prediction2" 1 "prediction3" 2}))
            b (:param-loss (back-propagation sample-model2
                                             (network-output sample-model2 (float-array [0 1 0]) #{"prediction1" "prediction2" "prediction3"})
                                             {"prediction1" 2 "prediction2" 1 "prediction3" 2}))]
        (is (= (vec (get (:w-delta (:hidden-delta a)) "language"))
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
                                                 {:hidden-delta {:w-delta {"natural" (array [0.1 0.1])
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
      (is (= (mapv float (get (:w hidden) "natural")) (map float [0.11 0.21])))
      (is (= (mapv float (get (:w hidden) "language")) (map float [0.11 0.21])))
      (is (= (mapv float (get (:w hidden) "processing")) (map float [0.11 0.21])))
      (is (= (mapv float (:bias hidden)) (map float (take 2 (repeat 1.01)))))
      (let [{:keys [w bias]} (get output "prediction1")]
        (is (= (mapv float w)
               (map float (take 2 (repeat 1.02)))))
        (is (= (mapv float bias)
               (map float [1.02]))))))

  (testing "init-model"
    (let [{:keys [hidden output input-type output-type]}
          (init-model {:input-type :dense
                       :input-items nil
                       :input-size 1
                       :hidden-size 3
                       :output-type :prediction
                       :output-items #{"prediction"}
                       :activation :sigmoid})]
      (is (= input-type :dense))
      (is (= output-type :prediction))
      (is (= (:unit-num hidden) 3))
      (is (= (ecount (:w hidden)) 3))
      (is (= (ecount (:bias hidden)) 3))
      (let [{:keys [w bias]} (get output "prediction")]
        (is (= (ecount w) 3))
        (is (= (ecount  bias) 1)))))

  (testing "init-model with sparse"
    (let [{:keys [hidden output input-type output-type]}
          (init-model {:input-type :sparse
                       :input-items #{"natural" "language" "processing"}
                       :input-size nil
                       :hidden-size 3
                       :output-type :binary-classification
                       :output-items #{"prediction"}
                       :activation :sigmoid})]
      (is (= input-type :sparse))
      (is (= output-type :binary-classification))
      (is (= (:unit-num hidden) 3))

      (is (= (ecount (get (:w hidden) "natural")) 3))
      (is (= (ecount (get (:w hidden) "language")) 3))
      (is (= (ecount (get (:w hidden) "processing")) 3))
      (is (= (ecount (get (:w hidden) "?")) 1))
      (is (= (ecount (:bias hidden)) 3))
      (let [{:keys [w bias]} (get output "prediction")]
        (is (= (ecount w) 3))
        (is (= (ecount bias) 1)))))
  )
