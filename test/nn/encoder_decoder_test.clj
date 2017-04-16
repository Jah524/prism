(ns nn.encoder-decoder-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test   :refer :all]
    [prism.nn.lstm  :refer [lstm-activation sequential-output]]
    [nn.lstm-test   :refer [sample-w-network]]
    [prism.nn.encoder-decoder :refer :all]))

(def decoder-sample-network
  "assumed 5 encoder connections"
  (let [h (:hidden sample-w-network)]
    (assoc sample-w-network
      :hidden
      (assoc h
        :block-we
        (float-array (take 50 (repeat 0.02)))
        :input-gate-we
        (float-array (take 50 (repeat 0.02)))
        :forget-gate-we
        (float-array (take 50 (repeat 0.02)))
        :output-gate-we
        (float-array (take 50 (repeat 0.02)))))))

(deftest encoder-decoder-test
  (testing "init-encoder-decoder-model"
    (let [{:keys [encoder decoder]} (init-encoder-decoder-model {:input-items  nil
                                                                 :output-items #{"A" "B" "C"}
                                                                 :input-type :dense
                                                                 :input-size 3
                                                                 :encoder-hidden-size 10
                                                                 :decoder-hidden-size 20
                                                                 :output-type :binary-classification})
          {eh :hidden} encoder
          {dh :hidden} decoder]
      ;; encoder
      (is (= 30  (count (remove zero? (:block-w eh)))))
      (is (= 100 (count (remove zero? (:block-wr eh)))))
      (is (= 30  (count (remove zero? (:input-gate-w eh)))))
      (is (= 100 (count (remove zero? (:input-gate-wr eh)))))
      (is (= 30  (count (remove zero? (:forget-gate-w eh)))))
      (is (= 100 (count (remove zero? (:forget-gate-wr eh)))))
      (is (= 30  (count (remove zero? (:output-gate-w eh)))))
      (is (= 100 (count (remove zero? (:output-gate-wr eh)))))
      (is (= 10  (count (remove zero? (:block-bias eh)))))
      (is (= 10  (count (remove zero? (:input-gate-bias eh)))))
      (is (= 10  (count (remove zero? (:forget-gate-bias eh)))))
      (is (= 10  (count (remove zero? (:output-gate-bias eh)))))
      (is (= 10  (count (remove zero? (:input-gate-peephole eh)))))
      (is (= 10  (count (remove zero? (:forget-gate-peephole eh)))))
      (is (= 10  (count (remove zero? (:output-gate-peephole eh)))))
      ;decoder
      ;; encoder connection
      (is (= 200 (count (remove zero? (:block-we dh)))))
      (is (= 200 (count (remove zero? (:input-gate-we dh)))))
      (is (= 200 (count (remove zero? (:forget-gate-we dh)))))
      (is (= 200 (count (remove zero? (:output-gate-we dh)))))
      ;; same as standard lstm
      (is (= 60  (count (remove zero? (:block-w dh)))))
      (is (= 400 (count (remove zero? (:block-wr dh)))))
      (is (= 60  (count (remove zero? (:input-gate-w dh)))))
      (is (= 400 (count (remove zero? (:input-gate-wr dh)))))
      (is (= 60  (count (remove zero? (:forget-gate-w dh)))))
      (is (= 400 (count (remove zero? (:forget-gate-wr dh)))))
      (is (= 60  (count (remove zero? (:output-gate-w dh)))))
      (is (= 400 (count (remove zero? (:output-gate-wr dh)))))
      (is (= 20  (count (remove zero? (:block-bias dh)))))
      (is (= 20  (count (remove zero? (:input-gate-bias dh)))))
      (is (= 20  (count (remove zero? (:forget-gate-bias dh)))))
      (is (= 20  (count (remove zero? (:output-gate-bias dh)))))
      (is (= 20  (count (remove zero? (:input-gate-peephole dh)))))
      (is (= 20  (count (remove zero? (:forget-gate-peephole dh)))))
      (is (= 20  (count (remove zero? (:output-gate-peephole dh)))))))

  ;;       input-type input-items input-size output-type output-items
  ;;                                 encoder-hidden-size decoder-hidden-size)

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
      (is (= (vec a1)
             (vec a2)
             (take 10 (repeat (float 0.787542)))))
      (is (= (vec (:lstm s1))
             (vec (:lstm s2))
             (take 10 (repeat (float 0.787542)))))
      (is (= (vec (:block s1))
             (vec (:block s2))
             (take 10 (repeat (float 1.5999999)))))
      (is (= (vec (:input-gate s1))
             (vec (:input-gate s2))
             (take 10 (repeat (float 1.3999999)))))
      (is (= (vec (:forget-gate s1))
             (vec (:forget-gate s2))
             (take 10 (repeat (float 1.3999999)))))
      (is (= (vec (:output-gate s1))
             (vec (:output-gate s2))
             (take 10 (repeat (float 1.3999999)))))
      (is (= (vec (:cell-state  s1))
             (vec (:cell-state  s2))
             (take 10 (repeat (float 2.3437154))))))
    (let [{a :activation s :state} (decoder-lstm-activation decoder-sample-network
                                                            (float-array (take 3 (repeat 2)))
                                                            (float-array (take 10 (repeat 2)))
                                                            (float-array (take 5 (repeat 3)))
                                                            (float-array (take 10 (repeat 2))))]
      (is (= (vec a) (take 10 (repeat (float 0.83420765)))))
      (is (= (vec (:lstm s)) (take 10 (repeat (float 0.83420765)))))
      (is (= (vec (:block s)) (take 10 (repeat (float 1.8999999)))))
      (is (= (vec (:input-gate s)) (take 10 (repeat (float 1.6999998)))))
      (is (= (vec (:forget-gate s)) (take 10 (repeat (float 1.6999998)))))
      (is (= (vec (:output-gate s)) (take 10 (repeat (float 1.6999998)))))))
  (testing "encoder-forward"
    (let [result (encoder-forward sample-w-network (map float-array [[1 0 0] [1 0 0]]) [:skip #{"prediction1" "prediction2" "prediction3"}])
          {:keys [block input-gate forget-gate output-gate]} (:state (last result))]
      (is (= 2 (count result)))
      (is (= (vec block) (take 10 (repeat (float -0.9590061)))))
      (is (= (vec input-gate) (take 10 (repeat (float -0.93830144)))))
      (is (= (vec forget-gate) (take 10 (repeat (float -0.93830144)))))
      (is (= (vec output-gate) (take 10 (repeat (float -0.93830144)))))))


  (testing "decorder-forward"
    (let [it1 (vec (:output (:activation (last (decoder-forward decoder-sample-network
                                                                (map float-array [[2 0 0] [1 0 0]])
                                                                (float-array 5)
                                                                [:skip #{"prediction1" "prediction2" "prediction3"}])))))
          it2 (vec (:output (:activation (last (sequential-output sample-w-network
                                                                  (map float-array [[2 0 0] [1 0 0]])
                                                                  [:skip #{"prediction1" "prediction2" "prediction3"}])))))]
      (is (= it1 it2))
      (is (= (->> it1 (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.254815) "prediction1" (float 0.254815) "prediction3" (float 0.254815)})))
    (let [result (decoder-forward decoder-sample-network
                                  (map float-array [[2 0 0] [1 0 0]])
                                  (float-array 5)
                                  [:skip #{"prediction1" "prediction2" "prediction3"}])
          {:keys [activation state]} (last result)
          {:keys [hidden output]} activation]
      (is (= (vec hidden) (take 10 (repeat (float -0.07309456)))))
      (is (= (->> output vec (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.254815) "prediction1" (float 0.254815) "prediction3" (float 0.254815)}))))




  )
