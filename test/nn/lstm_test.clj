(ns nn.lstm-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [prism.nn.lstm :refer :all]))


(def sample-w-network
  {:model-type :nil
   :unit-nums [3 10 3]
   :input-type :dense
   :output-type :binary-classification
   :hidden {:unit-type :lstm
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
   :output {:activation :sigmoid,
            :layer-type :output,
            :unit-num 3
            :w {"prediction1" (float-array (take 10 (repeat 0.1)))
                "prediction2" (float-array (take 10 (repeat 0.1)))
                "prediction3" (float-array (take 10 (repeat 0.1)))}
            :bias {"prediction1" (float-array [-1])
                   "prediction2" (float-array [-1])
                   "prediction3" (float-array [-1])}}})

(def sample-w-network-sparse
  {:model-type :nil
   :input-type :sparse
   :output-type :binary-classification
   :unit-nums [3 10 3]
   :hidden {:sparses {"natural" {:block-w (float-array (take 10 (repeat 0.1)))
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
            :unit-num 10
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
            :peephole #{:input-gate :forget-gate :output-gate}}
   :output {:activation :sigmoid,
            :layer-type :output,
            :unit-num 3
            :w {"prediction1" (float-array (take 10 (repeat 0.1)))
                "prediction2" (float-array (take 10 (repeat 0.1)))
                "prediction3" (float-array (take 10 (repeat 0.1)))}
            :bias {"prediction1" (float-array [-1])
                   "prediction2" (float-array [-1])
                   "prediction3" (float-array [-1])}}})

(def sample-w-network-prediction
  {:model-type :nil
   :input-type :sparse
   :output-type :prediction
   :unit-nums [3 10 1]
   :hidden {:sparses {"natural" {:block-w (float-array (take 10 (repeat 0.1)))
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
            :unit-num 10
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
            :peephole #{:input-gate :forget-gate :output-gate}}
   :output {:activation :linear,
            :layer-type :output,
            :unit-num 3
            :w {"prediction" (float-array (take 10 (repeat 0.1)))}
            :bias {"prediction" (float-array [-1])}}})

(deftest lstm-test
  (testing "lstm-activation"
    (let [result (lstm-activation sample-w-network
                                  (float-array (take 3 (repeat 2)))
                                  (float-array (take 10 (repeat 2)))
                                  (float-array (take 10 (repeat 2))))
          a (:activation result)
          s (:state result)]
      (is (= (vec a) (take 10 (repeat (float 0.787542)))))
      (is (= (vec (:lstm s)) (take 10 (repeat (float 0.787542)))))
      (is (= (vec (:block s)) (take 10 (repeat (float 1.5999999)))))
      (is (= (vec (:input-gate  s)) (take 10 (repeat (float 1.3999999)))))
      (is (= (vec (:forget-gate s)) (take 10 (repeat (float 1.3999999)))))
      (is (= (vec (:output-gate s)) (take 10 (repeat (float 1.3999999)))))
      (is (= (vec (:cell-state  s)) (take 10 (repeat (float 2.3437154)))))))

  (testing "lstm-activation with sparse"
    (let [result (lstm-activation sample-w-network-sparse
                                  {"natural" (float 2)}
                                  (float-array (take 10 (repeat 2)))
                                  (float-array (take 10 (repeat 2))))
          a (:activation result)
          s (:state result)
          result2 (lstm-activation sample-w-network
                                   (float-array [2 0 0])
                                   (float-array (take 10 (repeat 2)))
                                   (float-array (take 10 (repeat 2))))
          a2 (:activation result2)
          s2 (:state result2)]
      (is (= (vec a)
             (vec a2)
             (take 10 (repeat (float 0.70821303)))))
      (is (= (vec (:lstm s))
             (vec a2)
             (take 10 (repeat (float 0.70821303)))))
      (is (= (vec (:block s))
             (vec (:block s2))
             (take 10 (repeat (float 1.2)))))
      (is (= (vec (:input-gate  s))
             (vec (:input-gate  s2))
             (take 10 (repeat (float 1)))))
      (is (= (vec (:forget-gate s))
             (vec (:forget-gate s2))
             (take 10 (repeat (float 1)))))
      (is (= (vec (:output-gate s))
             (vec (:output-gate s2))
             (take 10 (repeat (float 1)))))
      (is (= (vec (:cell-state  s))
             (vec (:cell-state  s2))
             (take 10 (repeat (float 2.0715675)))))))

  (testing "lstm-model-output with dense input"
    (let [result (lstm-model-output sample-w-network
                                    (float-array [1 0 -10]); as x-input
                                    #{"prediction1" "prediction2" "prediction3"}
                                    (float-array (take 10 (repeat 0)))
                                    (float-array (take (:unit-num (:hidden sample-w-network)) (repeat 0) )))
          f  (get-in result [:state :input])
          ss (get-in result [:state :hidden])]
      (is (= (vec f) (map float [1 0 -10])))
      (is (= (vec (:lstm ss)) (take 10 (repeat (float -0.016104385)))))
      (is (= (vec (:block ss)) (take 10 (repeat (float -1.9)))))
      (is (= (vec (:input-gate  ss)) (take 10 (repeat (float -1.9)))))
      (is (= (vec (:forget-gate ss)) (take 10 (repeat (float -1.9)))))
      (is (= (vec (:output-gate  ss)) (take 10 (repeat (float -1.9)))))
      (is (= (vec (:cell-state ss)) (take 10 (repeat (float -0.12441459)))))
      (is (= (vec (:hidden (:activation result)))
             (take 10 (repeat (float -0.016104385)))))
      (is (= (->> result :activation :hidden vec)
             (take 10 (repeat (float -0.016104385)))))
      (is (= (->> result :activation :output (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.26578692)
              "prediction1" (float 0.26578692)
              "prediction3" (float 0.26578692)}))))

  (testing "lstm-model-output with sparse input"
    (let [result (lstm-model-output sample-w-network-sparse
                                    {"language" (float 1)}
                                    #{"prediction1" "prediction2" "prediction3"}
                                    (float-array (take 10 (repeat 0)))
                                    (float-array (take 10 (repeat 0))))
          f  (->> result :state :input)
          ss (->> result :state :hidden)
          result2 (lstm-model-output sample-w-network
                                     (float-array [0 1 0])
                                     #{"prediction1" "prediction2" "prediction3"}
                                     (float-array (take 10 (repeat 0)))
                                     (float-array (take 10 (repeat 0))))
          f2  (->> result2 :state :input)
          ss2 (->> result2 :state :hidden)]
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
    (let [result (sequential-output sample-w-network
                                    (map float-array [[0 1 0] [0 1 0] [0 2 0]])
                                    [:skip :skip #{"prediction1" "prediction2" "prediction3"}])]
      (is (= 3 (count result))))
    (let [result1 (sequential-output sample-w-network (map float-array [[1 0 0] [1 0 0]]) [:skip #{"prediction1" "prediction2" "prediction3"}])
          result2 (sequential-output sample-w-network (map float-array [[2 0 0] [1 0 0]]) [:skip #{"prediction1" "prediction2" "prediction3"}])
          it1 (vec (:output (:activation (last result1))))
          it2 (vec (:output (:activation (last result2))))]
      (is (not= it1 it2))
      (is (= (->> it1 (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.25474387) "prediction1" (float 0.25474387) "prediction3" (float 0.25474387)}))
      (is (= (->> it2 (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.254815) "prediction1" (float 0.254815) "prediction3" (float 0.254815)}))))

  (testing "sequential-output in sparse model"
    (let [result1 (sequential-output sample-w-network-sparse
                                     [{"language" (float 1)} {"processing" (float 1)}]
                                     [:skip #{"prediction1" "prediction2" "prediction3"}])
          result2 (sequential-output sample-w-network
                                     [(float-array [0 1 0]) (float-array [0 0 1])]
                                     [:skip #{"prediction1" "prediction2" "prediction3"}])
          out1 (vec (:hidden (:activation (first result1))))
          out2 (vec (:hidden (:activation (first result2))))
          out3 (vec (:hidden (:activation (second result1))))
          out4 (vec (:hidden (:activation (second result2))))]
      (is (= out1 out2 (take 10 (repeat (float -0.05900606)))))
      (is (= out3 out4 (take 10 (repeat (float -0.07346944)))))))

  ;; Back Propagation Through Time ;;
  (comment
    (testing "output-param-delta"
      (let [result (->> (output-param-delta {"A" 0.5 "B" 0 "C" -0.5} 10 (float-array (range 10)))
                        (reduce (fn [acc {:keys [item w-delta bias-delta]}]
                                  (assoc acc item {:w-delta (mapv float w-delta) :bias-delta bias-delta}))
                                {}))
            {:strs [A B C]} result]
        (is (= A {:w-delta (map float [0.0 0.5 1.0 1.5 2.0 2.5 3.0 3.5 4.0 4.5]) :bias-delta (float 0.5)}))
        (is (= B {:w-delta (take 10 (repeat (float 0))) :bias-delta 0}))
        (is (= C {:w-delta (map float [-0.0 -0.5 -1.0 -1.5 -2.0 -2.5 -3.0 -3.5 -4.0 -4.5]) :bias-delta (float -0.5)}))))
    )
  (testing "lstm-part-delta with peephole, assumed 1 lstm unit"
    (let [result (lstm-part-delta 1
                                  (float-array [10])
                                  {:input-gate-delta (float-array [2]) :forget-gate-delta (float-array [2]) :cell-state-delta (float-array [2])}
                                  {:output-gate (float-array [1]) :forget-gate (float-array [1]) :input-gate (float-array [1]) :block (float-array [1.2]) :cell-state (float-array[2])}
                                  {:forget-gate (float-array [1])}
                                  (float-array[1])
                                  (float-array [2])
                                  (float-array [3])
                                  (float-array[4]))]
      (is (= (vec (:output-gate-delta result)) [(float 1.8953932621681382)]))
      (is (= (vec (:cell-state-delta result)) [(float 19.56018912189448)]))
      (is (= (vec (:block-delta result)) [(float 4.3616767)]))
      (is (= (vec (:forget-gate-delta result)) [(float 3.8457663)]))
      (is (= (vec (:input-gate-delta result)) [(float 3.2060409)]))))


  (testing "lstm-param-delta"
    (let [it (sequential-output sample-w-network (map float-array [[2 1 -1] [-2 0 2]]) [:skip #{"prediction1" "prediction2" "prediction3"}])
          result (lstm-param-delta sample-w-network
                                   {:block-delta       (float-array (take 10 (repeat 1)))
                                    :input-gate-delta  (float-array (take 10 (repeat 1)))
                                    :forget-gate-delta (float-array (take 10 (repeat 1)))
                                    :output-gate-delta (float-array (take 10 (repeat 1)))}
                                   (float-array [2 1 -1])
                                   (:hidden (:activation (first it)))
                                   (:hidden (:state      (first it))))]
      (is (= (vec (:block-w-delta result))  (take 30 (flatten (repeat (map float [2.0 1.0 -1.0]))))))
      (is (= (vec (:block-wr-delta result)) (take 100 (repeat (float -0.0629378)))))
      (is (= (vec (:input-gate-w-delta result)) (take 30 (flatten (repeat (map float [2.0 1.0 -1.0]))))))
      (is (= (vec (:input-gate-wr-delta result))  (take 100 (repeat (float -0.0629378)))))
      (is (= (vec (:forget-gate-w-delta result)) (take 30 (flatten (repeat (map float [2.0 1.0 -1.0]))))))
      (is (= (vec (:forget-gate-wr-delta result)) (take 100 (repeat (float -0.0629378)))))
      (is (= (vec (:output-gate-w-delta result))  (take 30 (flatten (repeat (map float [2.0 1.0 -1.0]))))))
      (is (= (vec (:output-gate-wr-delta result)) (take 100 (repeat (float -0.0629378)))))
      (is (= (vec (:block-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:input-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:forget-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:output-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:peephole-input-gate-delta  result)) (take 10 (repeat (float -0.20586833)))))
      (is (= (vec (:peephole-forget-gate-delta result)) (take 10 (repeat (float -0.20586833)))))
      (is (= (vec (:peephole-output-gate-delta result)) (take 10 (repeat (float -0.20586833)))))))

  (testing "lstm-param-delta in sparse model"
    (let [it (sequential-output sample-w-network-sparse [{"language" 1} {"processing" 1}] [:skip #{"prediction1" "prediction2" "prediction3"}])
          result (lstm-param-delta sample-w-network-sparse
                                   {:block-delta       (float-array (take 10 (repeat 1)))
                                    :input-gate-delta  (float-array (take 10 (repeat 1)))
                                    :forget-gate-delta (float-array (take 10 (repeat 1)))
                                    :output-gate-delta (float-array (take 10 (repeat 1)))}
                                   {"processing" 1}
                                   (:hidden (:activation (first it)))
                                   (:hidden (:state      (first it))))
          it2 (sequential-output sample-w-network [(float-array [0 1 0]) (float-array [0 0 1])] [:skip #{"prediction1" "prediction2" "prediction3"}])
          result2 (lstm-param-delta sample-w-network
                                    {:block-delta       (float-array (take 10 (repeat 1)))
                                     :input-gate-delta  (float-array (take 10 (repeat 1)))
                                     :forget-gate-delta (float-array (take 10 (repeat 1)))
                                     :output-gate-delta (float-array (take 10 (repeat 1)))}
                                    (float-array [0 0 1])
                                    (:hidden (:activation (first it2)))
                                    (:hidden (:state      (first it2))))
          ws (get (:sparses-delta result) "processing")]
      (is (= (vec (:block-w-delta ws))
             (->> (take-nth 3 (drop 2 (vec (:block-w-delta result2)))))))
      (is (= (vec (:input-gate-w-delta ws))
             (->> (take-nth 3 (drop 2 (vec (:input-gate-w-delta result2)))))))
      (is (= (vec (:forget-gate-w-delta ws))
             (->> (take-nth 3 (drop 2 (vec (:forget-gate-w-delta result2)))))))
      (is (= (vec (:output-gate-w-delta ws))
             (->> (take-nth 3 (drop 2 (vec (:output-gate-w-delta result2)))))))))


  (testing "lstm-delta-zeros"
    (let [result (lstm-delta-zeros (:unit-num (:hidden sample-w-network)))]
      (is (= (count (:block-delta result))  10))
      (is (= (count (:input-gate-delta result)) 10))
      (is (= (count (:forget-gate-delta result)) 10))
      (is (= (count (:output-gate-delta result)) 10))))

  (testing "bptt with dense binary classification"
    (let [result (bptt sample-w-network
                       [(float-array [0 1 0]) (float-array [2 0 0])]
                       [{:pos ["prediction1"] :neg ["prediction3"]} {:pos ["prediction2"] :neg ["prediction3"]}])
          hd (:hidden-delta result)
          od (:output-delta result)]
      (is (= (count (:block-w-delta                 hd)) 30))
      (is (= (count (remove zero? (:block-w-delta   hd))) 20))
      (is (= (count (remove zero? (:block-wr-delta  hd))) 100))
      (is (= (count (:input-gate-w-delta            hd)) 30))
      (is (= (count (remove zero? (:input-gate-w-delta   hd))) 20))
      (is (= (count (remove zero? (:input-gate-wr-delta  hd))) 100))
      (is (= (count (:forget-gate-w-delta           hd)) 30))
      (is (= (count (remove zero? (:forget-gate-w-delta   hd))) 0))
      (is (= (count (:forget-gate-wr-delta          hd)) 100))
      (is (= (count (remove zero? (:forget-gate-wr-delta  hd))) 0))
      (is (= (count (:output-gate-w-delta           hd)) 30))
      (is (= (count (remove zero? (:output-gate-w-delta   hd))) 20))
      (is (= (count (remove zero? (:output-gate-wr-delta  hd))) 100))


      (is (= (count (:block-bias-delta           hd)) 10))
      (is (= (count (:input-gate-bias-delta      hd)) 10))
      (is (= (count (:forget-gate-bias-delta     hd)) 10))
      (is (= (count (:output-gate-bias-delta     hd)) 10))
      (is (= (count (:peephole-input-gate-delta  hd)) 10))
      (is (= (count (:peephole-forget-gate-delta hd)) 10))
      (is (= (count (:peephole-output-gate-delta hd)) 10))
      ;FIXME add output-delta
      ))


  (testing "bptt with sparse model"
    (let [result (bptt sample-w-network-sparse
                       [{"language" (float 1)} {"processing" (float 1)}]
                       [:skip {:pos ["prediction1"] :neg ["prediction3"]}])
          hd (:hidden-delta result)
          od (:output-delta result)
          it (:sparses-delta hd)]
      (is (= (count (:block-w-delta (get it "natural"))) 0))
      (is (= (count (:block-w-delta (get it "language"))) 10))
      (is (= (count (:block-w-delta (get it "processing"))) 10))))

  (testing "bptt with sparse prediction"
    (let [result (bptt sample-w-network-prediction
                       [{"natural" (float 1)} {"processing" (float 1)}]
                       [:skip {"prediction" 20}])
          hd (:hidden-delta result)
          o  (:output-delta result)
          it (:sparses-delta hd)]
      (is (= (count (:block-w-delta (get it "natural"))) 10))
      (is (= (count (:block-w-delta (get it "language"))) 0))
      (is (= (count (:block-w-delta (get it "processing"))) 10))))


  (testing "update-model! with dense model"
    (let [result (update-model! sample-w-network
                                (bptt sample-w-network
                                      [(float-array [0 1 0]) (float-array [2 0 0])]
                                      [{:pos ["prediction1"] :neg ["prediction3"]} {:pos ["prediction2"] :neg ["prediction3"]}])
                                0.1)
          hd (:hidden result)]
      (is (= (count (:block-w hd)) 30))
      (is (= (count (:block-wr hd)) 100))
      (is (= (count (:input-gate-w hd)) 30))
      (is (= (count (:input-gate-wr hd)) 100))
      (is (= (count (:forget-gate-w hd)) 30))
      (is (= (count (:forget-gate-wr hd)) 100))
      (is (= (count (:output-gate-w hd)) 30))
      (is (= (count (:output-gate-wr hd)) 100))

      (is (= (vec (:block-w hd))
             (flatten (take 10 (repeat (map float [0.10043954 0.10024438 0.1]))))))
      (is (= (vec (:block-wr hd))
             (take 100 (repeat (float 0.09998704)))))
      (is (= (vec (:input-gate-w hd))
             (flatten (take 10 (repeat (map float [0.09958622 0.09974441 0.1]))))))
      (is (= (vec (:input-gate-wr hd))
             (take 100 (repeat (float 0.100012206)))))
      (is (= (vec (:forget-gate-w hd))
             (flatten (take 30 (repeat (map float [0.1]))))))
      (is (= (vec (:forget-gate-wr hd))
             (take 100 (repeat (float 0.1)))))
      (is (= (vec (:output-gate-w hd))
             (flatten (take 10 (repeat (map float [0.099447146 0.0998076 0.1]))))))
      (is (= (vec (:output-gate-wr hd))
             (take 100 (repeat (float 0.10001631)))))
      ;peephole
      (is (= (vec (:input-gate-peephole hd))
             (take 10 (repeat (float -0.09995717)))))
      (is (= (vec (:forget-gate-peephole hd))
             (take 10 (repeat (float -0.1)))))
      (is (= (vec (:output-gate-peephole hd))
             (take 10 (repeat (float -0.099942766)))))

      (is (= (vec (:input-gate-bias  hd)) (take 10 (repeat (float -1.0004625)))))
      (is (= (vec (:forget-gate-bias hd)) (take 10 (repeat (float -1)))))
      (is (= (vec (:output-gate-bias hd)) (take 10 (repeat (float -1.0004689)))))))

  (testing "update-model! with sparse model"
    (let [result (update-model! sample-w-network-sparse
                                (bptt sample-w-network-sparse
                                      [{"language" (float 1)} {"processing" (float 1)}]
                                      [:skip {:pos ["prediction1"] :neg ["prediction3"]}])
                                0.1)
          hd (:hidden result)
          sparses (:sparses hd)]
      (is (= (count (:block-w (get sparses "language"))) 10))
      (is (= (count (:input-gate-w (get sparses "language"))) 10))
      (is (= (count (:forget-gate-w (get sparses "language"))) 10))
      (is (= (count (:output-gate-w (get sparses "language"))) 10))
      (is (= (count (:block-wr hd)) 100))
      (is (= (count (:input-gate-wr hd)) 100))
      (is (= (count (:forget-gate-wr hd)) 100))
      (is (= (count (:output-gate-wr hd)) 100))))

  (testing "init-model with dense input"
    (let [m (init-model nil #{"A" "B" "C"} :dense 3 10 :binary-classification)
          h (:hidden m)]
      (is (= [3 10 3] (:unit-nums m)))
      (is (not= :sparse (:input-type m)))
      (is (= 30  (count (remove zero? (:block-w h)))))
      (is (= 100 (count (remove zero? (:block-wr h)))))
      (is (= 30  (count (remove zero? (:input-gate-w h)))))
      (is (= 100 (count (remove zero? (:input-gate-wr h)))))
      (is (= 30  (count (remove zero? (:forget-gate-w h)))))
      (is (= 100 (count (remove zero? (:forget-gate-wr h)))))
      (is (= 30  (count (remove zero? (:output-gate-w h)))))
      (is (= 100 (count (remove zero? (:output-gate-wr h)))))
      (is (= 10  (count (remove zero? (:block-bias h)))))
      (is (= 10  (count (remove zero? (:input-gate-bias h)))))
      (is (= 10  (count (remove zero? (:forget-gate-bias h)))))
      (is (= 10  (count (remove zero? (:output-gate-bias h)))))
      (is (= 10  (count (remove zero? (:input-gate-peephole h)))))
      (is (= 10  (count (remove zero? (:forget-gate-peephole h)))))
      (is (= 10  (count (remove zero? (:output-gate-peephole h)))))
      (let [{:strs [A B C]} (:w (:output m))]
        (is (= 10 (count A) (count B) (count C))))
      (let [{:strs [A B C]} (:bias (:output m))]
        (is (= 1 (count A) (count B) (count C))))))
  (testing "init-model with sparse input"
    (let [m (init-model #{"X" "Y" "Z"} #{"A" "B" "C"} :sparse 3 10 :binary-classification)
          h (:hidden m)]
      (is (= [3 10 3] (:unit-nums m)))
      (is (= :sparse (:input-type m)))
      (is (= 10 (count (remove zero? (:block-w (get (:sparses h) "X"))))))
      (is (= 100 (count (remove zero? (:block-wr h)))))
      (is (= 10 (count (remove zero? (:input-gate-w (get (:sparses h) "Y"))))))
      (is (= 100 (count (remove zero? (:input-gate-wr h)))))
      (is (= 10 (count (remove zero? (:forget-gate-w (get (:sparses h) "Z"))))))
      (is (= 100 (count (remove zero? (:forget-gate-wr h)))))
      (is (= 10 (count (remove zero? (:output-gate-w (get (:sparses h) "X"))))))
      (is (= 100 (count (remove zero? (:output-gate-wr h)))))
      (is (= 10 (count (remove zero? (:block-bias h)))))
      (is (= 10 (count (remove zero? (:input-gate-bias h)))))
      (is (= 10 (count (remove zero? (:forget-gate-bias h)))))
      (is (= 10 (count (remove zero? (:output-gate-bias h)))))
      (is (= 10 (count (remove zero? (:input-gate-peephole h)))))
      (is (= 10 (count (remove zero? (:forget-gate-peephole h)))))
      (is (= 10 (count (remove zero? (:output-gate-peephole h)))))
      (let [{:strs [A B C]} (:w (:output m))]
        (is (= 10 (count A) (count B) (count C))))
      (let [{:strs [A B C]} (:bias (:output m))]
        (is (= 1 (count A) (count B) (count C)))))))

