(ns nn.encoder-decoder.gru-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test   :refer :all]
    [clojure.core.matrix :refer [mget array matrix ecount row-count]]
    [prism.nn.rnn.gru  :refer [gru-activation forward context]]
    [nn.rnn.gru-test   :refer [sample-w-network]]
    [prism.nn.encoder-decoder.gru :refer :all]))


(def encoder-sample-network
  "assumed 3->5->3 connection"
  {:output-type :binary-classification
   :input-size 3
   :hidden-size 5
   :hidden {:w (matrix (partition 3 (take 15 (repeat 0.1))))
            :wr (matrix (partition 5 (take 25 (repeat 0.1))))
            :bias (array (take 5 (repeat -1)))
            :update-gate-w  (matrix (partition 3 (take 15 (repeat 0.1))))
            :update-gate-wr  (matrix (partition 5 (take 25 (repeat 0.1))))
            :update-gate-bias (array (take 5 (repeat -1)))
            :reset-gate-w (matrix (partition 3 (take 15 (repeat 0.1))))
            :reset-gate-wr (matrix (partition 5 (take 25 (repeat 0.1))))
            :reset-gate-bias (array (take 5 (repeat -1)))}
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
        :we (matrix (partition 5 (take 50 (repeat 0.02))))
        :update-gate-we (matrix (partition 5 (take 50 (repeat 0.02))))
        :reset-gate-we (matrix (partition 5 (take 50 (repeat 0.02)))))
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

(deftest encoder-decoder-gru-test
  (testing "decoder-activation-time-fixed"
    (let [{:keys [activation state]} (decoder-activation-time-fixed decoder-sample-network
                                                                    (array (take 3 (repeat (float 0.3))))
                                                                    #{"prediction1"}
                                                                    (array (take 10 (repeat (float 0.1))))
                                                                    (array (take 5 (repeat (float 0.1))))
                                                                    (array (map float [0.2 0.2 0.1])))
          {hs :hidden} state]
      (is (= (mapv float (:input activation))
             (take 3 (repeat (float 0.3)))))
      (is (= (mapv float (:gru (:hidden  activation)))
             (take 10 (repeat (float -0.1482884)))))
      (is (= (mapv float (:h-state hs))
             (take 10 (repeat (float -0.86899745)))))
      (is (= (mapv float (:update-gate hs))
             (take 10 (repeat (float -0.8)))))
      (is (= (mapv float (:reset-gate hs))
             (take 10 (repeat (float -0.8)))))))

  (testing "decorder-forward"
    (let [it1 (vec (:output (:activation (last (decoder-forward decoder-sample-network
                                                                (map array [[2 0 0] [1 0 0]])
                                                                (array (repeat 5 0))
                                                                [:skip #{"prediction1" "prediction2" "prediction3"}])))))
          it2 (vec (:output (:activation (last (forward sample-w-network
                                                        (map array [[2 0 0] [1 0 0]])
                                                        [:skip #{"prediction1" "prediction2" "prediction3"}])))))]
      (is (not= it1 it2))
      (is (= (->> it1 (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.30179694) "prediction1" (float 0.30179694) "prediction3" (float 0.30179694)})))
    (let [result (decoder-forward decoder-sample-network
                                  (map float-array [[2 0 0] [1 0 0]])
                                  (float-array (take 5 (repeat (float -0.1))))
                                  [:skip #{"prediction1" "prediction2" "prediction3"}])
          {:keys [activation state]} (last result)
          {:keys [hidden output]} activation]
      (is (= (mapv float (:gru hidden)) (take 10 (repeat (float -0.3390219)))))
      (is (= (->> output vec (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.27110532) "prediction1" (float 0.27110532) "prediction3" (float 0.27110532)}))))

  (testing "encoder-decoder-forward"
    (let [{:keys [encoder decoder]} (encoder-decoder-forward sample-encoder-decoder
                                                             (map array [[2 0 0] [0 -1 1]])
                                                             (map array [[-1 1 -1] [2 -1 1]])
                                                             [#{"prediction1" "prediction2"} #{"prediction2" "prediction3"}])
          {:keys [hidden output]} (:activation (last decoder))]
      (is (= (mapv float (:gru (:activation (:hidden (last encoder)))))
             (take 5 (repeat (float -0.34698233)))))
      (is (= (mapv float (:gru hidden)) (take 10 (repeat (float -0.33217072)))))
      (is (= (->> output vec (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.108840086) "prediction3" (float 0.108840086)}))))

  (testing "gru-param-delta"
    (let [result (gru-param-delta decoder-sample-network
                                  {:unit-delta       (float-array (take 10 (repeat 1)))
                                   :update-gate-delta (float-array (take 10 (repeat 1)))
                                   :reset-gate-delta (float-array (take 10 (repeat 1)))}
                                  (float-array [2 1 -1])
                                  (float-array (take 10 (repeat (float 0.2))))
                                  (float-array (take 5 (repeat (float 0.02)))))]
      (is (= (map vec (:w-delta result))  (take 10 (repeat (map float [2.0 1.0 -1.0])))))
      (is (= (map vec (:wr-delta result)) (partition 10 (take 100 (repeat (float 0.2))))))
      (is (= (map vec (:update-gate-w-delta result)) (take 10 (repeat (map float [2.0 1.0 -1.0])))))
      (is (= (map vec (:update-gate-wr-delta result))  (partition 10 (take 100 (repeat (float 0.2))))))
      (is (= (map vec (:reset-gate-w-delta result))  (take 10 (repeat (map float [2.0 1.0 -1.0])))))
      (is (= (map vec (:reset-gate-wr-delta result)) (partition 10 (take 100 (repeat (float 0.2))))))
      ;; encoder connection
      (is (= (map vec (:we-delta result)) (partition 5 (take 50 (repeat (float 0.02))))))
      (is (= (map vec (:update-gate-we-delta result)) (partition 5  (take 50 (repeat (float 0.02))))))
      (is (= (map vec (:reset-gate-we-delta result)) (partition 5  (take 50 (repeat (float 0.02))))))
      ;; bias and peppholes
      (is (= (vec (:bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:update-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:reset-gate-bias-delta result)) (take 10 (repeat (float 1)))))))

  (testing "encoder-bptt"
    (let [hd (:hidden-delta (encoder-bptt encoder-sample-network
                                          (context encoder-sample-network (map float-array [[1 0 0] [1 0 0]]))
                                          (float-array (take 5 (repeat (float -0.5))))))]
      (is (= (row-count (:w-delta   hd)) 5))
      (is (= (row-count (:wr-delta  hd)) 5))
      (is (= (row-count (:update-gate-w-delta   hd)) 5))
      (is (= (row-count (:update-gate-wr-delta  hd)) 5))
      (is (= (row-count (:reset-gate-w-delta  hd)) 5))
      (is (= (row-count (:reset-gate-wr-delta  hd)) 5))
      ;; bias and peephole
      (is (= (ecount (remove zero? (:bias-delta           hd))) 5))
      (is (= (ecount (remove zero? (:update-gate-bias-delta      hd))) 5))
      (is (= (ecount (remove zero? (:reset-gate-bias-delta     hd))) 5))))

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
      (is (= loss [{} {"prediction2" (double 0.7288946874842523), "prediction3" (double -0.2711053125157477)}]))
      (is (= (row-count (:w-delta hd)) 10))
      (is (= (row-count (:wr-delta  hd))) 10)
      (is (= (row-count (:update-gate-w-delta   hd)) 10))
      (is (= (row-count (:update-gate-wr-delta  hd)) 10))
      (is (= (row-count (:reset-gate-w-delta  hd)) 10))
      (is (= (row-count (:reset-gate-wr-delta  hd)) 10))
      ;; encoder-connection
      (is (= (row-count (:we-delta  hd)) 10))
      (is (= (row-count (:update-gate-we-delta  hd)) 10))
      (is (= (row-count (:reset-gate-we-delta  hd)) 10))
      ;; bias and peepholes
      (is (= (ecount (:bias-delta           hd)) 10))
      (is (= (ecount (:update-gate-bias-delta      hd)) 10))
      (is (= (ecount (:reset-gate-bias-delta     hd)) 10))
      ;; output
      (is (= (mapv float w-delta)
             (take 10 (repeat (float -0.24711126)))))
      (is (= (mapv float bias-delta) [(float 0.7288947)]))
      (is (= (mapv float encoder-w-delta)
             (take 5 (repeat (float -0.07288947)))))
      (is (= (map float (:w previous-input-w-delta))
             (map float [1.4577894 0.0 0.0])))
      ;; encoder-delta
      (is (= (mapv float ed)
             (take 5 (repeat (float 0.08101824)))))))

  (testing "encoder-decoder-bptt"
    (let [{:keys [loss param-loss]} (encoder-decoder-bptt sample-encoder-decoder
                                                          (encoder-decoder-forward sample-encoder-decoder
                                                                                   (map #(array :vectorz %) [[2 0 0] [0 -1 1]])
                                                                                   (map #(array :vectorz %) [[-1 1 -1] [2 -1 1]])
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
      (is (= (row-count (:w hd)) 10))
      (is (= (row-count (:wr hd)) 10))
      (is (= (row-count (:update-gate-w hd)) 10))
      (is (= (row-count (:update-gate-wr hd)) 10))
      (is (= (row-count (:reset-gate-w hd)) 10))
      (is (= (row-count (:reset-gate-wr hd)) 10))

      (is (= (map #(mapv float %) (:w hd))
             (take 10 (repeat [(float 0.10786783) (float 0.1) (float 0.1)]))))
      (is (= (map #(mapv float %) (:update-gate-w hd))
             (take 10 (repeat [(float 0.15455884) (float 0.1) (float 0.1)]))))
      (is (= (map #(mapv float %) (:reset-gate-w hd))
             (take 10 (repeat [(float 0.09998072) (float 0.1) (float 0.1)]))))
      ;; reccurent connection
      (is (= (map #(mapv float %) (:wr hd))
             (partition 10 (take 100 (repeat (float 0.09989627))))))
      (is (= (map #(mapv float %) (:update-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.09626293))))))
      (is (= (map #(mapv float %) (:reset-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.10000397))))))
      ;; encoder connection
      (is (= (map #(mapv float %) (:we hd))
             (partition 5 (take 50 (repeat (float 0.01958145))))))
      (is (= (map #(mapv float %) (:update-gate-we hd))
             (partition 5 (take 50 (repeat (float 0.016365709))))))
      (is (= (map #(mapv float %) (:reset-gate-we hd))
             (partition 5 (take 50 (repeat (float 0.020001927))))))
      (is (= (mapv float (:bias  hd)) (take 10 (repeat (float -0.9958145)))))
      (is (= (mapv float (:update-gate-bias  hd)) (take 10 (repeat (float -0.9636571)))))
      (is (= (mapv float (:reset-gate-bias hd)) (take 10 (repeat (float -1.0000193)))))
      (let [{:keys [w bias encoder-w previous-input-w]} (get o "prediction3")]
        (is (= (mapv float w)
               (take 10 (repeat (float 0.10919107)))))
        (is (= (mapv float bias)
               [(float -1.0271106)]))
        (is (= (mapv float encoder-w)
               (take 5 (repeat (float 0.30271104)))))
        (is (= (mapv float previous-input-w)
               (map float [0.19577894 0.25 0.25]))))))
  )

