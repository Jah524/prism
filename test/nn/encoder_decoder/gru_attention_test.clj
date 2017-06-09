(ns nn.encoder-decoder.gru-attention-test
  (:require
    [clojure.test   :refer :all]
    [clojure.pprint :refer [pprint]]
    [clojure.core.matrix :refer [mget array matrix ecount row-count]]
    [nn.rnn.gru-test   :refer [sample-w-network]]
    [nn.encoder-decoder.gru-test   :refer [encoder-sample-network sample-encoder-decoder]]
    [prism.nn.rnn.gru :as rnn]
    [prism.nn.encoder-decoder.gru :as ed]
    [prism.nn.encoder-decoder.gru-attention :refer :all]))

(deftest encoder-decoder-gru-attention-test

  (testing "alignment"
    (let [{:keys [activation context w]} (alignment (rnn/context sample-w-network (map array [[1 0 0] [0 2 0]])))]
      (is (= (seq activation)
             [-0.2679385845025424 -0.2679385845025424 -0.2679385845025424 -0.2679385845025424 -0.2679385845025424
              -0.2679385845025424 -0.2679385845025424 -0.2679385845025424 -0.2679385845025424 -0.2679385845025424]))
      (is (= (mapv seq w)
             [[0.5325180351749467 0.5325180351749467 0.5325180351749467 0.5325180351749467 0.5325180351749467
               0.5325180351749467 0.5325180351749467 0.5325180351749467 0.5325180351749467 0.5325180351749467]
              [0.46748196482505333 0.46748196482505333 0.46748196482505333 0.46748196482505333 0.46748196482505333
               0.46748196482505333 0.46748196482505333 0.46748196482505333 0.46748196482505333 0.46748196482505333]]))))
  (testing "encoder-decoder-forward"
    (let [{:keys [encoder encoder-alignment decoder]} (encoder-decoder-forward sample-encoder-decoder
                                                                               (map array [[2 0 0] [0 -1 1]])
                                                                               (map array [[-1 1 -1] [2 -1 1]])
                                                                               [#{"prediction1" "prediction2"} #{"prediction2" "prediction3"}])
          {:keys [activation w] :as al} encoder-alignment
          {:keys [hidden output]} (:activation (last decoder))]
      (is (= (seq activation)
             [-0.2714552956707925,-0.2714552956707925,-0.2714552956707925,-0.2714552956707925,-0.2714552956707925]))
      (is (= (mapv seq w)
             [[0.5352200723774158 0.5352200723774158 0.5352200723774158 0.5352200723774158 0.5352200723774158],
              [0.4647799276225842 0.4647799276225842 0.4647799276225842 0.4647799276225842 0.4647799276225842]]))
      (is (= (-> encoder last :hidden :activation :gru seq)
             [-0.34698233717751137 -0.34698233717751137 -0.34698233717751137 -0.34698233717751137 -0.34698233717751137]))
      (is (= (mapv float (:gru (:activation (:hidden (last encoder)))))
             (take 5 (repeat (float -0.34698233)))))

      (is (= (mapv float (:gru hidden)) (take 10 (repeat (float -0.332298)))))
      (is (= (->> output vec (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.12031179) "prediction3" (float 0.12031179)}))))

  (testing "encoder-bptt"
    (let [c (rnn/context encoder-sample-network (map float-array [[1 0 0] [1 0 0]]))
          hd (:hidden-delta (encoder-bptt encoder-sample-network
                                          c
                                          (alignment c)
                                          (float-array (take 5 (repeat (float -0.5))))))
          {:keys [w-delta wr-delta
                  update-gate-w-delta update-gate-wr-delta
                  reset-gate-w-delta reset-gate-wr-delta
                  bias-delta update-gate-bias-delta reset-gate-bias-delta]} hd]
      (is (= (row-count (:w-delta   hd)) 5))
      (is (= (row-count (:wr-delta  hd)) 5))
      (is (= (row-count (:update-gate-w-delta   hd)) 5))
      (is (= (row-count (:update-gate-wr-delta  hd)) 5))
      (is (= (row-count (:reset-gate-w-delta  hd)) 5))
      (is (= (row-count (:reset-gate-wr-delta  hd)) 5))
      ;; bias and peephole
      (is (= (ecount (remove zero? (:bias-delta           hd))) 5))
      (is (= (ecount (remove zero? (:update-gate-bias-delta      hd))) 5))
      (is (= (ecount (remove zero? (:reset-gate-bias-delta     hd))) 5))
      (is (= (mapv seq w-delta)
             (repeat 5 [-0.18750261664536555,0.0,0.0])))
      (is (= (mapv seq update-gate-w-delta)
             (repeat 5 [0.576896794654228,0.0,0.0])))
      (is (= (mapv seq reset-gate-w-delta)
             (repeat 5 [0.00186769961455633,0.0,0.0])))
      (is (= (mapv seq wr-delta)
             (repeat 5 (repeat 5 0.01902984655188307))))
      (is (= (mapv seq update-gate-wr-delta)
             (repeat 5 (repeat 5 -0.05619655737464192))))
      (is (= (mapv seq reset-gate-wr-delta)
             (repeat 5 (repeat 5 -3.8670021187223565E-4 ))))
      (is (= (seq bias-delta)
             (repeat 5 -0.18750261664536555)))
      (is (= (seq update-gate-bias-delta)
             (repeat 5 0.576896794654228)))
      (is (= (seq reset-gate-bias-delta)
             (repeat 5 0.00186769961455633)))))
  (testing "encoder-decoder-bptt"
    (let [{:keys [loss param-loss]} (encoder-decoder-bptt sample-encoder-decoder
                                                          (encoder-decoder-forward sample-encoder-decoder
                                                                                   (map #(array :vectorz %) [[2 0 0] [0 -1 1]])
                                                                                   (map #(array :vectorz %) [[-1 1 -1] [2 -1 1]])
                                                                                   [#{"prediction1" "prediction2"} #{"prediction2" "prediction3"}])
                                                          [{:pos ["prediction1"] :neg ["prediction2"]} {:pos ["prediction2"] :neg ["prediction3"]}])
          {:keys [encoder-param-delta decoder-param-delta]} param-loss
          {:keys [w-delta wr-delta
                  update-gate-w-delta update-gate-wr-delta
                  reset-gate-w-delta reset-gate-wr-delta
                  bias-delta update-gate-bias-delta reset-gate-bias-delta]} (:hidden-delta encoder-param-delta)]
      (is (not (nil? encoder-param-delta)))
      (is (not (nil? decoder-param-delta)))

      (is (= (mapv seq w-delta)
             (repeat 5 [0.07181348871931156 -0.010636581664491896 0.010636581664491896])))
      (is (= (mapv seq update-gate-w-delta)
             (repeat 5 [0.3689653260145865 -0.17589418747006083 0.17589418747006083])))
      (is (= (mapv seq reset-gate-w-delta)
             (repeat 5 [0.0 2.0484383851670073E-4 -2.0484383851670073E-4])))
      (is (= (mapv seq wr-delta)
             (repeat 5 (repeat 5 -0.0021897354557768763))))
      (is (= (mapv seq update-gate-wr-delta)
             (repeat 5 (repeat 5 -0.036211045138123885))))
      (is (= (mapv seq reset-gate-wr-delta)
             (repeat 5 (repeat 5 4.2170861865787185E-5))))
      (is (= (seq bias-delta)
             (repeat 5 0.04654332602414768)))
      (is (= (seq update-gate-bias-delta)
             (repeat 5 0.36037685047735407)))
      (is (= (seq reset-gate-bias-delta)
             (repeat 5 -2.0484383851670073E-4)))))
  )
