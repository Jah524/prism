(ns nn.rnn.lstm-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [clojure.core.matrix :refer [set-current-implementation mget array matrix ecount row-count]]
    [prism.nn.feedforward :as ff]
    [prism.nn.rnn.lstm :refer :all]))

(def sample-w-network
  {:input-size 3
   :hidden-size 10
   :output-type :binary-classification
   :hidden {:unit-type :lstm
            :layer-type :hidden
            :block-w (matrix (partition 3 (take 30 (repeat 0.1))))
            :block-wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :block-bias (array (take 10 (repeat -1)))
            :input-gate-w  (matrix (partition 3 (take 30 (repeat 0.1))))
            :input-gate-wr  (matrix (partition 10 (take 100 (repeat 0.1))))
            :input-gate-bias (array (take 10 (repeat -1)))
            :forget-gate-w (matrix (partition 3 (take 30 (repeat 0.1))))
            :forget-gate-wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :forget-gate-bias (array (take 10 (repeat -1)))
            :output-gate-w (matrix (partition 3 (take 30 (repeat 0.1))))
            :output-gate-wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :output-gate-bias (array (take 10 (repeat -1)))
            :peephole #{:input-gate :forget-gate :output-gate}
            :input-gate-peephole  (array (take 10 (repeat -0.1)))
            :forget-gate-peephole (array (take 10 (repeat -0.1)))
            :output-gate-peephole (array (take 10 (repeat -0.1)))}
   :output {"prediction1" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}
            "prediction2" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}
            "prediction3" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}}})

(def sample-w-network-sparse
  {:output-type :binary-classification
   :hidden-size 10
   :hidden {:sparses {"natural" {:block-w (array (take 10 (repeat 0.1)))
                                 :input-gate-w  (array (take 10 (repeat 0.1)))
                                 :forget-gate-w (array (take 10 (repeat 0.1)))
                                 :output-gate-w (array (take 10 (repeat 0.1)))}
                      "language" {:block-w (array (take 10 (repeat 0.1)))
                                  :input-gate-w  (array (take 10 (repeat 0.1)))
                                  :forget-gate-w (array (take 10 (repeat 0.1)))
                                  :output-gate-w (array (take 10 (repeat 0.1)))}
                      "processing" {:block-w (array (take 10 (repeat 0.1)))
                                    :input-gate-w  (array (take 10 (repeat 0.1)))
                                    :forget-gate-w (array (take 10 (repeat 0.1)))
                                    :output-gate-w (array (take 10 (repeat 0.1)))}}
            :block-wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :block-bias (array (take 10 (repeat -1)))
            :input-gate-wr  (matrix (partition 10 (take 100 (repeat 0.1))))
            :input-gate-bias (array (take 10 (repeat -1)))
            :forget-gate-wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :forget-gate-bias (array (take 10 (repeat -1)))
            :output-gate-wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :output-gate-bias (array (take 10 (repeat -1)))
            :input-gate-peephole  (array (take 10 (repeat -0.1)))
            :forget-gate-peephole (array (take 10 (repeat -0.1)))
            :output-gate-peephole (array (take 10 (repeat -0.1)))
            :peephole #{:input-gate :forget-gate :output-gate}}
   :output {"prediction1" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}
            "prediction2" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}
            "prediction3" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}}})

(def sample-w-network-prediction
  {:output-type :prediction
   :hidden-size 10
   :hidden {:sparses {"natural" {:block-w (array (take 10 (repeat 0.1)))
                                 :input-gate-w  (array (take 10 (repeat 0.1)))
                                 :forget-gate-w (array (take 10 (repeat 0.1)))
                                 :output-gate-w (array (take 10 (repeat 0.1)))}
                      "language" {:block-w (array (take 10 (repeat 0.1)))
                                  :input-gate-w  (array (take 10 (repeat 0.1)))
                                  :forget-gate-w (array (take 10 (repeat 0.1)))
                                  :output-gate-w (array (take 10 (repeat 0.1)))}
                      "processing" {:block-w (array (take 10 (repeat 0.1)))
                                    :input-gate-w  (array (take 10 (repeat 0.1)))
                                    :forget-gate-w (array (take 10 (repeat 0.1)))
                                    :output-gate-w (array (take 10 (repeat 0.1)))}}
            :block-wr (matrix (partition 10 (take 100 (take 100 (repeat 0.1)))))
            :block-bias (array (take 10 (repeat -1)))
            :input-gate-wr  (matrix (partition 10 (take 100 (take 100 (repeat 0.1)))))
            :input-gate-bias (array (take 10 (repeat -1)))
            :forget-gate-wr (matrix (partition 10 (take 100 (take 100 (repeat 0.1)))))
            :forget-gate-bias (array (take 10 (repeat -1)))
            :output-gate-wr (matrix (partition 10 (take 100 (take 100 (repeat 0.1)))))
            :output-gate-bias (array (take 10 (repeat -1)))
            :input-gate-peephole  (array (take 10 (repeat -0.1)))
            :forget-gate-peephole (array (take 10 (repeat -0.1)))
            :output-gate-peephole (array (take 10 (repeat -0.1)))
            :peephole #{:input-gate :forget-gate :output-gate}}
   :output {"prediction" {:w (array (take 10 (repeat 0.1)))
                          :bias  (array [-1])}}})

(deftest lstm-test
  (testing "partial-state-sparse with set"
    (let [{:keys [hidden]} sample-w-network-sparse]
      (is (= (->> (partial-state-sparse #{"language"} (:sparses hidden)) (map vec))
             (take 4 (repeat (take 10 (repeat (double 0.1)))))))))
  (testing "partial-state-sparse with map"
    (let [{:keys [hidden]} sample-w-network-sparse]
      (is (= (->> (partial-state-sparse {"language" 1} (:sparses hidden)) (map vec))
             (->> (partial-state-sparse #{"language"} (:sparses hidden)) (map vec))
             (take 4 (repeat (take 10 (repeat (double 0.1)))))))
      (is (not= (->> (partial-state-sparse {"language" 2} (:sparses hidden)) (map vec))
                (->> (partial-state-sparse #{"language"} (:sparses hidden)) (map vec))))))
  (testing "lstm-activation"
    (let [result (lstm-activation sample-w-network
                                  (float-array (take 3 (repeat 2)))
                                  (float-array (take 10 (repeat 2)))
                                  (float-array (take 10 (repeat 2))))
          a (:activation result)
          s (:state result)]
      (is (= (mapv float a) (take 10 (repeat (float 0.787542)))))
      (is (= (mapv float (:lstm s)) (take 10 (repeat (float 0.787542)))))
      (is (= (mapv float (:block s)) (take 10 (repeat (float 1.6)))))
      (is (= (mapv float (:input-gate  s)) (take 10 (repeat (float 1.4)))))
      (is (= (mapv float (:forget-gate s)) (take 10 (repeat (float 1.4)))))
      (is (= (mapv float (:output-gate s)) (take 10 (repeat (float 1.4)))))
      (is (= (mapv float (:cell-state  s)) (take 10 (repeat (float 2.3437154)))))))

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
             (take 10 (repeat (double 0.7082130046205605)))))
      (is (= (vec (:lstm s))
             (vec a2)
             (take 10 (repeat (double 0.7082130046205605)))))
      (is (= (vec (:block s))
             (vec (:block s2))
             (take 10 (repeat (double 1.1999999999999997)))))
      (is (= (mapv float (:input-gate  s))
             (mapv float (:input-gate  s2))
             (take 10 (repeat (float 1)))))
      (is (= (mapv float (:forget-gate s))
             (mapv float (:forget-gate s2))
             (take 10 (repeat (float 1)))))
      (is (= (mapv float (:output-gate s))
             (mapv float (:output-gate s2))
             (take 10 (repeat (float 1)))))
      (is (= (mapv float (:cell-state  s))
             (mapv float (:cell-state  s2))
             (take 10 (repeat (float 2.0715675)))))))

  (testing "lstm-model-output with dense input"
    (let [result (lstm-model-output sample-w-network
                                    (float-array [1 0 -10]); as x-input
                                    #{"prediction1" "prediction2" "prediction3"}
                                    (float-array (take 10 (repeat 0)))
                                    (float-array (take (:hidden-size sample-w-network) (repeat 0) )))
          f  (get-in result [:state :input])
          ss (get-in result [:state :hidden])]
      (is (= (vec f) (map float [1 0 -10])))
      (is (= (mapv float (:lstm ss)) (take 10 (repeat (float -0.016104385)))))
      (is (= (mapv float (:block ss)) (take 10 (repeat (float -1.9)))))
      (is (= (mapv float (:input-gate  ss)) (take 10 (repeat (float -1.9)))))
      (is (= (mapv float (:forget-gate ss)) (take 10 (repeat (float -1.9)))))
      (is (= (mapv float (:output-gate  ss)) (take 10 (repeat (float -1.9)))))
      (is (= (mapv float (:cell-state ss)) (take 10 (repeat (float -0.1244146)))))
      (is (= (mapv float (:hidden (:activation result)))
             (take 10 (repeat (float -0.016104385)))))
      (is (= (->> result :activation :hidden (map float))
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
      (is (= (mapv float (:lstm ss))
             (mapv float (:lstm ss2))
             (take 10 (repeat (float -0.05900606274789055)))))
      (is (= (mapv float (:input-gate  ss))
             (mapv float (:input-gate ss2))
             (take 10 (repeat (float -0.8999999985098839)))))
      (is (= (mapv float (:forget-gate ss))
             (mapv float (:forget-gate ss2))
             (take 10 (repeat (float -0.8999999985098839)))))
      (is (= (mapv float (:output-gate  ss))
             (mapv float (:output-gate ss2))
             (take 10 (repeat (float -0.8999999985098839)))))
      (is (= (mapv float (:cell-state ss))
             (mapv float (:cell-state ss2))
             (take 10 (repeat (float -0.20704626)))))))

  (testing "forward"
    (let [result (forward sample-w-network
                          (mapv #(array %) [[0 1 0] [0 1 0] [0 2 0]])
                          [:skip :skip #{"prediction1" "prediction2" "prediction3"}])]
      (is (= 3 (count result))))
    (let [result1 (forward sample-w-network (map array [[1 0 0] [1 0 0]]) [:skip #{"prediction1" "prediction2" "prediction3"}])
          result2 (forward sample-w-network (map array [[2 0 0] [1 0 0]]) [:skip #{"prediction1" "prediction2" "prediction3"}])
          it1 (vec (:output (:activation (last result1))))
          it2 (vec (:output (:activation (last result2))))]
      (is (not= it1 it2))
      (is (= (->> it1 (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.25474384) "prediction1" (float 0.25474384) "prediction3" (float 0.25474384)}))
      (is (= (->> it2 (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.25481504) "prediction1" (float 0.25481504) "prediction3" (float 0.25481504)}))))

  (testing "forward in sparse model"
    (let [result1 (forward sample-w-network-sparse
                           [{"language" (float 1)} {"processing" (float 1)}]
                           [:skip #{"prediction1" "prediction2" "prediction3"}])
          result2 (forward sample-w-network
                           [(float-array [0 1 0]) (float-array [0 0 1])]
                           [:skip #{"prediction1" "prediction2" "prediction3"}])
          out1 (mapv float (:hidden (:activation (first result1))))
          out2 (mapv float (:hidden (:activation (first result2))))
          out3 (mapv float (:hidden (:activation (second result1))))
          out4 (mapv float (:hidden (:activation (second result2))))]
      (is (= out1 out2 (take 10 (repeat (float -0.05900606)))))
      (is (= out3 out4 (take 10 (repeat (float -0.07346944)))))))
  (testing "context"
    (let [result (context sample-w-network (map array [[1 0 0] [1 0 0]]))
          {:keys [hidden input]} (last result)
          {:keys [block input-gate forget-gate output-gate]} (:state hidden)]
      (is (= (vec input) (map float [1 0 0])))
      (is (= 2 (count result)))
      (is (= (mapv float block)       (take 10 (repeat (float -0.9590061)))))
      (is (= (mapv float input-gate)  (take 10 (repeat (float -0.93830144)))))
      (is (= (mapv float forget-gate) (take 10 (repeat (float -0.93830144)))))
      (is (= (mapv float output-gate) (take 10 (repeat (float -0.93830144)))))))

  ;; Back Propagation Through Time ;;


  (testing "output-param-delta"
    (let [result (->> (ff/output-param-delta {"A" 0.5 "B" 0 "C" -0.5} 10 (float-array (range 10)))
                      (reduce (fn [acc [item {:keys [w-delta bias-delta]}]]
                                (assoc acc item {:w-delta (mapv float w-delta) :bias-delta (map float bias-delta)}))
                              {}))
          {:strs [A B C]} result]
      (is (= A {:w-delta (map float [0.0 0.5 1.0 1.5 2.0 2.5 3.0 3.5 4.0 4.5]) :bias-delta [(float 0.5)]}))
      (is (= B {:w-delta (take 10 (repeat (float 0))) :bias-delta [(float 0)]}))
      (is (= C {:w-delta (map float [-0.0 -0.5 -1.0 -1.5 -2.0 -2.5 -3.0 -3.5 -4.0 -4.5]) :bias-delta [(float -0.5)]}))))

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
      (is (= (mapv float (:output-gate-delta result)) [(float 1.8953931)]))
      (is (= (mapv float (:cell-state-delta result)) [(float 19.56018912189448)]))
      (is (= (mapv float (:block-delta result)) [(float 4.361677)]))
      (is (= (mapv float (:forget-gate-delta result)) [(float 3.8457663)]))
      (is (= (mapv float (:input-gate-delta result)) [(float 3.2060409)]))))

  (testing "param-delta-sparse with set"
    (let [{:keys [block-w-delta input-gate-w-delta forget-gate-w-delta output-gate-w-delta]}
          (-> (param-delta-sparse #{"processing"}
                                  (float-array (take 10 (repeat 1)))
                                  (float-array (take 10 (repeat 1)))
                                  (float-array (take 10 (repeat 1)))
                                  (float-array (take 10 (repeat 1))))
              (get "processing"))]
      (is (= (vec block-w-delta)       (take 10 (repeat (float 1)))))
      (is (= (vec input-gate-w-delta)  (take 10 (repeat (float 1)))))
      (is (= (vec forget-gate-w-delta) (take 10 (repeat (float 1)))))
      (is (= (vec output-gate-w-delta) (take 10 (repeat (float 1)))))))


  (testing "lstm-param-delta"
    (let [it (forward sample-w-network (map float-array [[2 1 -1] [-2 0 2]]) [:skip #{"prediction1" "prediction2" "prediction3"}])
          result (lstm-param-delta sample-w-network
                                   {:block-delta       (float-array (take 10 (repeat 1)))
                                    :input-gate-delta  (float-array (take 10 (repeat 1)))
                                    :forget-gate-delta (float-array (take 10 (repeat 1)))
                                    :output-gate-delta (float-array (take 10 (repeat 1)))}
                                   (float-array [2 1 -1])
                                   (:hidden (:activation (first it)))
                                   (:hidden (:state      (first it))))]
      (is (= (map vec (:block-w-delta result))  (partition 3 (take 30 (flatten (repeat (map float [2.0 1.0 -1.0])))))))
      (is (= (map #(mapv float %) (:block-wr-delta result)) (partition 10 (take 100 (repeat (float -0.0629378))))))
      (is (= (map #(mapv float %) (:input-gate-w-delta result)) (partition 3 (take 30 (flatten (repeat (map float [2.0 1.0 -1.0])))))))
      (is (= (map #(mapv float %) (:input-gate-wr-delta result)) (partition 10 (take 100 (repeat (float -0.0629378))))))
      (is (= (map #(mapv float %) (:forget-gate-w-delta result)) (partition 3 (take 30 (flatten (repeat (map float [2.0 1.0 -1.0])))))))
      (is (= (map #(mapv float %) (:forget-gate-wr-delta result)) (partition 10 (take 100 (repeat (float -0.0629378))))))
      (is (= (map #(mapv float %) (:output-gate-w-delta result))  (partition 3 (take 30 (flatten (repeat (map float [2.0 1.0 -1.0])))))))
      (is (= (map #(mapv float %) (:output-gate-wr-delta result)) (partition 10 (take 100 (repeat (float -0.0629378))))))
      (is (= (vec (:block-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:input-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:forget-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:output-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (mapv float (:peephole-input-gate-delta  result)) (take 10 (repeat (float -0.20586835)))))
      (is (= (mapv float (:peephole-forget-gate-delta result)) (take 10 (repeat (float -0.20586835)))))
      (is (= (mapv float (:peephole-output-gate-delta result)) (take 10 (repeat (float -0.20586835)))))))

  (testing "lstm-param-delta in sparse model"
    (let [it (forward sample-w-network-sparse [{"language" 1} {"processing" 1}] [:skip #{"prediction1" "prediction2" "prediction3"}])
          result (lstm-param-delta sample-w-network-sparse
                                   {:block-delta       (float-array (take 10 (repeat 1)))
                                    :input-gate-delta  (float-array (take 10 (repeat 1)))
                                    :forget-gate-delta (float-array (take 10 (repeat 1)))
                                    :output-gate-delta (float-array (take 10 (repeat 1)))}
                                   {"processing" 1}
                                   (:hidden (:activation (first it)))
                                   (:hidden (:state      (first it))))
          it2 (forward sample-w-network [(float-array [0 1 0]) (float-array [0 0 1])] [:skip #{"prediction1" "prediction2" "prediction3"}])
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
             (->> (vec (map #(nth % 2) (:block-w-delta result2))))))
      (is (= (vec (:input-gate-w-delta ws))
             (->> (vec (map #(nth % 2) (:input-gate-w-delta result2))))))
      (is (= (vec (:forget-gate-w-delta ws))
             (->> (vec (map #(nth % 2) (:forget-gate-w-delta result2))))))
      (is (= (vec (:output-gate-w-delta ws))
             (->> (vec (map #(nth % 2) (:output-gate-w-delta result2))))))))


  (testing "lstm-delta-zeros"
    (let [result (lstm-delta-zeros (:hidden-size sample-w-network))]
      (is (= (ecount (:block-delta result))  10))
      (is (= (ecount (:input-gate-delta result)) 10))
      (is (= (ecount (:forget-gate-delta result)) 10))
      (is (= (ecount (:output-gate-delta result)) 10))))

  (testing "bptt with dense binary classification"
    (let [{:keys [param-loss loss]} (bptt sample-w-network
                                          (forward sample-w-network
                                                   [(float-array [0 1 0]) (float-array [2 0 0])]
                                                   [["prediction1" "prediction3"] ["prediction2" "prediction3"]])
                                          [{:pos ["prediction1"] :neg ["prediction3"]} {:pos ["prediction2"] :neg ["prediction3"]}])
          {hd :hidden-delta od :output-delta} param-loss]
      (is (= (count loss) 2))
      (is (= loss [{"prediction1" (double 0.7425005567875662) "prediction3" (double -0.25749944321243373)}
                   {"prediction2" (double 0.746550968296988)  "prediction3" (double -0.25344903170301203)}]))
      (is (= (row-count (:block-w-delta                 hd)) 10))
      (is (= (row-count (first (:block-w-delta               hd))) 3))
      (is (= (row-count (:block-wr-delta  hd)) 10))
      (is (= (row-count (first (:block-wr-delta  hd))) 10))
      (is (= (row-count (:input-gate-w-delta            hd)) 10))
      (is (= (row-count (first (:input-gate-w-delta          hd))) 3))
      (is (= (row-count (:input-gate-wr-delta  hd)) 10))
      (is (= (row-count (first (:input-gate-wr-delta  hd))) 10))
      (is (= (row-count (:forget-gate-w-delta           hd)) 10))
      (is (= (row-count (first (:forget-gate-w-delta           hd))) 3))
      (is (= (row-count (:forget-gate-wr-delta          hd)) 10))
      (is (= (row-count (first (:forget-gate-wr-delta          hd))) 10))
      (is (= (row-count (:output-gate-w-delta           hd)) 10))
      (is (= (row-count (first (:output-gate-w-delta     hd))) 3))
      (is (= (row-count (:output-gate-wr-delta  hd)) 10))
      (is (= (row-count (first (:output-gate-wr-delta  hd))) 10))


      (is (= (row-count (:block-bias-delta           hd)) 10))
      (is (= (row-count (:input-gate-bias-delta      hd)) 10))
      (is (= (row-count (:forget-gate-bias-delta     hd)) 10))
      (is (= (row-count (:output-gate-bias-delta     hd)) 10))
      (is (= (row-count (:peephole-input-gate-delta  hd)) 10))
      (is (= (row-count (:peephole-forget-gate-delta hd)) 10))
      (is (= (row-count (:peephole-output-gate-delta hd)) 10))
      ;FIXME add output-delta
      ))


  (testing "bptt with sparse model"
    (let [{:keys [param-loss loss]} (bptt sample-w-network-sparse
                                          (forward sample-w-network-sparse
                                                   [{"language" (float 1)} {"processing" (float 1)}]
                                                   [:skip ["prediction1" "prediction3"]])
                                          [:skip {:pos ["prediction1"] :neg ["prediction3"]}])
          {hd :hidden-delta od :output-delta} param-loss
          it (:sparses-delta hd)]
      (is (= (count loss) 2))
      (is (= loss [{} {"prediction1" (double 0.7452561474072019) "prediction3" (double -0.25474385259279814)}]))
      (is (= (nil? (:block-w-delta (get it "natural")))))
      (is (= (row-count (:block-w-delta (get it "language"))) 10))
      (is (= (row-count (:block-w-delta (get it "processing"))) 10))))

  (testing "bptt with sparse prediction"
    (let [{:keys [param-loss loss]} (bptt sample-w-network-prediction
                                          (forward sample-w-network-prediction
                                                   [{"natural" (float 1)} {"processing" (float 1)}]
                                                   [:skip ["prediction"]])
                                          [:skip {"prediction" 20}])
          {hd :hidden-delta o :output-delta} param-loss
          it (:sparses-delta hd)]
      (is (= (count loss) 2))
      (is (= loss [{} {"prediction" (double 21.07346944063493)}]))
      (is (= (row-count (:block-w-delta (get it "natural"))) 10))
      (is (= (nil? (:block-w-delta (get it "language")))))
      (is (= (row-count (:block-w-delta (get it "processing"))) 10))))


  (testing "update-model! with dense model"
    (let [result (update-model! sample-w-network
                                (:param-loss (bptt sample-w-network
                                                   (forward sample-w-network
                                                            [(float-array [0 1 0]) (float-array [2 0 0])]
                                                            [["prediction1" "prediction3"] ["prediction2" "prediction3"]])
                                                   [{:pos ["prediction1"] :neg ["prediction3"]} {:pos ["prediction2"] :neg ["prediction3"]}]))
                                0.1)
          hd (:hidden result)]
      (is (= (row-count (:block-w hd)) 10))
      (is (= (row-count (:block-wr hd)) 10))
      (is (= (row-count (:input-gate-w hd)) 10))
      (is (= (row-count (:input-gate-wr hd)) 10))
      (is (= (row-count (:forget-gate-w hd)) 10))
      (is (= (row-count (:forget-gate-wr hd)) 10))
      (is (= (row-count (:output-gate-w hd)) 10))
      (is (= (row-count (:output-gate-wr hd)) 10))

      (is (= (map #(map float %) (:block-w hd))
             (take 10 (repeat (map float [0.10043953 0.10024281 0.1])))))
      (is (= (map #(map float %) (:block-wr hd))
             (partition 10 (take 100 (repeat (float 0.09998703))))))
      (is (= (map #(map float %) (:input-gate-w hd))
             (take 10 (repeat (map float [0.09958622 0.09974605 0.1])))))
      (is (= (map #(map float %) (:input-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.100012206))))))
      (is (= (map #(map float %) (:forget-gate-w hd))
             (take 10 (repeat (map float [0.099876866 0.1 0.1])))))
      (is (= (map #(map float %) (:forget-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.10000363))))))
      (is (= (map #(map float %) (:output-gate-w hd))
             (take 10 (repeat (map float [0.099447146 0.099810176 0.1])))))
      (is (= (map #(map float %) (:output-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.10001631))))))
      ;peephole
      (is (= (mapv float (:input-gate-peephole hd))
             (take 10 (repeat (float -0.09995716)))))
      (is (= (mapv float (:forget-gate-peephole hd))
             (take 10 (repeat (float -0.09998725)))))
      (is (= (mapv float (:output-gate-peephole hd))
             (take 10 (repeat (float -0.099942766)))))

      (is (= (mapv float (:input-gate-bias  hd)) (take 10 (repeat (float -1.0004609)))))
      (is (= (mapv float (:forget-gate-bias hd)) (take 10 (repeat (float -1.0000615)))))
      (is (= (mapv float (:output-gate-bias hd)) (take 10 (repeat (float -1.0004662)))))))

  (testing "update-model! with sparse model"
    (let [result (update-model! sample-w-network-sparse
                                (:param-loss (bptt sample-w-network-sparse
                                                   (forward sample-w-network-sparse
                                                            [{"language" (float 1)} {"processing" (float 1)}]
                                                            [:skip ["prediction1" "prediction3"]])
                                                   [:skip {:pos ["prediction1"] :neg ["prediction3"]}]))
                                0.1)
          hd (:hidden result)
          sparses (:sparses hd)]
      (is (= (row-count (:block-w (get sparses "language"))) 10))
      (is (= (row-count (:input-gate-w (get sparses "language"))) 10))
      (is (= (row-count (:forget-gate-w (get sparses "language"))) 10))
      (is (= (row-count (:output-gate-w (get sparses "language"))) 10))
      (is (= (row-count (:block-wr hd)) 10))
      (is (= (row-count (:input-gate-wr hd)) 10))
      (is (= (row-count (:forget-gate-wr hd)) 10))
      (is (= (row-count (:output-gate-wr hd)) 10))))

  (testing "init-model with dense input"
    (let [m (init-model {:input-items  nil
                         :output-items #{"A" "B" "C"}
                         :input-type :dense
                         :input-size 3
                         :hidden-size 10
                         :output-type :binary-classification})
          h (:hidden m)]
      (is (not= :sparse (:input-type m)))
      (is (= 10 (row-count (:block-w h))))
      (is (= 10 (row-count (:block-wr h))))
      (is (= 10 (row-count (:input-gate-w h))))
      (is (= 10 (row-count (:input-gate-wr h))))
      (is (= 10 (row-count (:forget-gate-w h))))
      (is (= 10 (row-count (:forget-gate-wr h))))
      (is (= 10 (row-count (:output-gate-w h))))
      (is (= 10 (row-count (:output-gate-wr h))))
      (is (= 10 (count (remove zero? (:block-bias h)))))
      (is (= 10 (count (remove zero? (:input-gate-bias h)))))
      (is (= 10 (count (remove zero? (:forget-gate-bias h)))))
      (is (= 10 (count (remove zero? (:output-gate-bias h)))))
      (is (= 10 (count (remove zero? (:input-gate-peephole h)))))
      (is (= 10 (count (remove zero? (:forget-gate-peephole h)))))
      (is (= 10 (count (remove zero? (:output-gate-peephole h)))))
      (let [{aw :w abias :bias} (get (:output m) "A")
            {bw :w bbias :bias} (get (:output m) "B")
            {cw :w cbias :bias} (get (:output m) "C")]
        (is (= 10 (ecount aw) (ecount bw) (ecount cw)))
        (is (= 1 (ecount abias) (ecount bbias) (ecount cbias))))))
  (testing "init-model with sparse input"
    (let [m (init-model {:input-items  #{"X" "Y" "Z"}
                         :output-items #{"A" "B" "C"}
                         :input-type :sparse
                         :input-size nil
                         :hidden-size 10
                         :output-type :binary-classification})
          h (:hidden m)]
      (is (= 10 (row-count (remove zero? (:block-w (get (:sparses h) "X"))))))
      (is (= 10 (row-count  (:block-wr h))))
      (is (= 10 (row-count (remove zero? (:input-gate-w (get (:sparses h) "Y"))))))
      (is (= 10 (row-count (:input-gate-wr h))))
      (is (= 10 (row-count (remove zero? (:forget-gate-w (get (:sparses h) "Z"))))))
      (is (= 10 (row-count (:forget-gate-wr h))))
      (is (= 10 (row-count (remove zero? (:output-gate-w (get (:sparses h) "X"))))))
      (is (= 10 (row-count  (:output-gate-wr h))))
      (is (= 10 (count (remove zero? (:block-bias h)))))
      (is (= 10 (count (remove zero? (:input-gate-bias h)))))
      (is (= 10 (count (remove zero? (:forget-gate-bias h)))))
      (is (= 10 (count (remove zero? (:output-gate-bias h)))))
      (is (= 10 (count (remove zero? (:input-gate-peephole h)))))
      (is (= 10 (count (remove zero? (:forget-gate-peephole h)))))
      (is (= 10 (count (remove zero? (:output-gate-peephole h)))))
      (let [{aw :w abias :bias} (get (:output m) "A")
            {bw :w bbias :bias} (get (:output m) "B")
            {cw :w cbias :bias} (get (:output m) "C")]
        (is (= 10 (ecount aw) (ecount bw) (ecount cw)))
        (is (= 1 (ecount abias) (ecount bbias) (ecount cbias))))))

  )

