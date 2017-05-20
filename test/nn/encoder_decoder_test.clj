(ns nn.encoder-decoder-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test   :refer :all]
    [clojure.core.matrix :refer [set-current-implementation mget array matrix ecount row-count]]
    [prism.nn.lstm  :refer [lstm-activation sequential-output]]
    [nn.lstm-test   :refer [sample-w-network]]
    [prism.nn.encoder-decoder :refer :all]
    [matrix.default :refer [default-matrix-kit]]))


(def encoder-sample-network
  "assumed 3->5->3 connection"
  {:matrix-kit default-matrix-kit
   :input-type :dense
   :output-type :binary-classification
   :input-size 3
   :hidden {:unit-type :lstm
            :unit-num 5
            :block-w (matrix (partition 3 (take 15 (repeat 0.1))))
            :block-wr (matrix (partition 5 (take 25 (repeat 0.1))))
            :block-bias (array (take 5 (repeat -1)))
            :input-gate-w  (matrix (partition 3 (take 15 (repeat 0.1))))
            :input-gate-wr  (matrix (partition 5 (take 25 (repeat 0.1))))
            :input-gate-bias (array (take 5 (repeat -1)))
            :forget-gate-w (matrix (partition 3 (take 15 (repeat 0.1))))
            :forget-gate-wr (matrix (partition 5 (take 25 (repeat 0.1))))
            :forget-gate-bias (array (take 5 (repeat -1)))
            :output-gate-w (matrix (partition 3 (take 15 (repeat 0.1))))
            :output-gate-wr (matrix (partition 5 (take 25 (repeat 0.1))))
            :output-gate-bias (array (take 5 (repeat -1)))
            :peephole #{:input-gate :forget-gate :output-gate}
            :input-gate-peephole  (array (take 5 (repeat -0.1)))
            :forget-gate-peephole (array (take 5 (repeat -0.1)))
            :output-gate-peephole (array (take 5 (repeat -0.1)))}
   :output {:activation :sigmoid,
            :layer-type :output,
            :unit-num 3
            :w {"prediction1" (array (take 5 (repeat 0.1)))
                "prediction2" (array (take 5 (repeat 0.1)))
                "prediction3" (array (take 5 (repeat 0.1)))}
            :bias {"prediction1" (array [-1])
                   "prediction2" (array [-1])
                   "prediction3" (array [-1])}}})

(def decoder-sample-network
  "assumed 10 self hidden, 5 encoder connections and embedding-size is 3"
  (let [h (:hidden sample-w-network)]
    (assoc sample-w-network
      :input-size 3
      :encoder-size 5
      :hidden
      (assoc h
        :block-we (matrix (partition 5 (take 50 (repeat 0.02))))
        :input-gate-we (matrix (partition 5 (take 50 (repeat 0.02))))
        :forget-gate-we (matrix (partition 5 (take 50 (repeat 0.02))))
        :output-gate-we (matrix (partition 5 (take 50 (repeat 0.02)))))
      :output {"prediction1" {:w (array (take 10 (repeat 0.1)))
                              :bias (array [-1])
                              :encoder-w (array (take 5 (repeat 0.3)))
                              :previous-input-w (array (take 3 (repeat 0.25)))}
               "prediction2" {:w (array (take 10 (repeat 0.1)))
                              :bias (array [-1])
                              :encoder-w (array (take 5 (repeat 0.3)))
                              :previous-input-w (array (take 3 (repeat 0.25)))}
               "prediction3" {:w (array (take 10 (repeat 0.1)))
                              :bias (array [-1])
                              :encoder-w (array (take 5 (repeat 0.3)))
                              :previous-input-w (array (take 3 (repeat 0.25)))}})))

(def sample-encoder-decoder
  {:encoder encoder-sample-network
   :decoder decoder-sample-network})


(deftest encoder-decoder-test
  (testing "init-encoder-decoder-model"
    (let [{:keys [encoder decoder]} (init-encoder-decoder-model {:input-items  nil
                                                                 :output-items #{"A" "B" "C"}
                                                                 :input-type :dense
                                                                 :input-size 3
                                                                 :encoder-hidden-size 10
                                                                 :decoder-hidden-size 20
                                                                 :output-type :binary-classification
                                                                 :embedding {"A" (array (map float [1 2 3]))
                                                                             "B" (array (map float [1 2 3]))
                                                                             "C" (array (map float [1 2 3]))}
                                                                 :embedding-size 3})
          {eh :hidden eis :input-size} encoder
          {dh :hidden dis :input-size o :output es :encoder-size} decoder]
      ;; encoder
      (is (= eis 3))
      (is (= 10  (row-count (:block-w eh))))
      (is (= 10 (row-count (:block-wr eh))))
      (is (= 10  (row-count (:input-gate-w eh))))
      (is (= 10 (row-count (:input-gate-wr eh))))
      (is (= 10  (row-count (:forget-gate-w eh))))
      (is (= 10 (row-count (:forget-gate-wr eh))))
      (is (= 10  (row-count (:output-gate-w eh))))
      (is (= 10 (row-count (:output-gate-wr eh))))
      (is (= 10  (ecount (:block-bias eh))))
      (is (= 10  (ecount (:input-gate-bias eh))))
      (is (= 10  (ecount (:forget-gate-bias eh))))
      (is (= 10  (ecount (:output-gate-bias eh))))
      (is (= 10  (ecount (:input-gate-peephole eh))))
      (is (= 10  (ecount (:forget-gate-peephole eh))))
      (is (= 10  (ecount (:output-gate-peephole eh))))
      ;decoder
      (is (= es 10))
      (is (= dis 3))
      ;; encoder connection
      (is (= 20 (row-count (:block-we dh))))
      (is (= 20 (row-count (:input-gate-we dh))))
      (is (= 20 (row-count (:forget-gate-we dh))))
      (is (= 20 (row-count (:output-gate-we dh))))
      ;; same as standard lstm
      (is (= 20  (row-count (:block-w dh))))
      (is (= 20 (row-count (:block-wr dh))))
      (is (= 20  (row-count (:input-gate-w dh))))
      (is (= 20 (row-count (:input-gate-wr dh))))
      (is (= 20  (row-count (:forget-gate-w dh))))
      (is (= 20 (row-count (:forget-gate-wr dh))))
      (is (= 20  (row-count (:output-gate-w dh))))
      (is (= 20 (row-count (:output-gate-wr dh))))
      (is (= 20  (ecount (:block-bias dh))))
      (is (= 20  (ecount (:input-gate-bias dh))))
      (is (= 20  (ecount (:forget-gate-bias dh))))
      (is (= 20  (ecount (:output-gate-bias dh))))
      (is (= 20  (ecount (:input-gate-peephole dh))))
      (is (= 20  (ecount (:forget-gate-peephole dh))))
      (is (= 20  (ecount (:output-gate-peephole dh))))
      ;; decoder output
      (let [{:keys [w bias encoder-w previous-input-w]} (get o "A")]
        (is (= 20 (ecount w)))
        (is (= 1 (ecount bias)))
        (is (= 10 (ecount encoder-w)))
        (is (= 3 (ecount previous-input-w))))

      ))


  (testing "decoder-lstm-activation"
    (let [{a1 :activation s1 :state} (decoder-lstm-activation decoder-sample-network
                                                              (float-array (take 3 (repeat 2)))
                                                              (float-array (take 10 (repeat 2)))
                                                              (float-array 5)
                                                              (float-array (take 10 (repeat 2))))
          {a2 :activation s2 :state} (lstm-activation sample-w-network
                                                      (float-array (take 3 (repeat 2)))
                                                      (float-array (take 10 (repeat 2)))
                                                      (float-array (take 10 (repeat 2))))]
      (is (= (mapv float a1)
             (mapv float a2)
             (take 10 (repeat (float 0.787542)))))
      (is (= (mapv float (:lstm s1))
             (mapv float (:lstm s2))
             (take 10 (repeat (float 0.787542)))))
      (is (= (mapv float (:block s1))
             (mapv float (:block s2))
             (take 10 (repeat (float 1.6)))))
      (is (= (mapv float (:input-gate s1))
             (mapv float (:input-gate s2))
             (take 10 (repeat (float 1.4)))))
      (is (= (mapv float (:forget-gate s1))
             (mapv float (:forget-gate s2))
             (take 10 (repeat (float 1.4)))))
      (is (= (mapv float (:output-gate s1))
             (mapv float (:output-gate s2))
             (take 10 (repeat (float 1.4)))))
      (is (= (mapv float (:cell-state  s1))
             (mapv float (:cell-state  s2))
             (take 10 (repeat (float 2.3437154))))))
    (let [{a :activation s :state} (decoder-lstm-activation decoder-sample-network
                                                            (float-array (take 3 (repeat 2)))
                                                            (float-array (take 10 (repeat 2)))
                                                            (float-array (take 5 (repeat 3)))
                                                            (float-array (take 10 (repeat 2))))]
      (is (= (mapv float a) (take 10 (repeat (float 0.8342077)))))
      (is (= (mapv float (:lstm s)) (take 10 (repeat (float 0.8342077)))))
      (is (= (mapv float (:block s)) (take 10 (repeat (float 1.9)))))
      (is (= (mapv float (:input-gate s)) (take 10 (repeat (float 1.7)))))
      (is (= (mapv float (:forget-gate s)) (take 10 (repeat (float 1.7)))))
      (is (= (mapv float (:output-gate s)) (take 10 (repeat (float 1.7)))))))
  (testing "encoder-forward"
    (let [result (encoder-forward sample-w-network (map array [[1 0 0] [1 0 0]]))
          {:keys [hidden input]} (last result)
          {:keys [block input-gate forget-gate output-gate]} (:state hidden)]
      (is (= (vec input) (map float [1 0 0])))
      (is (= 2 (count result)))
      (is (= (mapv float block)       (take 10 (repeat (float -0.9590061)))))
      (is (= (mapv float input-gate)  (take 10 (repeat (float -0.93830144)))))
      (is (= (mapv float forget-gate) (take 10 (repeat (float -0.93830144)))))
      (is (= (mapv float output-gate) (take 10 (repeat (float -0.93830144)))))))

  (testing "decoder-output-activation"
    (is (= (decoder-output-activation decoder-sample-network
                                      (float-array (take 10 (repeat (float 0.05))))
                                      (float-array (take 5 (repeat (float 0.1))))
                                      (float-array (map float [0.2 0.2 0.1]))
                                      #{"prediction1"})
           {"prediction1" (float 0.33737817)})))

  (testing "decoder-activation-time-fixed"
    (let [{:keys [activation state]} (decoder-activation-time-fixed decoder-sample-network
                                                                    (array (take 3 (repeat (float 0.3))))
                                                                    #{"prediction1"}
                                                                    (array (take 10 (repeat (float 0.1))))
                                                                    (array (take 5 (repeat (float 0.1))))
                                                                    (array (map float [0.2 0.2 0.1]))
                                                                    (array (take 10 (repeat (float 0.25)))))
          {hs :hidden} state]
      (is (= (mapv float (:input activation))
             (take 3 (repeat (float 0.3)))))
      (is (= (mapv float (:hidden  activation))
             (take 10 (repeat (float -0.038238227)))))
      (is (= (mapv float (:block hs))
             (take 10 (repeat (float -0.8)))))
      (is (= (mapv float (:input-gate hs))
             (take 10 (repeat (float -0.825)))))
      (is (= (mapv float (:forget-gate hs))
             (take 10 (repeat (float -0.825)))))
      (is (= (mapv float (:output-gate hs))
             (take 10 (repeat (float -0.825)))))))

  (testing "decorder-forward"
    (let [it1 (vec (:output (:activation (last (decoder-forward decoder-sample-network
                                                                (map array [[2 0 0] [1 0 0]])
                                                                (array (repeat 5 0))
                                                                [:skip #{"prediction1" "prediction2" "prediction3"}])))))
          it2 (vec (:output (:activation (last (sequential-output sample-w-network
                                                                  (map array [[2 0 0] [1 0 0]])
                                                                  [:skip #{"prediction1" "prediction2" "prediction3"}])))))]
      (is (not= it1 it2))
      (is (= (->> it1 (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.36052307) "prediction1" (float 0.36052307) "prediction3" (float 0.36052307)})))
    (let [result (decoder-forward decoder-sample-network
                                  (map float-array [[2 0 0] [1 0 0]])
                                  (float-array (take 5 (repeat (float -0.1))))
                                  [:skip #{"prediction1" "prediction2" "prediction3"}])
          {:keys [activation state]} (last result)
          {:keys [hidden output]} activation]
      (is (= (mapv float hidden) (take 10 (repeat (float -0.07243964)))))
      (is (= (->> output vec (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.326856) "prediction1" (float 0.326856) "prediction3" (float 0.326856)}))))
  (testing "encoder-decoder-forward"
    (let [{:keys [encoder decoder]} (encoder-decoder-forward sample-encoder-decoder
                                                             (map array [[2 0 0] [0 -1 1]])
                                                             (map array [[-1 1 -1] [2 -1 1]])
                                                             [#{"prediction1" "prediction2"} #{"prediction2" "prediction3"}])
          {:keys [hidden output]} (:activation (last decoder))]
      (is (= (mapv float (:activation (:hidden (last encoder))))
             (take 5 (repeat (float -0.06823918)))))
      (is (= (mapv float hidden) (take 10 (repeat (float -0.07979079)))))
      (is (= (->> output vec (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.19276398) "prediction3" (float 0.19276398)}))))



  ;;   BPTT   ;;



  (testing "decoder-output-param-delta"
    (let [result (->> (decoder-output-param-delta {:matrix-kit default-matrix-kit}
                                                  {"A" 0.5 "B" 0 "C" -0.5}
                                                  10
                                                  (array (range 10))
                                                  5
                                                  (array (take 5 (repeat (float 0.1))))
                                                  3
                                                  (array (take 3 (repeat (float -0.1)))))
                      (reduce (fn [acc [item {:keys [w-delta bias-delta encoder-w-delta previous-input-w-delta]}]]
                                (assoc acc item {:w-delta (mapv float w-delta)
                                                 :bias-delta (map float bias-delta)
                                                 :encoder-w-delta (map float encoder-w-delta)
                                                 :previous-input-w-delta (map float previous-input-w-delta)}))
                              {}))
          {:strs [A B C]} result]
      (is (= A {:w-delta (map float [0.0 0.5 1.0 1.5 2.0 2.5 3.0 3.5 4.0 4.5])
                :bias-delta [(float 0.5)]
                :encoder-w-delta (take 5 (repeat (float 0.05)))
                :previous-input-w-delta (take 3 (repeat (float -0.05)))}))
      (is (= B {:w-delta (take 10 (repeat (float 0)))
                :bias-delta [(float 0)]
                :encoder-w-delta (take 5 (repeat (float 0)))
                :previous-input-w-delta (take 3 (repeat (float 0)))}))
      (is (= C {:w-delta (map float [-0.0 -0.5 -1.0 -1.5 -2.0 -2.5 -3.0 -3.5 -4.0 -4.5])
                :bias-delta [(float -0.5)]
                :encoder-w-delta (take 5 (repeat (float -0.05)))
                :previous-input-w-delta (take 3 (repeat (float 0.05)))}))))
  (testing "decoder-lstm-param-delta"
    (let [result (decoder-lstm-param-delta decoder-sample-network
                                           {:block-delta       (float-array (take 10 (repeat 1)))
                                            :input-gate-delta  (float-array (take 10 (repeat 1)))
                                            :forget-gate-delta (float-array (take 10 (repeat 1)))
                                            :output-gate-delta (float-array (take 10 (repeat 1)))}
                                           (float-array [2 1 -1])
                                           (float-array (take 10 (repeat (float 0.2))))
                                           (float-array (take 5 (repeat (float 0.02))))
                                           {:cell-state (float-array (take 10 (repeat (float -0.1))))})]
      (is (= (map vec (:block-w-delta result))  (take 10 (repeat (map float [2.0 1.0 -1.0])))))
      (is (= (map vec (:block-wr-delta result)) (partition 10 (take 100 (repeat (float 0.2))))))
      (is (= (map vec (:input-gate-w-delta result)) (take 10 (repeat (map float [2.0 1.0 -1.0])))))
      (is (= (map vec (:input-gate-wr-delta result))  (partition 10 (take 100 (repeat (float 0.2))))))
      (is (= (map vec (:forget-gate-w-delta result)) (take 10 (repeat (map float [2.0 1.0 -1.0])))))
      (is (= (map vec (:forget-gate-wr-delta result)) (partition 10 (take 100 (repeat (float 0.2))))))
      (is (= (map vec (:output-gate-w-delta result))  (take 10 (repeat (map float [2.0 1.0 -1.0])))))
      (is (= (map vec (:output-gate-wr-delta result)) (partition 10 (take 100 (repeat (float 0.2))))))
      ;; encoder connection
      (is (= (map vec (:block-we-delta result)) (partition 5 (take 50 (repeat (float 0.02))))))
      (is (= (map vec (:input-gate-we-delta result)) (partition 5  (take 50 (repeat (float 0.02))))))
      (is (= (map vec (:forget-gate-we-delta result)) (partition 5  (take 50 (repeat (float 0.02))))))
      (is (= (map vec (:output-gate-we-delta result)) (partition 5  (take 50 (repeat (float 0.02))))))
      ;; bias and peppholes
      (is (= (vec (:block-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:input-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:forget-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:output-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:peephole-input-gate-delta  result)) (take 10 (repeat (float -0.1)))))
      (is (= (vec (:peephole-forget-gate-delta result)) (take 10 (repeat (float -0.1)))))
      (is (= (vec (:peephole-output-gate-delta result)) (take 10 (repeat (float -0.1)))))))
  (testing "encoder-bptt"
    (let [hd (:hidden-delta (encoder-bptt encoder-sample-network
                                          (encoder-forward encoder-sample-network (map float-array [[1 0 0] [1 0 0]]))
                                          (float-array (take 5 (repeat (float -0.5))))))]
      (is (= (row-count (:block-w-delta                 hd)) 5))
      (is (= (row-count (:block-w-delta   hd)) 5))
      (is (= (row-count (:block-wr-delta  hd)) 5))
      (is (= (row-count (:input-gate-w-delta   hd)) 5))
      (is (= (row-count (:input-gate-w-delta   hd)) 5))
      (is (= (row-count (:input-gate-wr-delta  hd)) 5))
      (is (= (row-count (:forget-gate-w-delta  hd)) 5))
      (is (= (row-count (:forget-gate-w-delta  hd)) 5))
      (is (= (row-count (:forget-gate-wr-delta  hd)) 5))
      (is (= (row-count (:forget-gate-wr-delta  hd)) 5))
      (is (= (row-count (:output-gate-w-delta   hd)) 5))
      (is (= (row-count (:output-gate-w-delta   hd)) 5))
      (is (= (row-count (:output-gate-wr-delta  hd)) 5))
      ;; bias and peephole
      (is (= (ecount (remove zero? (:block-bias-delta           hd))) 5))
      (is (= (ecount (remove zero? (:input-gate-bias-delta      hd))) 5))
      (is (= (ecount (remove zero? (:forget-gate-bias-delta     hd))) 5))
      (is (= (ecount (remove zero? (:output-gate-bias-delta     hd))) 5))
      (is (= (ecount (remove zero? (:peephole-input-gate-delta  hd))) 5))
      (is (= (ecount (remove zero? (:peephole-forget-gate-delta hd))) 5))
      (is (= (ecount (remove zero? (:peephole-output-gate-delta hd))) 5))))
  (testing "decoder-bptt"
    (let [encoder-input (float-array (take 5 (repeat (float -0.1))))
          {:keys [loss param-loss]} (decoder-bptt decoder-sample-network
                                                  (decoder-forward decoder-sample-network
                                                                   (map float-array [[2 0 0] [1 0 0]])
                                                                   encoder-input
                                                                   [:skip #{"prediction1" "prediction2" "prediction3"}])
                                                  encoder-input
                                                  [:skip {:pos ["prediction2"] :neg ["prediction3"]}])
          {hd :hidden-delta od :output-delta ed :encoder-delta} param-loss
          {:keys [w-delta bias-delta encoder-w-delta previous-input-w-delta]} (get od "prediction2")]
      (is (= loss [{} {"prediction2" (double 0.6731440126895905), "prediction3" (double -0.32685598731040955)}]))
      (is (= (row-count (:block-w-delta hd)) 10))
      (is (= (row-count (:block-w-delta   hd))) 10)
      (is (= (row-count (:block-wr-delta  hd))) 10)
      (is (= (row-count (:input-gate-w-delta   hd)) 10))
      (is (= (row-count (:input-gate-w-delta   hd)) 10))
      (is (= (row-count (:input-gate-wr-delta  hd)) 10))
      (is (= (row-count (:forget-gate-w-delta  hd)) 10))
      (is (= (row-count (:forget-gate-w-delta  hd))) 10)
      (is (= (row-count (:forget-gate-wr-delta  hd)) 10))
      (is (= (row-count (:forget-gate-wr-delta  hd)) 10))
      (is (= (row-count (:output-gate-w-delta   hd)) 10))
      (is (= (row-count (:output-gate-w-delta   hd))) 10)
      (is (= (row-count (:output-gate-wr-delta  hd))) 10)
      ;; encoder-connection
      (is (= (row-count (:block-we-delta  hd)) 10))
      (is (= (row-count (:input-gate-we-delta  hd)) 10))
      (is (= (row-count (:forget-gate-we-delta  hd)) 10))
      (is (= (row-count (:output-gate-we-delta  hd)) 10))
      ;; bias and peepholes
      (is (= (ecount (:block-bias-delta           hd)) 10))
      (is (= (ecount (:input-gate-bias-delta      hd)) 10))
      (is (= (ecount (:forget-gate-bias-delta     hd)) 10))
      (is (= (ecount (:output-gate-bias-delta     hd)) 10))
      (is (= (ecount (:peephole-input-gate-delta  hd)) 10))
      (is (= (ecount (:peephole-forget-gate-delta hd)) 10))
      (is (= (ecount (:peephole-output-gate-delta hd)) 10))
      ;; output
      (is (= (mapv float w-delta)
             (take 10 (repeat (float -0.04876231)))))
      (is (= (mapv float bias-delta) [(float 0.673144)]))
      (is (= (mapv float encoder-w-delta)
             (take 5 (repeat (float -0.0673144)))))
      (is (= (mapv float previous-input-w-delta)
             (map float [1.346288 0.0 0.0])))
      ;; encoder-delta
      (is (= (mapv float ed)
             (take 5 (repeat (float -4.5863548E-4)))))))

  (testing "encoder-decoder-bptt"
    (let [{:keys [loss param-loss]} (encoder-decoder-bptt sample-encoder-decoder
                                                          (encoder-decoder-forward sample-encoder-decoder
                                                                                   (map float-array [[2 0 0] [0 -1 1]])
                                                                                   (map float-array [[-1 1 -1] [2 -1 1]])
                                                                                   [#{"prediction1" "prediction2"} #{"prediction2" "prediction3"}])
                                                          [{:pos ["prediction1"] :neg ["prediction2"]} {:pos ["prediction2"] :neg ["prediction3"]}])
          {:keys [encoder-param-delta decoder-param-delta]} param-loss]
      (is (not (nil? encoder-param-delta)))
      (is (not (nil? decoder-param-delta)))))

  (testing "update-decoder!"
    (let [encoder-input (array (take 5 (repeat (float -0.1))))
          result (update-decoder! decoder-sample-network
                                  (:param-loss (decoder-bptt decoder-sample-network
                                                             (decoder-forward decoder-sample-network
                                                                              (map array [[2 0 0] [1 0 0]])
                                                                              encoder-input
                                                                              [:skip #{"prediction1" "prediction2" "prediction3"}])
                                                             encoder-input
                                                             [:skip {:pos ["prediction2"] :neg ["prediction3"]}]))
                                  0.1)
          {hd :hidden o :output} result]
      (is (= (row-count (:block-w hd)) 10))
      (is (= (row-count (:block-wr hd)) 10))
      (is (= (row-count (:input-gate-w hd)) 10))
      (is (= (row-count (:input-gate-wr hd)) 10))
      (is (= (row-count (:forget-gate-w hd)) 10))
      (is (= (row-count (:forget-gate-wr hd)) 10))
      (is (= (row-count (:output-gate-w hd)) 10))
      (is (= (row-count (:output-gate-wr hd)) 10))

      (is (= (map #(mapv float %) (:block-w hd))
             (take 10 (repeat [(float 0.100179605) (float 0.1) (float 0.1)]))))
      (is (= (map #(mapv float %) (:input-gate-w hd))
             (take 10 (repeat [(float 0.099804856) (float 0.1) (float 0.1)]))))
      (is (= (map #(mapv float %) (:forget-gate-w hd))
             (take 10 (repeat [(float 0.099962) (float 0.1) (float 0.1)]))))
      (is (= (map #(mapv float %) (:output-gate-w hd))
             (take 10 (repeat [(float 0.09984027) (float 0.1) (float 0.1)]))))
      ;; reccurent connection
      (is (= (map #(mapv float %) (:block-wr hd))
             (partition 10 (take 100 (repeat (float 0.099993))))))
      (is (= (map #(mapv float %) (:input-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.10000865))))))
      (is (= (map #(mapv float %) (:forget-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.10000238))))))
      (is (= (map #(mapv float %) (:output-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.10001133))))))
      ;; encoder connection
      (is (= (map #(mapv float %) (:block-we hd))
             (partition 5 (take 50 (repeat (float 0.01998543))))))
      (is (= (map #(mapv float %) (:input-gate-we hd))
             (partition 5 (take 50 (repeat (float 0.020016667))))))
      (is (= (map #(mapv float %) (:forget-gate-we hd))
             (partition 5 (take 50 (repeat (float 0.0200038))))))
      (is (= (map #(mapv float %) (:output-gate-we hd))
             (partition 5 (take 50 (repeat (float 0.020017035))))))
      ;peephole
      (is (= (mapv float (:input-gate-peephole hd))
             (take 10 (repeat (float -0.09997151)))))
      (is (= (mapv float (:forget-gate-peephole hd))
             (take 10 (repeat (float -0.09999216)))))
      (is (= (mapv float (:output-gate-peephole hd))
             (take 10 (repeat (float -0.09996269)))))

      (is (= (mapv float (:input-gate-bias  hd)) (take 10 (repeat (float -1.0001667)))))
      (is (= (mapv float (:forget-gate-bias hd)) (take 10 (repeat (float -1.000038)))))
      (is (= (mapv float (:output-gate-bias hd)) (take 10 (repeat (float -1.0001704)))))
      (let [{:keys [w bias encoder-w previous-input-w]} (get o "prediction3")]
        (is (= (mapv float w)
               (take 10 (repeat (float 0.10236774)))))
        (is (= (mapv float bias)
               [(float -1.0326856)]))
        (is (= (mapv float encoder-w)
               (take 5 (repeat (float 0.30326855)))))
        (is (= (mapv float previous-input-w)
               (map float [0.1846288 0.25 0.25]))))))
  (testing "update-encoder-decoder!"
    (let [encoder-input (array (take 5 (repeat (float -0.1))))]
      (update-encoder-decoder! sample-encoder-decoder
                               (:param-loss (encoder-decoder-bptt sample-encoder-decoder
                                                                  (encoder-decoder-forward sample-encoder-decoder
                                                                                           (map array [[2 0 0] [0 -1 1]])
                                                                                           (map array [[-1 1 -1] [2 -1 1]])
                                                                                           [#{"prediction1" "prediction2"} #{"prediction2" "prediction3"}])
                                                                  [{:pos ["prediction1"] :neg ["prediction2"]} {:pos ["prediction2"] :neg ["prediction3"]}]))
                               0.01)
      (let [{:keys [encoder decoder]} sample-encoder-decoder
            {eh :hidden __ :output} encoder
            {dh :hidden do :output} decoder]
        ;; encoder
        (is (= (row-count (:block-w eh)) 5))
        (is (= (row-count (:block-wr eh)) 5))
        (is (= (row-count (:input-gate-w eh)) 5))
        (is (= (row-count (:input-gate-wr eh)) 5))
        (is (= (row-count (:forget-gate-w eh)) 5))
        (is (= (row-count (:forget-gate-wr eh)) 5))
        (is (= (row-count (:output-gate-w eh)) 5))
        (is (= (row-count (:output-gate-wr eh)) 5))

        ;; Caution! decoder's parameter already have updated by above test.
        (is (= (row-count (:block-w dh)) 10))
        (is (= (row-count (:block-wr dh)) 10))
        (is (= (row-count (:input-gate-w dh)) 10))
        (is (= (row-count (:input-gate-wr dh)) 10))
        (is (= (row-count (:forget-gate-w dh)) 10))
        (is (= (row-count (:forget-gate-wr dh)) 10))
        (is (= (row-count (:output-gate-w dh)) 10))
        (is (= (row-count (:output-gate-wr dh)) 10))
        ;; encoder connection
        (is (= (map #(mapv float %) (:block-we dh))
               (partition 5 (take 50 (repeat (float 0.019982643))))))
        (is (= (map #(mapv float %) (:input-gate-we dh))
               (partition 5 (take 50 (repeat (float 0.020020049))))))
        (is (= (map #(mapv float %) (:forget-gate-we dh))
               (partition 5 (take 50 (repeat (float 0.020004272))))))
        (is (= (map #(mapv float %) (:output-gate-we dh))
               (partition 5 (take 50 (repeat (float 0.020020425))))))
        ;; decoder output
        (let [{:keys [w bias encoder-w previous-input-w]} (get do "prediction1")]
          (is (= 10 (ecount w)))
          (is (= 1  (ecount bias)))
          (is (= 5  (ecount encoder-w)))
          (is (= 3  (ecount previous-input-w))))))))

