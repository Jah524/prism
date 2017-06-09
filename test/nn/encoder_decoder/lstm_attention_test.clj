(ns nn.encoder-decoder.lstm-attention-test
  (:require
    [clojure.test   :refer :all]
    [clojure.pprint :refer [pprint]]
    [clojure.core.matrix :refer [mget array matrix ecount row-count]]
    [nn.rnn.lstm-test   :refer [sample-w-network]]
    [nn.encoder-decoder.lstm-test   :refer [encoder-sample-network sample-encoder-decoder]]
    [prism.nn.rnn.lstm :as rnn]
    [prism.nn.encoder-decoder.lstm :as ed]
    [prism.nn.encoder-decoder.lstm-attention :refer :all]))

(deftest encoder-decoder-lstm-attention-test

  (testing "alignment"
    (let [{:keys [activation context w]} (alignment (rnn/context sample-w-network (map array [[1 0 0] [0 2 0]])))]
      (is (= (seq activation)
             (repeat 10 -0.06954023878261448)))
      (is (= (mapv seq w)
             [(repeat 10 0.5053235697775816)
              (repeat 10 0.49467643022241836)]))))
  (testing "encoder-decoder-forward"
    (let [{:keys [encoder encoder-alignment decoder]} (encoder-decoder-forward sample-encoder-decoder
                                                                               (map array [[2 0 0] [0 -1 1]])
                                                                               (map array [[-1 1 -1] [2 -1 1]])
                                                                               [#{"prediction1" "prediction2"} #{"prediction2" "prediction3"}])
          {:keys [activation w] :as al} encoder-alignment
          {:keys [hidden output]} (:activation (last decoder))]
      (is (= (seq activation)
             (repeat 5 -0.06558146299817119)))
      (is (= (mapv seq w)
             [(repeat 5 0.5013253395777532)
              (repeat 5 0.4986746604222469)]))
      (is (= (-> encoder last :hidden :activation seq)
             (repeat 5 -0.06823917447812336)))
      (is (= (mapv float (:activation (:hidden (last encoder))))
             (take 5 (repeat (float -0.068239175)))))

      (is (= (mapv float hidden) (take 10 (repeat (float -0.079808645)))))
      (is (= (->> output vec (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.1933823) "prediction3" (float 0.1933823)}))))

  (testing "encoder-bptt"
    (let [c (rnn/context encoder-sample-network (map float-array [[1 0 0] [1 0 0]]))
          hd (:hidden-delta (encoder-bptt encoder-sample-network
                                          c
                                          (alignment c)
                                          (float-array (take 5 (repeat (float -0.5))))))
          {:keys [block-w-delta block-wr-delta
                  input-gate-w-delta input-gate-wr-delta
                  forget-gate-w-delta forget-gate-wr-delta
                  output-gate-w-delta output-gate-wr-delta
                  block-bias-delta input-gate-bias-delta forget-gate-bias-delta output-gate-bias-delta
                  peephole-input-gate-delta peephole-forget-gate-delta peephole-output-gate-delta]} hd]
      (is (= (row-count block-w-delta) 5))
      (is (= (row-count block-wr-delta) 5))
      (is (= (row-count input-gate-w-delta) 5))
      (is (= (row-count input-gate-wr-delta) 5))
      (is (= (row-count forget-gate-w-delta) 5))
      (is (= (row-count forget-gate-wr-delta) 5))
      (is (= (row-count output-gate-w-delta) 5))
      (is (= (row-count output-gate-wr-delta) 5))
      ;; bias and peephole
      (is (= (ecount (remove zero? block-bias-delta)) 5))
      (is (= (ecount (remove zero? input-gate-bias-delta)) 5))
      (is (= (ecount (remove zero? forget-gate-bias-delta)) 5))
      (is (= (ecount (remove zero? output-gate-bias-delta)) 5))
      (is (= (ecount (remove zero? peephole-input-gate-delta)) 5))
      (is (= (ecount (remove zero? peephole-forget-gate-delta)) 5))
      (is (= (ecount (remove zero? peephole-output-gate-delta)) 5))
      (is (= (mapv seq block-w-delta)
             (repeat 5 [-0.02347160948764427,0.0,0.0])))
      (is (= (mapv seq input-gate-w-delta)
             (repeat 5 [0.02582521813310183,0.0,0.0])))
      (is (= (mapv seq forget-gate-w-delta)
             (repeat 5 [0.005781312168347074,0.0,0.0])))
      (is (= (mapv seq output-gate-w-delta)
             (repeat 5 [0.026186607153362962,0.0,0.0])))
      (is (= (mapv seq block-wr-delta)
             (repeat 5 (repeat 5 0.0010785339140916765))))
      (is (= (mapv seq input-gate-wr-delta)
             (repeat 5 (repeat 5 -0.0012033553969636056))))
      (is (= (mapv seq forget-gate-wr-delta)
             (repeat 5 (repeat 5 -3.411324685202444E-4))))
      (is (= (mapv seq output-gate-wr-delta)
             (repeat 5 (repeat 5 -0.0015882559772432037))))
      (is (= (seq block-bias-delta)
             (repeat 5 -0.02347160948764427)))
      (is (= (seq input-gate-bias-delta)
             (repeat 5 0.02582521813310183)))
      (is (= (seq forget-gate-bias-delta)
             (repeat 5 0.005781312168347074)))
      (is (= (seq output-gate-bias-delta)
             (repeat 5 0.026186607153362962)))
      (is (= (seq output-gate-bias-delta)
             (repeat 5 0.026186607153362962)))
      (is (= (seq peephole-input-gate-delta)
             (repeat 5 -0.0042224513480668404)))
      (is (= (seq peephole-forget-gate-delta)
             (repeat 5 -0.0011969990371981847)))
      (is (= (seq peephole-output-gate-delta)
             (repeat 5 -0.005573028225167473)))))
  (testing "encoder-decoder-bptt"
    (let [{:keys [loss param-loss]} (encoder-decoder-bptt sample-encoder-decoder
                                                          (encoder-decoder-forward sample-encoder-decoder
                                                                                   (map #(array :vectorz %) [[2 0 0] [0 -1 1]])
                                                                                   (map #(array :vectorz %) [[-1 1 -1] [2 -1 1]])
                                                                                   [#{"prediction1" "prediction2"} #{"prediction2" "prediction3"}])
                                                          [{:pos ["prediction1"] :neg ["prediction2"]} {:pos ["prediction2"] :neg ["prediction3"]}])
          {:keys [encoder-param-delta decoder-param-delta]} param-loss
          {:keys [block-w-delta block-wr-delta
                  input-gate-w-delta input-gate-wr-delta
                  forget-gate-w-delta forget-gate-wr-delta
                  output-gate-w-delta output-gate-wr-delta
                  block-bias-delta input-gate-bias-delta forget-gate-bias-delta output-gate-bias-delta
                  peephole-input-gate-delta peephole-forget-gate-delta peephole-output-gate-delta]} (:hidden-delta encoder-param-delta)]
      (is (not (nil? encoder-param-delta)))
      (is (not (nil? decoder-param-delta)))

      (is (= (mapv seq block-w-delta)
             (repeat 5 [-2.9170752753452023E-5 3.689655566272576E-5 -3.689655566272576E-5])))
      (is (= (mapv seq input-gate-w-delta)
             (repeat 5 [2.3906618398595367E-5 -5.2359521884721946E-5 5.2359521884721946E-5])))
      (is (= (mapv seq forget-gate-w-delta)
             (repeat 5 [0.0 -1.3917640614359764E-5 1.3917640614359764E-5])))
      (is (= (mapv seq output-gate-w-delta)
             (repeat 5 [-4.227838771386334E-6 -6.797787578326401E-5 6.797787578326401E-5])))
      (is (= (mapv seq block-wr-delta)
             (repeat 5 (repeat 5 2.3221881793921983E-6))))
      (is (= (mapv seq input-gate-wr-delta)
             (repeat 5 (repeat 5 -3.295393312882094E-6))))
      (is (= (mapv seq forget-gate-wr-delta)
             (repeat 5 (repeat 5 -8.75945733664924E-7))))
      (is (= (mapv seq output-gate-wr-delta)
             (repeat 5 (repeat 5 -4.278378205463774E-6))))
      (is (= (seq block-bias-delta)
             (repeat 5 -5.148193203945177E-5 )))
      (is (= (seq input-gate-bias-delta)
             (repeat 5 6.431283108401963E-5)))
      (is (= (seq forget-gate-bias-delta)
             (repeat 5 1.3917640614359764E-5)))
      (is (= (seq output-gate-bias-delta)
             (repeat 5 6.586395639757084E-5)))
      (is (= (seq peephole-input-gate-delta)
             (repeat 5 -1.077916807626728E-5)))
      (is (= (seq peephole-forget-gate-delta)
             (repeat 5 -2.865201629181461E-6)))
      (is (= (seq peephole-output-gate-delta)
             (repeat 5 -1.399449273331181E-5)))))
  )
