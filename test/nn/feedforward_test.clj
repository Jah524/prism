(ns nn.feedforward-test
  (:require [clojure.test :refer :all]
            [sai-ai.nn.feedforward :refer :all]))

(def sample-model ;1->3->1
  {:model-type nil
   :layers [{:activate-fn :sigmoid,
             :layer-type :hidden,
             :unit-num 3
             :w (float-array [1 1 1])
             :bias (float-array [1 1 1])}
            {:activate-fn :linear,
             :layer-type :output,
             :unit-num 1
             :w (float-array [1 1 1])
             :bias (float-array [1])}]})


(def sample-model2 ;3->2->3
  {:model-type nil
   ;;    :input-type  :one-hot
   ;;    :output-type :one-hot
   :layers [{:activate-fn :sigmoid,
             :layer-type :hidden,
             :unit-num 2
             :w (float-array [0.1 0.1 0.1 0.2 0.2 0.2])
             :bias (float-array [1 1])}
            {:activate-fn :linear,
             :layer-type :output,
             :unit-num 3
             :w (float-array [1 1 1 1 1 1])
             :bias (float-array [1 1 1])}]})

(def sample-model2:sparse ;3->2->3
  {:model-type nil
   :input-type  :sparse
   :layers [{:activate-fn :sigmoid,
             :layer-type :hidden,
             :unit-num 2
             :w {"natural" (float-array [0.1 0.2]) "language" (float-array [0.1 0.2]) "processing" (float-array [0.1 0.2])}
             :bias (float-array [1 1])}
            {:activate-fn :linear,
             :layer-type :output,
             :unit-num 3
             :w (float-array [1 1 1 1 1 1])
             :bias (float-array [1 1 1])}]})
;:w {"natural" (float-array [0.1 0.2]) "language" (float-array [0.1 0.2]) "processing" (float-array [0.1 0.2])}
;:bias {"natural" (float-array [1]) "language" (float-array [1]) "processing" (float-array [1])}}]})

(deftest feedforward-test
  (testing "hidden-state-by-sparse"
    (are [arg expect] (= (vec (hidden-state-by-sparse {"natural"    (float-array [0 1])
                                                       "language"   (float-array [2 3])
                                                       "processing" (float-array [4 5])}
                                                      arg 2)) expect)
         {"natural"    1.0} [0.0 1.0]
         {"natural"   -2.0} [0.0 -2.0]
         {"language"   1.0} [2.0 3.0]
         {"language"  -2.0} [-4.0 -6.0]
         {"processing" 1.0} [4.0 5.0]
         {"natural" 2 "processing" -1} [-4.0 -3.0]))

  (testing "network-output with gemv"
    (let [result (network-output sample-model (float-array [2]))]
      (is (= (vec (:activation (first result))) [2.0]))
      (is (= (vec (:activation (second result))) (take 3 (repeat (float 0.95257413)))))
      (is (= (vec (:state (second result))) [3.0 3.0 3.0]))
      (is (= (vec (:activation (nth result 2))) [(float 3.8577223)]))
      (is (= (vec (:state (nth result 2))) [(float 3.8577223)]))))

  (testing "network-output with sparse vector"
    (let [result (network-output sample-model2:sparse {"language" 1})]
      (is (= (vec (:activation (first result)))  [["language" 1]]))
      (is (= (vec (:activation (second result))) [(float 0.7502601) (float 0.76852477)]))
      (is (= (vec (:state (second result))) [(float 1.1) (float 1.2)]))
      (is (= (vec (:activation (nth result 2))) (take 3 (repeat (float 2.518785)))))
      (is (= (vec (:state (nth result 2))) (take 3 (repeat (float 2.518785))))))

    (let [a (rest (network-output sample-model2 (float-array [0 1 0])))
          b (rest (network-output sample-model2:sparse {"language" 1}))]
      (is (= (vec (:activation a)) (vec (:activation b))))
      (is (= (vec (:state a)) (vec (:state b))))))
  (testing "param-delta"
    (let [r (param-delta (float-array (range 4)) (float-array (range 4)))]
      (is (= (vec (:w-delta r)) [0.0 0.0 0.0 0.0, 0.0 1.0 2.0 3.0, 0.0 2.0 4.0 6.0, 0.0 3.0 6.0 9.0]))
      (is (= (vec (:bias-delta r)) [0.0 1.0 2.0 3.0]))))
  ;;   (testing "params-delta:one-hot"
  ;;     (let [r (params-delta:one-hot (range 1 4 1) 3 5)]
  ;;     (is (=
  ;;            (map float [0 0 0 1 0 0 0 0 2 0 0 0 0 3 0]))))


  (testing "back-propagation with gemv"
    (let [result (back-propagation sample-model (float-array [2]) (float-array [2]))]
      (is (= (vec (:w-delta (first  result)))
             (take 3 (repeat (float -0.16785136)))))
      (is (= (vec (:bias-delta (first result)))
             (take 3 (repeat (float -0.08392568)))))
      (is (= (vec (:w-delta (second result)))
             (take 3 (repeat (float -1.7696182)))))
      (is (= (vec (:bias-delta (second result)))
             [(float -1.8577223)]))))

  (testing "back-propagation with sparse vector"
    (let [result (back-propagation sample-model2:sparse {"language" 1} (float-array [1 1 1]))]
      (is (= (vec (get (:w-delta (first result)) "language"))
             (map float [-0.85372365 -0.8105502])))
      ;             (map float [0.0 -1.2284634 0.0 0.0 -1.1663392 0.0])))
      (is (= (vec (:bias-delta (first result)))
             (map float [-0.85372365 -0.8105502])))
      (is (= (vec (:w-delta (second result)))
             (map float [-1.1394838 -1.1672239 -1.1394838 -1.1672239 -1.1394838 -1.1672239])))
      (is (= (vec (:bias-delta (second result)))
             (map float [-1.518785 -1.518785 -1.518785]))))
    (let [a (back-propagation sample-model2:sparse {"language" 1} (float-array [1 1 1]))
          b (back-propagation sample-model2 (float-array [0 1 0]) (float-array [1 1 1]))]
      (is (= (vec (get (:w-delta (first a)) "language"))
             (let[it (:w-delta (first b))]
               [(nth it 1) (nth it 4)])))
      (is (= (vec (:bias-delta (first a)))
             (vec (:bias-delta (first b)))))
      (is (= (vec(:w-delta (second a)))
             (vec (:w-delta (second b)))))
      (is (= (vec(:bias-delta (second a)))
             (vec (:bias-delta (second b)))))))


  (testing "update-model"
    (let [result (:layers (update-model sample-model [{:w-delta (float-array [0.1 0.2 0.3]) :bias-delta (float-array [0.1 0.2 0.3])}
                                                      {:w-delta (float-array [2 2 2]) :bias-delta (float-array [2])}] 0.1))]
      (is (= (:layer-type (first result)) :hidden))
      (is (= (:unit-num (first result)) 3))
      (is (= (:activate-fn (first result)) :sigmoid))
      (is (= (vec (:w (first result))) (map float [1.01 1.02 1.03])))
      (is (= (vec (:bias (first result))) (map float [1.01 1.02 1.03])))
      (is (= (:layer-type (second result)) :output))
      (is (= (:unit-num (second result)) 1))
      (is (= (:activate-fn (second result)) :linear))
      (is (= (vec (:w (second result))) (map float [1.2 1.2 1.2])))
      (is (= (vec (:bias (second result))) (map float [1.2]))))

    (let [m (update-model sample-model2:sparse
                          [{:w-delta {"natural" (float-array [0.1 0.1])
                                      "language" (float-array [0.1 0.1])
                                      "processing" (float-array [0.1 0.1])}
                            :bias-delta (float-array [0.1 0.1])}
                           {:w-delta (float-array (take 6 (repeat 0.2)))
                            :bias-delta (float-array (take 3 (repeat 0.2)))}]
                          0.1)
          result (:layers m)]
      (is (= (:input-type m) :sparse))
      (is (= (:layer-type (first result)) :hidden))
      (is (= (:unit-num (first result)) 2))
      (is (= (:activate-fn (first result)) :sigmoid))
      (is (= (vec (get (:w (first result)) "natural")) (map float [0.11 0.21000001])))
      (is (= (vec (get (:w (first result)) "language")) (map float [0.11 0.21000001])))
      (is (= (vec (get (:w (first result)) "processing")) (map float [0.11 0.21000001])))
      (is (= (vec (:bias (first result))) (map float (take 2 (repeat 1.01)))))
      (is (= (vec (:w (second result))) (map float (take 6 (repeat 1.02)))))
      (is (= (vec (:bias (second result))) (map float (take 3 (repeat 1.02)))))))


  (testing "init-model"
    (let [model (init-model {:model-type nil
                             :layers [{:unit-num 1 :layer-type :input}
                                      {:unit-num 3 :activate-fn :sigmoid :layer-type :hidden}
                                      {:unit-num 1 :activate-fn :softmax  :layer-type :output}]})]
      (is (= (count (:layers model)) 2))
      (is (= (count (:w (first (:layers model)))) 3))
      (is (= (count (:w (second (:layers model)))) 3))
      (is (= (count (:bias (first (:layers model)))) 3))
      (is (= (count (:bias (second (:layers model)))) 1))))
  (testing "init-model with sparse"
    (let [model (init-model {:model-type nil
                             :input-type :sparse
                             :layers [{:unit-num 3 :layer-type :input}
                                      {:unit-num 2 :activate-fn :sigmoid :layer-type :hidden}
                                      {:unit-num 3 :activate-fn :softmax  :layer-type :output}]}
                            {"natural" 3 "language" 1 "processing" 1})]
      (is (= (count (:layers model)) 2))
      (is (= (count (get (:w (first (:layers model))) "natural")) 2))
      (is (= (count (get (:w (first (:layers model))) "language")) 2))
      (is (= (count (get (:w (first (:layers model))) "processing")) 2))
      (is (= (count (get (:w (first (:layers model))) "?")) 0))
      (is (= (count (:w (second (:layers model)))) 6))
      (is (= (count (:bias (first (:layers model)))) 2))
      (is (= (count (:bias (second (:layers model)))) 3)))))
