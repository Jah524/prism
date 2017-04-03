(ns nn.lstm-test
  (:require [clojure.test :refer :all]
            [nn.lstm :refer :all]))


(def sample-w-network-deep ;1->20->20->1
  {:model-type :deep
   :unit-nums [1 20 20 1]
   :layers [{:unit-type :lstm
             :unit-num 20
             :peephole #{:input-gate :forget-gate :output-gate}
             :layer-type :hidden,
             :block-w (float-array (take 20 (repeat 0.1)))
             :block-wr (float-array (take 400 (repeat 0.1)))
             :block-bias (float-array (take 20 (repeat -1)))
             :input-gate-w  (float-array (take 20 (repeat 0.1)))
             :input-gate-wr  (float-array (take 400 (repeat 0.1)))
             :input-gate-bias (float-array (take 20 (repeat -1)))
             :forget-gate-w (float-array (take 20 (repeat 0.1)))
             :forget-gate-wr (float-array (take 400 (repeat 0.1)))
             :forget-gate-bias (float-array (take 20 (repeat -1)))
             :output-gate-w (float-array (take 20 (repeat 0.1)))
             :output-gate-wr (float-array (take 400 (repeat 0.1)))
             :output-gate-bias (float-array (take 20 (repeat -1)))
             :input-gate-peephole  (float-array (take 20 (repeat -0.1)))
             :forget-gate-peephole (float-array (take 20 (repeat -0.1)))
             :output-gate-peephole (float-array (take 20 (repeat -0.1)))
             }
            {:unit-type :lstm
             :unit-num 20
             :layer-type :hidden,
             :peephole #{:input-gate :forget-gate :output-gate}
             :block-w (float-array (take 420 (repeat 0.1)))
             :block-wr (float-array (take 400 (repeat 0.1)))
             :block-bias (float-array (take 20 (repeat -1)))
             :input-gate-w  (float-array (take 420 (repeat 0.1)))
             :input-gate-wr  (float-array (take 400 (repeat 0.1)))
             :input-gate-bias (float-array (take 20 (repeat -1)))
             :forget-gate-w (float-array (take 420 (repeat 0.1)))
             :forget-gate-wr (float-array (take 400 (repeat 0.1)))
             :forget-gate-bias (float-array (take 20 (repeat -1)))
             :output-gate-w (float-array (take 420 (repeat 0.1)))
             :output-gate-wr (float-array (take 400 (repeat 0.1)))
             :output-gate-bias (float-array (take 20 (repeat -1)))
             :input-gate-peephole (float-array (take 20 (repeat -0.1)))
             :forget-gate-peephole (float-array (take 20 (repeat -0.1)))
             :output-gate-peephole (float-array (take 20 (repeat -0.1)))
             }
            {:activate-fn :linear,
             :layer-type :output,
             :unit-num 1
             :w (float-array (take 40 (repeat 0.1)))
             :bias (float-array [-1])}]})

(def sample-w-network
  {:model-type :nil
   :unit-nums [3 10 3]
   :layers [{:unit-type :lstm
             :unit-num 10
             :layer-type :hidden
             :block-w (float-array (take 30 (repeat 0.1)))
             :block-wr (float-array (take 100 (repeat 0.1)))
             :block-bias (float-array (take 10 (repeat -1)))
             :input-gate-w  (float-array (take 30 (repeat 0.1)))
             :input-gate-wr  (float-array (take 100 (repeat 0.1)))
             :input-gate-bias (float-array (take 10 (repeat -1)))
             :forget-gate-w (float-array (take 30 (repeat 0.1)))
             :forget-gate-wr (float-array (take 100 (repeat 0.1)))
             :forget-gate-bias (float-array (take 10 (repeat -1)))
             :output-gate-w (float-array (take 30 (repeat 0.1)))
             :output-gate-wr (float-array (take 100 (repeat 0.1)))
             :output-gate-bias (float-array (take 10 (repeat -1)))
             :peephole #{:input-gate :forget-gate :output-gate}
             :input-gate-peephole  (float-array (take 10 (repeat -0.1)))
             :forget-gate-peephole (float-array (take 10 (repeat -0.1)))
             :output-gate-peephole (float-array (take 10 (repeat -0.1)))}
            {:activate-fn :linear,
             :layer-type :output,
             :unit-num 3
             :w (float-array (take 30 (repeat 0.1)))
             :bias (float-array [-1 -1 -1])}]})

(def sample-w-network-sparse
  {:model-type :nil
   :input-type :sparse
   :unit-nums [3 10 3]
   :layers [{:unit-type :lstm
             :unit-num 10
             :peephole #{:input-gate :forget-gate :output-gate}
             :layer-type :hidden,
             :sparses {"natural" {:block-w (float-array (take 10 (repeat 0.1)))
                                  :input-gate-w  (float-array (take 10 (repeat 0.1)))
                                  :forget-gate-w (float-array (take 10 (repeat 0.1)))
                                  :output-gate-w (float-array (take 10 (repeat 0.1)))}
                       "language" {:block-w (float-array (take 10 (repeat 0.1)))
                                   :input-gate-w  (float-array (take 10 (repeat 0.1)))
                                   :forget-gate-w (float-array (take 10 (repeat 0.1)))
                                   :output-gate-w (float-array (take 10 (repeat 0.1)))}
                       "processing" {:block-w (float-array (take 10 (repeat 0.1)))
                                     :input-gate-w  (float-array (take 10 (repeat 0.1)))
                                     :forget-gate-w (float-array (take 10 (repeat 0.1)))
                                     :output-gate-w (float-array (take 10 (repeat 0.1)))}}
             :block-wr (float-array (take 100 (repeat 0.1)))
             :block-bias (float-array (take 10 (repeat -1)))
             :input-gate-wr  (float-array (take 100 (repeat 0.1)))
             :input-gate-bias (float-array (take 10 (repeat -1)))
             :forget-gate-wr (float-array (take 100 (repeat 0.1)))
             :forget-gate-bias (float-array (take 10 (repeat -1)))
             :output-gate-wr (float-array (take 100 (repeat 0.1)))
             :output-gate-bias (float-array (take 10 (repeat -1)))
             :input-gate-peephole  (float-array (take 10 (repeat -0.1)))
             :forget-gate-peephole (float-array (take 10 (repeat -0.1)))
             :output-gate-peephole (float-array (take 10 (repeat -0.1)))
             :sparse? true}
            {:activate-fn :linear,
             :layer-type :output,
             :unit-num 3
             :w (float-array (take 30 (repeat 0.1)))
             :bias (float-array [-1 -1 -1])}]})

(deftest lstm-test
  (testing "init-lstm-model with stack model"
    (let [m (init-lstm-model
             {:model-type :stack
              :input-type nil;:one-hot
              :layers [{:unit-num 10 :layer-type :input}
                       {:unit-num 20 :unit-type :lstm :layer-type :hidden :peephole #{:input-gate :forget-gate :output-gate}}
                       {:unit-num 20 :unit-type :lstm :layer-type :hidden}
                       {:unit-num 10 :activate-fn :softmax  :layer-type :output}]})
          layers (:layers m)
          f (first layers)
          s (second layers)
          l (last layers)]
      (is (= [10 20 20 10] (:unit-nums m)))
      (is (= 200 (count (remove zero? (:block-w f)))))
      (is (= 400 (count (remove zero? (:block-wr f)))))
      (is (= 200 (count (remove zero? (:input-gate-w f)))))
      (is (= 400 (count (remove zero? (:input-gate-wr f)))))
      (is (= 200 (count (remove zero? (:forget-gate-w f)))))
      (is (= 400 (count (remove zero? (:forget-gate-wr f)))))
      (is (= 200 (count (remove zero? (:output-gate-w f)))))
      (is (= 400 (count (remove zero? (:output-gate-wr f)))))
      (is (= 20 (count (remove zero? (:block-bias f)))))
      (is (= 20 (count (remove zero? (:input-gate-bias f)))))
      (is (= 20 (count (remove zero? (:forget-gate-bias f)))))
      (is (= 20 (count (remove zero? (:output-gate-bias f)))))
      (is (= 20 (count (remove zero? (:input-gate-peephole f)))))
      (is (= 20 (count (remove zero? (:forget-gate-peephole f)))))
      (is (= 20 (count (remove zero? (:output-gate-peephole f)))))
      (is (= 400 (count (remove zero? (:block-w s)))))
      (is (= 400 (count (remove zero? (:block-wr s)))))
      (is (= 400 (count (remove zero? (:input-gate-w s)))))
      (is (= 400 (count (remove zero? (:input-gate-wr s)))))
      (is (= 400 (count (remove zero? (:forget-gate-w s)))))
      (is (= 400 (count (remove zero? (:forget-gate-wr s)))))
      (is (= 400 (count (remove zero? (:output-gate-w s)))))
      (is (= 400 (count (remove zero? (:output-gate-wr s)))))
      (is (= 20 (count (remove zero? (:block-bias s)))))
      (is (= 20 (count (remove zero? (:input-gate-bias s)))))
      (is (= 20 (count (remove zero? (:forget-gate-bias s)))))
      (is (= 20 (count (remove zero? (:output-gate-bias s)))))
      (is (= 0 (count (:input-gate-peephole s))))
      (is (= 0 (count (:forget-gate-peephole s))))
      (is (= 0 (count (:output-gate-peephole s))))
      (is (= 200 (count (remove zero? (:w l)))))
      (is (= 10 (count (remove zero? (:bias l)))))))

  (testing "init-lstm-model with deep model"
    (let [m (init-lstm-model
             {:model-type :deep
              :input-type nil;:one-hot
              :layers [{:unit-num 10 :layer-type :input}
                       {:unit-num 20 :unit-type :lstm :layer-type :hidden :peephole #{:input-gate :forget-gate :output-gate}}
                       {:unit-num 20 :unit-type :lstm :layer-type :hidden}
                       {:unit-num 10 :activate-fn :softmax  :layer-type :output}]})
          layers (:layers m)
          f (first layers)
          s (second layers)
          l (last layers)]
      (is (= [10 20 20 10] (:unit-nums m)))
      (is (= 200 (count (remove zero? (:block-w f)))))
      (is (= 400 (count (remove zero? (:block-wr f)))))
      (is (= 200 (count (remove zero? (:input-gate-w f)))))
      (is (= 400 (count (remove zero? (:input-gate-wr f)))))
      (is (= 200 (count (remove zero? (:forget-gate-w f)))))
      (is (= 400 (count (remove zero? (:forget-gate-wr f)))))
      (is (= 200 (count (remove zero? (:output-gate-w f)))))
      (is (= 400 (count (remove zero? (:output-gate-wr f)))))
      (is (= 20 (count (remove zero? (:block-bias f)))))
      (is (= 20 (count (remove zero? (:input-gate-bias f)))))
      (is (= 20 (count (remove zero? (:forget-gate-bias f)))))
      (is (= 20 (count (remove zero? (:output-gate-bias f)))))
      (is (= 20 (count (remove zero? (:input-gate-peephole f)))))
      (is (= 20 (count (remove zero? (:forget-gate-peephole f)))))
      (is (= 20 (count (remove zero? (:output-gate-peephole f)))))
      (is (= 600 (count (remove zero? (:block-w s)))))
      (is (= 400 (count (remove zero? (:block-wr s)))))
      (is (= 600 (count (remove zero? (:input-gate-w s)))))
      (is (= 400 (count (remove zero? (:input-gate-wr s)))))
      (is (= 600 (count (remove zero? (:forget-gate-w s)))))
      (is (= 400 (count (remove zero? (:forget-gate-wr s)))))
      (is (= 600 (count (remove zero? (:output-gate-w s)))))
      (is (= 400 (count (remove zero? (:output-gate-wr s)))))
      (is (= 20 (count (remove zero? (:block-bias s)))))
      (is (= 20 (count (remove zero? (:input-gate-bias s)))))
      (is (= 20 (count (remove zero? (:forget-gate-bias s)))))
      (is (= 20 (count (remove zero? (:output-gate-bias s)))))
      (is (= 0 (count (:input-gate-peephole s))))
      (is (= 0 (count (:forget-gate-peephole s))))
      (is (= 0 (count (:output-gate-peephole s))))
      (is (= 400 (count (remove zero? (:w l)))))
      (is (= 10 (count (remove zero? (:bias l)))))))
  (testing "init-lstm-model with sparse"
    (let [m (init-lstm-model
             {:model-type :deep
              :input-type :sparse
              :layers [{:unit-num 3 :layer-type :input}
                       {:unit-num 10 :unit-type :lstm :layer-type :hidden :peephole #{:input-gate :forget-gate :output-gate}}
                       {:unit-num 3 :activate-fn :softmax  :layer-type :output}]}
             ["natural" "language" "processing"])
          layers (:layers m)
          f (first layers)
          l (last layers)]
      (is (= [3 10 3] (:unit-nums m)))
      (is (true? (:sparse? f)))
      (is (= 10 (count (remove zero? (:block-w (get (:sparses f) "natural"))))))
      (is (= 100 (count (remove zero? (:block-wr f)))))
      (is (= 10 (count (remove zero? (:input-gate-w (get (:sparses f) "language"))))))
      (is (= 100 (count (remove zero? (:input-gate-wr f)))))
      (is (= 10 (count (remove zero? (:forget-gate-w (get (:sparses f) "processing"))))))
      (is (= 100 (count (remove zero? (:forget-gate-wr f)))))
      (is (= 10 (count (remove zero? (:output-gate-w (get (:sparses f) "language"))))))
      (is (= 100 (count (remove zero? (:output-gate-wr f)))))
      (is (= 10 (count (remove zero? (:block-bias f)))))
      (is (= 10 (count (remove zero? (:input-gate-bias f)))))
      (is (= 10 (count (remove zero? (:forget-gate-bias f)))))
      (is (= 10 (count (remove zero? (:output-gate-bias f)))))
      (is (= 10 (count (remove zero? (:input-gate-peephole f)))))
      (is (= 10 (count (remove zero? (:forget-gate-peephole f)))))
      (is (= 10 (count (remove zero? (:output-gate-peephole f)))))
      (is (= 30 (count (remove zero? (:w l)))))
      (is (= 3 (count (remove zero? (:bias l)))))))
  (testing "standard-activation"
    (let [result (standard-activation (float-array (range 10)) {:w (float-array (take 10 (repeat 0.1))) :bias (float-array [-2]) :unit-num 1 :activate-fn :sigmoid})]
      (is (= (vec (:activation result)) [(float 0.9241418)]))
      (is (= (vec (:state result)) [2.5]))))
  (testing "lstm-activation"
    (let [result (lstm-activation {:block-w (float-array (take 5 (repeat 1)))
                                   :block-wr (float-array (take 5 (repeat 1)))
                                   :block-bias (float-array [1])
                                   :input-gate-w  (float-array (take 5 (repeat -0.01)))
                                   :input-gate-wr  (float-array (take 5 (repeat -0.01)))
                                   :input-gate-bias  (float-array [0])
                                   :forget-gate-w (float-array (take 5 (repeat 0.01)))
                                   :forget-gate-wr (float-array (take 5 (repeat 0.01)))
                                   :forget-gate-bias (float-array [0])
                                   :output-gate-w (float-array (take 5 (repeat 0.01)))
                                   :output-gate-wr (float-array (take 5 (repeat 0.01)))
                                   :output-gate-bias (float-array [1])
                                   :peephole nil :unit-num 1}
                                  (float-array (take 5 (repeat 2)))
                                  (float-array (take 5 (repeat 2)))
                                  (float-array [10]))
          a (:activation result)
          s (:state result)]
      (is (= (vec a) [(float 0.7685143)]))
      (is (= (vec (:lstm s)) [(float 0.7685143)]))
      (is (= (vec (:block s)) [(float 21.0)]))
      (is (= (vec (:input-gate  s)) [(float -0.19999998807907104)]))
      (is (= (vec (:forget-gate s)) [(float 0.19999998807907104)]))
      (is (= (vec (:output-gate s)) [(float 1.199999988079071)]))
      (is (= (vec (:cell-state  s)) [(float 5.9485064)]))))
  (testing "lstm-activation with peephole"
    (let [result (lstm-activation {:block-w (float-array (take 5 (repeat 1)))
                                   :block-wr (float-array (take 5 (repeat 1)))
                                   :block-bias (float-array [1])
                                   :input-gate-w  (float-array (take 5 (repeat -0.01)))
                                   :input-gate-wr  (float-array (take 5 (repeat -0.01)))
                                   :input-gate-bias (float-array [0])
                                   :forget-gate-w (float-array (take 5 (repeat -0.01)))
                                   :forget-gate-wr (float-array (take 5 (repeat -0.01)))
                                   :forget-gate-bias (float-array [0])
                                   :output-gate-w (float-array (take 5 (repeat -0.01)))
                                   :output-gate-wr (float-array (take 5 (repeat -0.01)))
                                   :output-gate-bias (float-array [1])
                                   :peephole #{:input-gate :forget-gate :output-gate}
                                   :input-gate-peephole  (float-array [1])
                                   :forget-gate-peephole (float-array [1])
                                   :output-gate-peephole (float-array [1])
                                   :unit-num 1}
                                  (float-array (take 5 (repeat 2)))
                                  (float-array (take 5 (repeat 2)))
                                  (float-array [10]))
          a (:activation result)
          s (:state result)]
      (is (= (vec a) [(float 0.9999796)]))
      (is (= (vec (:lstm s)) [(float 0.9999796)]))
      (is (= (vec (:block s)) [(float 21.0)]))
      (is (= (vec (:input-gate  s)) [(float 9.800000011920929)]))
      (is (= (vec (:forget-gate s)) [(float 9.800000011920929)]))
      (is (= (vec (:output-gate s)) [(float 10.800000011920929)]))
      (is (= (vec (:cell-state  s)) [(float 10.999391)]))))

  (testing "lstm-activation with sparse"
    (let [result (lstm-activation {:sparses {"natural" {:block-w       (float-array (take 10 (repeat 1)))
                                                        :input-gate-w  (float-array (take 10 (repeat -0.01)))
                                                        :forget-gate-w (float-array (take 10 (repeat 0.01)))
                                                        :output-gate-w (float-array (take 10 (repeat 0.01)))}}
                                   :block-wr (float-array (take 100 (repeat 1)))
                                   :block-bias (float-array (take 10 (repeat 1)))
                                   :input-gate-wr  (float-array (take 100 (repeat -0.01)))
                                   :input-gate-bias  (float-array (take 10 (repeat 0)))
                                   :forget-gate-wr (float-array (take 100 (repeat 0.01)))
                                   :forget-gate-bias (float-array (take 10 (repeat 0)))
                                   :output-gate-wr (float-array (take 100 (repeat 0.01)))
                                   :output-gate-bias (float-array (take 10 (repeat 1)))
                                   :peephole nil :unit-num 10 :sparse? true}
                                  {"natural" (float 2)}
                                  (float-array (take 10 (repeat 2)))
                                  (float-array (float-array (take 10 (repeat 10)))))
          a (:activation result)
          s (:state result)
          result2 (lstm-activation  {:block-w       (float-array (take 10 (repeat 1)))
                                     :block-wr (float-array (take 100 (repeat 1)))
                                     :block-bias (float-array (take 10 (repeat 1)))
                                     :input-gate-w  (float-array (take 10 (repeat -0.01)))
                                     :input-gate-wr  (float-array (take 100 (repeat -0.01)))
                                     :input-gate-bias  (float-array (take 10 (repeat 0)))
                                     :forget-gate-w (float-array (take 10 (repeat 0.01)))
                                     :forget-gate-wr (float-array (take 100 (repeat 0.01)))
                                     :forget-gate-bias (float-array (take 10 (repeat 0)))
                                     :output-gate-w (float-array (take 10 (repeat 0.01)))
                                     :output-gate-wr (float-array (take 100 (repeat 0.01)))
                                     :output-gate-bias (float-array (take 10 (repeat 1)))
                                     :peephole nil :unit-num 10}
                                    (float-array [2])
                                    (float-array (take 10 (repeat 2)))
                                    (float-array (float-array (take 10 (repeat 10)))))
          a2 (:activation result2)
          s2 (:state result2)]
      (is (= (vec a)
             (vec a2)
             (take 10 (repeat (float 0.77205396)))))
      (is (= (vec (:lstm s))
             (vec a2)
             (take 10 (repeat (float 0.77205396)))))
      (is (= (vec (:block s))
             (vec (:block s2))
             (take 10 (repeat (float 23.0)))))
      (is (= (vec (:input-gate  s))
             (vec (:input-gate  s2))
             (take 10 (repeat (float -0.21999998)))))
      (is (= (vec (:forget-gate s))
             (vec (:forget-gate s2))
             (take 10 (repeat (float 0.21999998)))))
      (is (= (vec (:output-gate s))
             (vec (:output-gate s2))
             (take 10 (repeat (float 1.22)))))
      (is (= (vec (:cell-state  s))
             (vec (:cell-state  s2))
             (take 10 (repeat (float 5.993013)))))))

  (testing "lstm-model-output"
    (let [result (->> (mapv #(float-array (take (:unit-num %) (repeat 0))) (:layers sample-w-network-deep))
                      (lstm-model-output sample-w-network-deep (float-array [1])
                                         [(float-array (take 20 (repeat 0))) (float-array (take 20 (repeat 0)))]))
          f (first (:state result))
          ss (second (:state result))]
      (is (= (vec f) [(float 1)]))
      (is (= (vec (:lstm ss)) (take 20 (repeat (float -0.05900606)))))
      (is (= (vec (:block ss)) (take 20 (repeat (float -0.8999999985098839)))))
      (is (= (vec (:input-gate  ss)) (take 20 (repeat (float -0.8999999985098839)))))
      (is (= (vec (:forget-gate ss)) (take 20 (repeat (float -0.8999999985098839)))))
      (is (= (vec (:output-gate  ss)) (take 20 (repeat (float -0.8999999985098839)))))
      (is (= (vec (:cell-state ss)) (take 20 (repeat (float -0.20704626)))))
      (is (= (vec (last (:state result))) [(float -1.2248842418193817)]))
      (is (= (vec (second (:activation result)))
             (take 20 (repeat (float -0.05900606280435081)))))
      (is (= (->> result :activation rest rest first vec)
             (take 20 (repeat (float -0.053436052)))))
      (is (= (vec (last (:activation result)))
             [(float -1.2248842418193817)]))))
  (testing "lstm-model-output in sparse model"
    (let [result (->> (mapv #(float-array (take (:unit-num %) (repeat 0))) (:layers sample-w-network-sparse))
                      (lstm-model-output sample-w-network-sparse {"language" (float 1)}
                                         [(float-array (take 10 (repeat 0))) (float-array (take 10 (repeat 0)))]))
          f (first (:state result))
          ss (second (:state result))
          result2 (->> (mapv #(float-array (take (:unit-num %) (repeat 0))) (:layers sample-w-network))
                       (lstm-model-output sample-w-network (float-array [0 1 0])
                                          [(float-array (take 10 (repeat 0))) (float-array (take 10 (repeat 0)))]))

          f2 (first (:state result2))
          ss2 (second (:state result2))]
      (is (= (vec f) [["language" (float 1)]]))
      (is (= (vec (:lstm ss))
             (vec (:lstm ss2))
             (take 10 (repeat (float -0.05900606)))))
      (is (= (vec (:input-gate  ss))
             (vec (:input-gate ss2))
             (take 10 (repeat (float -0.8999999985098839)))))
      (is (= (vec (:forget-gate ss))
             (vec (:forget-gate ss2))
             (take 10 (repeat (float -0.8999999985098839)))))
      (is (= (vec (:output-gate  ss))
             (vec (:output-gate ss2))
             (take 10 (repeat (float -0.8999999985098839)))))
      (is (= (vec (:cell-state ss))
             (vec (:cell-state ss2))
             (take 10 (repeat (float -0.20704626)))))))

  (testing "sequential-output"
    (let [result (sequential-output sample-w-network-deep (map float-array [[1] [1] [2]]))]
      (is (= 3 (count result))))
    (let [result1 (sequential-output sample-w-network-deep (map float-array [[1] [1]]))
          result2 (sequential-output sample-w-network-deep (map float-array [[2] [1]]))
          it1 (vec (nth (:activation (first result1)) 2))
          it2 (vec (nth (:activation (first result2)) 2))]
      (is (not= it1 it2))
      (is (= it1 (take 20 (repeat (float -0.053436052)))))
      (is (= it2 (take 20 (repeat (float -0.05785076)))))))
  (testing "sequential-output in sparse model"
    (let [result1 (sequential-output sample-w-network-sparse [{"language" (float 1)} {"processing" (float 1)}])
          result2 (sequential-output sample-w-network [(float-array [0 1 0]) (float-array [0 0 1])])
          out1 (vec (nth (:activation (first result1)) 2))
          out2 (vec (nth (:activation (first result2)) 2))
          out3 (vec (nth (:activation (second result1)) 2))
          out4 (vec (nth (:activation (second result2)) 2))]
      (is (= out1 out2 (take 3 (repeat (float -1.0590061)))))
      (is (= out3 out4 (take 3 (repeat (float -1.0734694)))))))
  (testing "lstm-delta"
    (let [result (lstm-delta (float-array [10]) {:input-gate-delta (float-array [2]) :forget-gate-delta (float-array [2]) :cell-state-delta (float-array [2])}
                             {:output-gate (float-array [1]) :forget-gate (float-array [1]) :input-gate (float-array [1]) :block (float-array [1.2]) :cell-state (float-array[2])}
                             {:forget-gate (float-array [1])}
                             (float-array[1]) #{} nil nil nil)]
      (is (= (vec (:output-gate-delta result)) [(float 1.8953932621681382)]))
      (is (= (vec (:cell-state-delta result)) [(float 1.9786160732219278)]))
      (is (= (vec (:block-delta result)) [(float 0.44120657)]))
      (is (= (vec (:forget-gate-delta result)) [(float 0.38901953129883265)]))
      (is (= (vec (:input-gate-delta result)) [(float 0.32430795)]))))
  (testing "lstm-delta with peephole"
    (let [result (lstm-delta (float-array [10]) {:input-gate-delta (float-array [2]) :forget-gate-delta (float-array [2]) :cell-state-delta (float-array [2])}
                             {:output-gate (float-array [1]) :forget-gate (float-array [1]) :input-gate (float-array [1]) :block (float-array [1.2]) :cell-state (float-array[2])}
                             {:forget-gate (float-array [1])}
                             (float-array[1])
                             #{:input-gate :forget-gate :output-gate} (float-array [2]) (float-array [3]) (float-array[4]))]
      (is (= (vec (:output-gate-delta result)) [(float 1.8953932621681382)]))
      (is (= (vec (:cell-state-delta result)) [(float 19.56018912189448)]))
      (is (= (vec (:block-delta result)) [(float 4.3616767)]))
      (is (= (vec (:forget-gate-delta result)) [(float 3.8457663)]))
      (is (= (vec (:input-gate-delta result)) [(float 3.2060409)]))))

  (testing "hidden-layer-delta"
    (let [result (hidden-layer-delta true  :stack
                                     (last (:layers sample-w-network-deep))
                                     nil
                                     (second (:layers sample-w-network-deep))
                                     0
                                     nil
                                     (float-array [5])
                                     nil
                                     {:block-delta (float-array (take 20 (repeat 1))) :input-gate-delta (float-array (take 20 (repeat 2)))
                                      :forget-gate-delta (float-array (take 20 (repeat 1))) :output-gate-delta (float-array (take 20 (repeat 1)))})]
      (is (= (vec result) (take 20 (repeat (float 10.5))))))
    (let [result (hidden-layer-delta false :stack
                                     (last   (:layers sample-w-network-deep));as output-layer
                                     (second (:layers sample-w-network-deep));as above-layer
                                     (first  (:layers sample-w-network-deep));as self-layer
                                     0 ;as offset-output
                                     0;as offset-above
                                     (float-array [5])
                                     {:block-delta (float-array (take 20 (repeat 1.5))) :input-gate-delta (float-array (take 20 (repeat 2)))
                                      :forget-gate-delta (float-array (take 20 (repeat 1))) :output-gate-delta (float-array (take 20 (repeat 1)))}
                                     {:block-delta (float-array (take 20 (repeat 1))) :input-gate-delta (float-array (take 20 (repeat 2)))
                                      :forget-gate-delta (float-array (take 20 (repeat 1))) :output-gate-delta (float-array (take 20 (repeat 1)))})]
      (is (= (vec result) (take 20 (repeat (float 21.0))))))
    (let [result (hidden-layer-delta true  :deep
                                     (last (:layers sample-w-network-deep))
                                     nil
                                     (second (:layers sample-w-network-deep))
                                     (* 1 20)
                                     nil
                                     (float-array [5])
                                     nil
                                     {:block-delta (float-array (take 20 (repeat 1))) :input-gate-delta (float-array (take 20 (repeat 2)))
                                      :forget-gate-delta (float-array (take 20 (repeat 1))) :output-gate-delta (float-array (take 20 (repeat 1)))})]
      (is (= (vec result) (take 20 (repeat (float 10.5))))))
    (let [result (hidden-layer-delta false :deep
                                     (last   (:layers sample-w-network-deep));as output-layer
                                     (second (:layers sample-w-network-deep));as above-layer
                                     (first  (:layers sample-w-network-deep));as self-layer
                                     0 ;as offset-output
                                     (* 1 20);as offset-above
                                     (float-array [5])
                                     {:block-delta (float-array (take 20 (repeat 1.5))) :input-gate-delta (float-array (take 20 (repeat 2)))
                                      :forget-gate-delta (float-array (take 20 (repeat 1))) :output-gate-delta (float-array (take 20 (repeat 1)))}
                                     {:block-delta (float-array (take 20 (repeat 1))) :input-gate-delta (float-array (take 20 (repeat 2)))
                                      :forget-gate-delta (float-array (take 20 (repeat 1))) :output-gate-delta (float-array (take 20 (repeat 1)))})]
      (is (= (vec result) (take 20 (repeat (float 21.5)))))))
  (testing "time-fixed-back-propagation"
    (let [it (sequential-output sample-w-network-deep (mapv float-array [[1] [2] [3]]))
          result (time-fixed-back-propagation sample-w-network-deep (nth it 2) (second it) (first it) (float-array [1])
                                              (take 2 (repeat {:block-delta (float-array (take 20 (repeat 1))) :input-gate-delta (float-array (take 20 (repeat 1)))
                                                               :forget-gate-delta (float-array (take 20 (repeat 1))) :output-gate-delta (float-array (take 20 (repeat 1)))
                                                               :cell-state-delta (float-array (take 20 (repeat 1)))})) nil)
          f (first result)
          s (second result)]
      (is (= (vec (:output-gate-delta f)) (take 20 (repeat (float -0.3955663)))))
      (is (= (vec (:cell-state-delta f))  (take 20 (repeat (float 2.0936916)))))
      (is (= (vec (:block-delta  f))      (take 20 (repeat (float 0.20956239)))))
      (is (= (vec (:forget-gate-delta f)) (take 20 (repeat (float -0.08249492)))))
      (is (= (vec (:input-gate-delta f))  (take 20 (repeat (float -0.3174528)))))

      (is (= (vec (:output-gate-delta s)) (take 20 (repeat (float -0.37563837)))))
      (is (= (vec (:cell-state-delta s))  (take 20 (repeat (float 2.0091796)))))
      (is (= (vec (:block-delta  s))      (take 20 (repeat (float 0.2760604 )))))
      (is (= (vec (:forget-gate-delta s)) (take 20 (repeat (float -0.085583754)))))
      (is (= (vec (:input-gate-delta s))  (take 20 (repeat (float -0.2996646)))))))
  (testing "param-delta"
    (let [result (param-delta (float-array (range 10)) (float-array (range 10)))]
      (is (= (vec (:w-delta result))
             (mapv float [0 0 0 0 0 0 0 0 0 0
                          0 1 2 3 4 5 6 7 8 9
                          0 2 4 6 8 10 12 14 16 18
                          0 3 6 9 12 15 18 21 24 27
                          0 4 8 12 16 20 24 28 32 36
                          0 5 10 15 20 25 30 35 40 45
                          0 6 12 18 24 30 36 42 48 54
                          0 7 14 21 28 35 42 49 56 63
                          0 8 16 24 32 40 48 56 64 72
                          0 9 18 27 36 45 54 63 72 81])))
      (is (= (vec (:bias-delta result)) (mapv float  [0 1 2 3 4 5 6 7 8 9])))))
  (testing "lstm-param-delta"
    (let [it (sequential-output sample-w-network-deep (map float-array [[1] [2] [3]]))
          result (lstm-param-delta sample-w-network-deep
                                   (take 2 (repeat {:block-delta (float-array (take 20 (repeat 1)))
                                                    :input-gate-delta (float-array (take 20 (repeat 1)))
                                                    :forget-gate-delta (float-array (take 20 (repeat 1)))
                                                    :output-gate-delta (float-array (take 20 (repeat 1)))}))
                                   (second it)
                                   (first it))
          layer-lower (first  result)
          layer-upper (second result)]
      ;lower layer
      (is (= (vec (:block-w-delta layer-lower))  (take 20 (repeat (float 2)))))
      (is (= (vec (:block-wr-delta layer-lower)) (take 400 (repeat (float -0.05900606280435081)))))
      (is (= (vec (:input-gate-w-delta layer-lower)) (take 20 (repeat (float 2)))))
      (is (= (vec (:input-gate-wr-delta layer-lower))  (take 400 (repeat (float -0.05900606280435081)))))
      (is (= (vec (:forget-gate-w-delta layer-lower))  (take 20 (repeat (float 2)))))
      (is (= (vec (:forget-gate-wr-delta layer-lower)) (take 400 (repeat (float -0.05900606280435081)))))
      (is (= (vec (:output-gate-w-delta layer-lower))  (take 20 (repeat (float 2)))))
      (is (= (vec (:output-gate-wr-delta layer-lower)) (take 400 (repeat (float -0.05900606280435081)))))
      (is (= (vec (:block-bias-delta layer-lower)) (take 20 (repeat (float 1)))))
      (is (= (vec (:input-gate-bias-delta layer-lower)) (take 20 (repeat (float 1)))))
      (is (= (vec (:forget-gate-bias-delta layer-lower)) (take 20 (repeat (float 1)))))
      (is (= (vec (:output-gate-bias-delta layer-lower)) (take 20 (repeat (float 1)))))
      (is (= (vec (:peephole-input-gate-delta  layer-lower)) (take 20 (repeat (float -0.20704625565929818)))))
      (is (= (vec (:peephole-forget-gate-delta layer-lower)) (take 20 (repeat (float -0.20704625565929818)))))
      (is (= (vec (:peephole-output-gate-delta layer-lower)) (take 20 (repeat (float -0.20704625565929818)))))
      ;upper layer
      (is (= (vec (:block-w-delta layer-upper))   (flatten (take 20 (repeat (flatten (concat [(float 2)]  (take 20 (repeat (float -0.076323025))))))))))
      (is (= (vec (:block-wr-delta layer-upper))  (take 400 (repeat (float -0.053436052)))))
      (is (= (vec (:input-gate-w-delta layer-upper))  (flatten (take 20  (repeat (flatten (concat [(float 2)]  (take 20 (repeat (float -0.076323025))))))))))
      (is (= (vec (:input-gate-wr-delta layer-upper))   (take 400 (repeat (float -0.053436052)))))
      (is (= (vec (:forget-gate-w-delta layer-upper))  (flatten (take 20  (repeat (flatten (concat [(float 2)]  (take 20 (repeat (float -0.076323025))))))))))
      (is (= (vec (:forget-gate-wr-delta layer-upper))  (take 400 (repeat (float -0.053436052)))))
      (is (= (vec (:output-gate-w-delta layer-upper))  (flatten (take 20  (repeat (flatten (concat [(float 2)]  (take 20 (repeat (float -0.076323025))))))))))
      (is (= (vec (:output-gate-wr-delta layer-upper))  (take 400 (repeat (float -0.053436052)))))
      (is (= (vec (:block-bias-delta layer-upper)) (take 20 (repeat (float 1)))))
      (is (= (vec (:input-gate-bias-delta layer-upper)) (take 20 (repeat (float 1)))))
      (is (= (vec (:forget-gate-bias-delta layer-upper)) (take 20 (repeat (float 1)))))
      (is (= (vec (:output-gate-bias-delta layer-upper)) (take 20 (repeat (float 1)))))
      (is (= (vec (:peephole-input-gate-delta  layer-upper)) (take 20 (repeat (float  -0.20411873)))))
      (is (= (vec (:peephole-forget-gate-delta layer-upper)) (take 20 (repeat (float  -0.20411873)))))
      (is (= (vec (:peephole-output-gate-delta layer-upper)) (take 20 (repeat (float  -0.20411873)))))))

  (testing "lstm-param-delta in sparse model"
    (let [it (sequential-output sample-w-network-sparse [{"language" 1} {"processing" 1}])
          result (lstm-param-delta sample-w-network-sparse
                                   (take 2 (repeat {:block-delta       (float-array (take 10 (repeat 1)))
                                                    :input-gate-delta  (float-array (take 10 (repeat 1)))
                                                    :forget-gate-delta (float-array (take 10 (repeat 1)))
                                                    :output-gate-delta (float-array (take 10 (repeat 1)))}))
                                   (second it)
                                   (first it))
          layer-lower (first  result)
          layer-upper (second result)
          it2 (sequential-output sample-w-network [(float-array [0 1 0]) (float-array [0 0 1])])
          result2 (lstm-param-delta sample-w-network
                                    (take 2 (repeat {:block-delta       (float-array (take 10 (repeat 1)))
                                                     :input-gate-delta  (float-array (take 10 (repeat 1)))
                                                     :forget-gate-delta (float-array (take 10 (repeat 1)))
                                                     :output-gate-delta (float-array (take 10 (repeat 1)))}))
                                    (second it2)
                                    (first it2))
          layer-lower2 (first  result2)
          layer-upper2 (second result2)
          ws (get (:sparses-delta layer-lower) "processing")]
      (is (= (vec (:block-w-delta ws))
             (->> (take-nth 3 (drop 2 (vec (:block-w-delta layer-lower2)))))))
      (is (= (vec (:input-gate-w-delta ws))
             (->> (take-nth 3 (drop 2 (vec (:input-gate-w-delta layer-lower2)))))))
      (is (= (vec (:forget-gate-w-delta ws))
             (->> (take-nth 3 (drop 2 (vec (:forget-gate-w-delta layer-lower2)))))))
      (is (= (vec (:output-gate-w-delta ws))
             (->> (take-nth 3 (drop 2 (vec (:output-gate-w-delta layer-lower2)))))))))

  (testing "lstm-delta-zeros"
    (let [result (lstm-delta-zeros sample-w-network-deep)]
      (is (= (mapv #(count (:block-delta %)) result) [1 20 20]))
      (is (= (mapv #(count (:input-gate-delta %)) result) [1 20 20]))
      (is (= (mapv #(count (:forget-gate-delta %)) result) [1 20 20]))
      (is (= (mapv #(count (:output-gate-delta %)) result) [1 20 20]))))
  (testing "bptt-partial"
    (let [result (bptt-partial sample-w-network-deep (sequential-output sample-w-network-deep [(float-array [1])(float-array [2])]) (float-array [1]))
          f (first result)
          s (second result)]
      (is (= (count (:block-w-delta              f)) 20))
      (is (= (count (:block-wr-delta             f)) 400))
      (is (= (count (:input-gate-w-delta         f)) 20))
      (is (= (count (:input-gate-wr-delta        f)) 400))
      (is (= (count (:forget-gate-w-delta        f)) 20))
      (is (= (count (:forget-gate-wr-delta       f)) 400))
      (is (= (count (:output-gate-w-delta        f)) 20))
      (is (= (count (:output-gate-wr-delta       f)) 400))
      (is (= (count (:block-bias-delta           f)) 20))
      (is (= (count (:input-gate-bias-delta      f)) 20))
      (is (= (count (:forget-gate-bias-delta     f)) 20))
      (is (= (count (:output-gate-bias-delta     f)) 20))
      (is (= (count (:peephole-input-gate-delta  f)) 20))
      (is (= (count (:peephole-forget-gate-delta f)) 20))
      (is (= (count (:peephole-output-gate-delta f)) 20))
      (is (= (count (:block-w-delta              s)) 420))
      (is (= (count (:block-wr-delta             s)) 400))
      (is (= (count (:input-gate-w-delta         s)) 420))
      (is (= (count (:input-gate-wr-delta        s)) 400))
      (is (= (count (:forget-gate-w-delta        s)) 420))
      (is (= (count (:forget-gate-wr-delta       s)) 400))
      (is (= (count (:output-gate-w-delta        s)) 420))
      (is (= (count (:output-gate-wr-delta       s)) 400))
      (is (= (count (:block-bias-delta           s)) 20))
      (is (= (count (:input-gate-bias-delta      s)) 20))
      (is (= (count (:forget-gate-bias-delta     s)) 20))
      (is (= (count (:output-gate-bias-delta     s)) 20))
      (is (= (count (:peephole-input-gate-delta  s)) 20))
      (is (= (count (:peephole-forget-gate-delta s)) 20))
      (is (= (count (:peephole-output-gate-delta s)) 20))))
  (testing "bptt-partial in sparse model"
    (let [result (bptt-partial sample-w-network (sequential-output sample-w-network [(float-array [0 1 0])(float-array [0 0 1])]) (float-array [1 0 0]))
          result-sparse (bptt-partial sample-w-network-sparse (sequential-output sample-w-network-sparse [{"language" (float 1)} {"processing" (float 1)}]) (float-array [1 0 0]))
          f (first result)
          s (second result)
          fs (first result-sparse)
          ss (second result-sparse)
          it (:sparses-delta fs)]
      (is (= (count (:block-w-delta (get it "natural"))) 0))
      (is (= (count (:block-w-delta (get it "language"))) 10))
      (is (= (count (:block-w-delta (get it "processing"))) 10))
      (is (= (vec (:block-w-delta (get it "processing")))
             (->> (take-nth 3 (drop 2 (:block-w-delta f))))))
      (is (= (vec (:input-gate-w-delta (get it "processing")))
             (->> (take-nth 3 (drop 2 (:input-gate-w-delta f))))))
      (is (= (vec (:forget-gate-w-delta (get it "processing")))
             (->> (take-nth 3 (drop 2 (:forget-gate-w-delta f))))))
      (is (= (vec (:output-gate-w-delta (get it "processing")))
             (->> (take-nth 3 (drop 2 (:output-gate-w-delta f))))))
      ))

  (testing "bptt"
    (let [result (bptt sample-w-network-deep [(float-array [1])(float-array [2])] [(float-array [1]) (float-array [3])])
          f (first result)
          s (second result)]
      (is (= (count (:block-w-delta              f)) 20))
      (is (= (count (:block-wr-delta             f)) 400))
      (is (= (count (:input-gate-w-delta         f)) 20))
      (is (= (count (:input-gate-wr-delta        f)) 400))
      (is (= (count (:forget-gate-w-delta        f)) 20))
      (is (= (count (:forget-gate-wr-delta       f)) 400))
      (is (= (count (:output-gate-w-delta        f)) 20))
      (is (= (count (:output-gate-wr-delta       f)) 400))
      (is (= (count (:block-bias-delta           f)) 20))
      (is (= (count (:input-gate-bias-delta      f)) 20))
      (is (= (count (:forget-gate-bias-delta     f)) 20))
      (is (= (count (:output-gate-bias-delta     f)) 20))
      (is (= (count (:peephole-input-gate-delta  f)) 20))
      (is (= (count (:peephole-forget-gate-delta f)) 20))
      (is (= (count (:peephole-output-gate-delta f)) 20))
      (is (= (count (:block-w-delta              s)) 420))
      (is (= (count (:block-wr-delta             s)) 400))
      (is (= (count (:input-gate-w-delta         s)) 420))
      (is (= (count (:input-gate-wr-delta        s)) 400))
      (is (= (count (:forget-gate-w-delta        s)) 420))
      (is (= (count (:forget-gate-wr-delta       s)) 400))
      (is (= (count (:output-gate-w-delta        s)) 420))
      (is (= (count (:output-gate-wr-delta       s)) 400))
      (is (= (count (:block-bias-delta           s)) 20))
      (is (= (count (:input-gate-bias-delta      s)) 20))
      (is (= (count (:forget-gate-bias-delta     s)) 20))
      (is (= (count (:output-gate-bias-delta     s)) 20))
      (is (= (count (:peephole-input-gate-delta  s)) 20))
      (is (= (count (:peephole-forget-gate-delta s)) 20))
      (is (= (count (:peephole-output-gate-delta s)) 20))))
  (testing "bptt in sparse model"
    (let [result (bptt sample-w-network-sparse
                       [{"language" (float 1)} {"processing" (float 1)}]
                       (mapv float-array [[1 2 3] [5 5 5]]))
          f (first result)
          s (second result)
          it (:sparses-delta f)]
      (is (= (count (:block-w-delta (get it "natural"))) 0))
      (is (= (count (:block-w-delta (get it "language"))) 10))
      (is (= (count (:block-w-delta (get it "processing"))) 10))))

  (testing "update-model"
    (let [result (:layers (update-model sample-w-network-deep (bptt sample-w-network-deep (mapv float-array [[1] [2] [3]]) (mapv float-array [[2] [4] [6]])) 0.1))
          lower-hidden (first result)
          upper-hidden (second result)]
      (is (= (count (:block-w lower-hidden)) 20))
      (is (= (count (:block-wr lower-hidden)) 400))
      (is (= (count (:input-gate-w lower-hidden)) 20))
      (is (= (count (:input-gate-wr lower-hidden)) 400))
      (is (= (count (:forget-gate-w lower-hidden)) 20))
      (is (= (count (:forget-gate-wr lower-hidden)) 400))
      (is (= (count (:output-gate-w lower-hidden)) 20))
      (is (= (count (:output-gate-wr lower-hidden)) 400))

      (is (= (vec (:block-w lower-hidden))
             (take 20(repeat (float 0.11089216)))))
      (is (= (vec (:block-wr lower-hidden))
             (take 400 (repeat (float 0.09974099)))))
      (is (= (vec (:input-gate-w lower-hidden))
             (take 20 (repeat (float 0.08482326)))))
      (is (= (vec (:input-gate-wr lower-hidden))
             (take 400 (repeat (float 0.10036282)))))
      (is (= (vec (:forget-gate-w lower-hidden))
             (take 20 (repeat (float 0.09582109)))))
      (is (= (vec (:forget-gate-wr lower-hidden))
             (take 400 (repeat (float 0.10011148)))))
      (is (= (vec (:output-gate-w lower-hidden))
             (take 20 (repeat (float 0.0828144)))))
      (is (= (vec (:output-gate-wr lower-hidden))
             (take 400 (repeat (float 0.10042779)))))
      ;peephole
      (is (= (vec (:input-gate-peephole lower-hidden))
             (take 20 (repeat (float -0.09872089)))))
      (is (= (vec (:forget-gate-peephole lower-hidden))
             (take 20 (repeat (float -0.09960681)))))
      (is (= (vec (:output-gate-peephole lower-hidden))
             (take 20 (repeat (float -0.09849062)))))

      (is (= (vec (:input-gate-bias  lower-hidden)) (take 20 (repeat (float -1.0070037)))))
      (is (= (vec (:forget-gate-bias lower-hidden)) (take 20 (repeat (float -1.0016046)))))
      (is (= (vec (:output-gate-bias lower-hidden)) (take 20 (repeat (float -1.007143)))))

      (is (= (count (:block-w upper-hidden)) 420))
      (is (= (count (:block-wr upper-hidden)) 400))
      (is (= (count (:input-gate-w upper-hidden)) 420))
      (is (= (count (:input-gate-wr upper-hidden)) 400))
      (is (= (count (:forget-gate-w upper-hidden)) 420))
      (is (= (count (:forget-gate-wr upper-hidden)) 400))
      (is (= (count (:output-gate-w upper-hidden)) 420))
      (is (= (count (:output-gate-wr upper-hidden)) 400))

      (is (= (vec (:block-w upper-hidden))
             (flatten (take 20 (repeat (concat [(float 0.11427468)]
                                               (take 20 (repeat (float 0.09950024)))))))))
      (is (= (vec (:block-wr upper-hidden))
             (take 400 (repeat (float 0.09969999)))))))
  (testing "update-model in sparse model"
    (let [result (:layers (update-model sample-w-network-sparse
                                        (bptt sample-w-network-sparse
                                              [{"language" (float 1)} {"processing" (float 1)}]
                                              (mapv float-array [[1 2 3] [5 5 5]]))
                                        0.1))
          hidden (first result)
          sparses (:sparses hidden)]
      (is (= (count (:block-w (get sparses "language"))) 10))
      (is (= (count (:input-gate-w (get sparses "language"))) 10))
      (is (= (count (:forget-gate-w (get sparses "language"))) 10))
      (is (= (count (:output-gate-w (get sparses "language"))) 10))
      (is (= (count (:block-wr hidden)) 100))
      (is (= (count (:input-gate-wr hidden)) 100))
      (is (= (count (:forget-gate-wr hidden)) 100))
      (is (= (count (:output-gate-wr hidden)) 100))
      )))

