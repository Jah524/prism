(ns nn.feedforward-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [prism.nn.feedforward :refer :all]))

(def sample-model ;1->3->1
  {:input-type  :dense
   :output-type :prediction
   :hidden {:unit-num 3
            :activation :sigmoid
            :w (float-array [1 1 1])
            :bias (float-array [1 1 1])}
   :output {:unit-num 1
            :w {"prediction" (float-array (take 3 (repeat 0.1)))}
            :bias {"prediction" (float-array [1])}}})


(def sample-model2 ;3->2->3
  {:input-type  :dense
   :output-type :prediction
   :hidden {:unit-num 2
            :activation :sigmoid
            :w (float-array [0.1 0.1 0.1 0.2 0.2 0.2])
            :bias (float-array [1 1])}
   :output {:w {"prediction1" (float-array (take 2 (repeat 1)))
                "prediction2" (float-array (take 2 (repeat 1)))
                "prediction3" (float-array (take 2 (repeat 1)))}
            :bias {"prediction1" (float-array [1])
                   "prediction2" (float-array [1])
                   "prediction3" (float-array [1])}}})

(def sample-model2:sparse ;3->2->3
  {:input-type  :sparse
   :output-type :prediction
   :hidden {:unit-num 2
            :activation :sigmoid
            :w {"natural" (float-array [0.1 0.2]) "language" (float-array [0.1 0.2]) "processing" (float-array [0.1 0.2])}
            :bias (float-array [1 1])}
   :output {:w {"prediction1" (float-array (take 2 (repeat 1)))
                "prediction2" (float-array (take 2 (repeat 1)))
                "prediction3" (float-array (take 2 (repeat 1)))}
            :bias {"prediction1" (float-array [1])
                   "prediction2" (float-array [1])
                   "prediction3" (float-array [1])}}})

(deftest feedforward-test
  (testing "hidden-state-by-sparse"
    (are [arg expect] (= (vec (hidden-state-by-sparse
                                {:hidden {:w {"natural"    (float-array [0 1])
                                              "language"   (float-array [2 3])
                                              "processing" (float-array [4 5])}
                                          :unit-num 2}}
                                arg 2))
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
      (is (= (vec (:hidden state)) (map float [1.1 1.2])))
      (is (= (reduce (fn [acc [k v]](assoc acc k (float v))) {} output)
             {"prediction1" (float 2.518785) "prediction3" (float 2.518785)})))

    (let [a (:activation (network-output sample-model2 (float-array [0 1 0]) #{"prediction1" "prediction3"}))
          b (:activation (network-output sample-model2:sparse {"language" 1} #{"prediction1" "prediction3"}))]
      (is (= (:output a) (:output b)))))


  (testing "param-delta"
    (let [r (param-delta (float-array (range 4)) (float-array (range 4)))]
      (is (= (vec (:w-delta r)) [0.0 0.0 0.0 0.0, 0.0 1.0 2.0 3.0, 0.0 2.0 4.0 6.0, 0.0 3.0 6.0 9.0]))
      (is (= (vec (:bias-delta r)) [0.0 1.0 2.0 3.0]))))

  (testing "back-propagation with gemv"
    (let [{:keys [output-delta hidden-delta]} (back-propagation sample-model (float-array [2]) {"prediction" 2})
          {hidden-w-delta :w-delta hidden-bias-delta :bias-delta} hidden-delta
          {output-w-delta :w-delta output-bias-delta :bias-delta} (get output-delta "prediction")]
      (is (= (vec hidden-w-delta)
             (take 3 (repeat (float 0.0064532845)))))
      (is (= (vec hidden-bias-delta)
             (take 3 (repeat (float 0.0032266423)))))
      (is (= (vec output-w-delta)
             (take 3 (repeat (float 0.6803549)))))
      (is (= (vec output-bias-delta)
             [(float 0.71422774)]))))

  (testing "back-propagation with sparse vector"
    (let [{:keys [output-delta hidden-delta]} (back-propagation sample-model2:sparse
                                                                {"language" 1}
                                                                {"prediction1" 2 "prediction2" 1 "prediction3" 2})
          {w-delta1 :w-delta bias-delta1 :bias-delta} (get output-delta "prediction1")
          {w-delta2 :w-delta bias-delta2 :bias-delta} (get output-delta "prediction2")
          {w-delta3 :w-delta bias-delta3 :bias-delta} (get output-delta "prediction3")]
      (is (= (vec (get (:w-delta hidden-delta) "language"))
             (map float [-0.47898382 -0.45476127])))
      (is (= (vec (:bias-delta hidden-delta))
             (map float [-0.47898382 -0.45476127])))
      (is (= (vec w-delta1)
             (vec w-delta3)
             (map float [-0.3892236 -0.39869902])))
      (is (= (vec w-delta2) (map float [-1.1394837 -1.1672238])))
      (is (= (vec bias-delta1)
             (vec bias-delta3)
             (map float [-0.5187849])))
      (is (= (vec bias-delta2) (map float [-1.5187849])))
      (let [a (back-propagation sample-model2:sparse {"language" 1} {"prediction1" 2 "prediction2" 1 "prediction3" 2})
            b (back-propagation sample-model2 (float-array [0 1 0]) {"prediction1" 2 "prediction2" 1 "prediction3" 2})]
        (is (= (vec (get (:w-delta (:hidden-delta a)) "language"))
               (let [it (:w-delta (:hidden-delta b))]
                 [(nth it 1) (nth it 4)])))
        (is (= (vec (:bias-delta (:hidden-delta a)))
               (vec (:bias-delta (:hidden-delta b)))))
        (is (= (vec (:w-delta (:output-delta a)))
               (vec (:w-delta (:output-delta b)))))1
        (is (= (vec (:bias-delta (:output-delta a)))
               (vec (:bias-delta (:output-delta b))))))))

  (testing "update-model!"
    (let [{:keys [hidden output]} (update-model! sample-model
                                                 {:hidden-delta {:w-delta (float-array [0.1 0.2 0.3])
                                                                 :bias-delta (float-array [0.1 0.2 0.3])}
                                                  :output-delta {"prediction" {:w-delta (float-array [2 2 2])
                                                                               :bias-delta (float-array [2])}}}
                                                 0.1)]
      (is (= (:activation hidden) :sigmoid))
      (is (= (vec (:w hidden)) (map float [1.01 1.02 1.03])))
      (is (= (vec (:bias hidden)) (map float [1.01 1.02 1.03])))
      (is (= (reduce (fn [acc [k v]] (assoc acc k (vec v))) {} (:w output))
             {"prediction" (map float [0.3 0.3 0.3])}))
      (is (= (reduce (fn [acc [k v]] (assoc acc k (vec v))) {} (:bias output))
             {"prediction" (map float [1.2])})))

    (let [{:keys [hidden output]} (update-model! sample-model2:sparse
                                                 {:hidden-delta {:w-delta {"natural" (float-array [0.1 0.1])
                                                                           "language" (float-array [0.1 0.1])
                                                                           "processing" (float-array [0.1 0.1])}
                                                                 :bias-delta (float-array [0.1 0.1])}
                                                  :output-delta {"prediction1" {:w-delta (float-array (take 2 (repeat 0.2)))
                                                                                :bias-delta (float-array [0.2])}
                                                                 "prediction2" {:w-delta (float-array (take 2 (repeat 0.2)))
                                                                                :bias-delta (float-array [0.2])}
                                                                 "prediction3" {:w-delta (float-array (take 2 (repeat 0.2)))
                                                                                :bias-delta (float-array [0.2])}}}
                                                 0.1)]
      (is (= (:activation hidden) :sigmoid))
      (is (= (vec (get (:w hidden) "natural")) (map float [0.11 0.21000001])))
      (is (= (vec (get (:w hidden) "language")) (map float [0.11 0.21000001])))
      (is (= (vec (get (:w hidden) "processing")) (map float [0.11 0.21000001])))
      (is (= (vec (:bias hidden)) (map float (take 2 (repeat 1.01)))))
      (is (= (vec (get (:w    output) "prediction1"))
             (map float (take 2 (repeat 1.02)))))
      (is (= (vec (get (:bias output) "prediction1"))
             (map float [1.02])))))

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
      (is (= (count (:w hidden)) 3))
      (is (= (count (:bias hidden)) 3))
      (is (= (count (get (:w output) "prediction")) 3))
      (is (= (count (get (:bias output) "prediction")) 1))))

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

      (is (= (count (get (:w hidden) "natural")) 2))
      (is (= (count (get (:w hidden) "language")) 2))
      (is (= (count (get (:w hidden) "processing")) 2))
      (is (= (count (get (:w hidden) "?")) 0))
      (is (= (count (:bias hidden)) 3))
      (is (= (count (get (:w output) "prediction")) 3))
      (is (= (count (get (:bias output) "prediction")) 1)))))
