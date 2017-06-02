(ns nn.rnn.gru-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [clojure.core.matrix :refer [mget array matrix ecount row-count]]
    [prism.nn.feedforward :as ff]
    [prism.nn.rnn.gru :refer :all]))

(def sample-w-network
  {:input-size 3
   :hidden-size 10
   :output-type :binary-classification
   :hidden {:w (matrix (partition 3 (take 30 (repeat 0.1))))
            :wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :bias (array (take 10 (repeat -1)))
            :update-gate-w  (matrix (partition 3 (take 30 (repeat 0.1))))
            :update-gate-wr  (matrix (partition 10 (take 100 (repeat 0.1))))
            :update-gate-bias (array (take 10 (repeat -1)))
            :reset-gate-w (matrix (partition 3 (take 30 (repeat 0.1))))
            :reset-gate-wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :reset-gate-bias (array (take 10 (repeat -1)))}
   :output {"prediction1" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}
            "prediction2" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}
            "prediction3" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}}})

(def sample-w-network-sparse
  {:output-type :binary-classification
   :hidden-size 10
   :hidden {:sparses {"natural" {:w (array (take 10 (repeat 0.1)))
                                 :update-gate-w  (array (take 10 (repeat 0.1)))
                                 :reset-gate-w (array (take 10 (repeat 0.1)))}
                      "language" {:w (array (take 10 (repeat 0.1)))
                                  :update-gate-w  (array (take 10 (repeat 0.1)))
                                  :reset-gate-w (array (take 10 (repeat 0.1)))}
                      "processing" {:w (array (take 10 (repeat 0.1)))
                                    :update-gate-w  (array (take 10 (repeat 0.1)))
                                    :reset-gate-w (array (take 10 (repeat 0.1)))}}
            :wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :bias (array (take 10 (repeat -1)))
            :update-gate-wr  (matrix (partition 10 (take 100 (repeat 0.1))))
            :update-gate-bias (array (take 10 (repeat -1)))
            :reset-gate-wr (matrix (partition 10 (take 100 (repeat 0.1))))
            :reset-gate-bias (array (take 10 (repeat -1)))}
   :output {"prediction1" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}
            "prediction2" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}
            "prediction3" {:w (array (take 10 (repeat 0.1)))
                           :bias (array [-1])}}})

(def sample-w-network-prediction
  {:output-type :prediction
   :hidden-size 10
   :hidden {:sparses {"natural" {:w (array (take 10 (repeat 0.1)))
                                 :update-gate-w  (array (take 10 (repeat 0.1)))
                                 :reset-gate-w (array (take 10 (repeat 0.1)))}
                      "language" {:w (array (take 10 (repeat 0.1)))
                                  :update-gate-w  (array (take 10 (repeat 0.1)))
                                  :reset-gate-w (array (take 10 (repeat 0.1)))}
                      "processing" {:w (array (take 10 (repeat 0.1)))
                                    :update-gate-w  (array (take 10 (repeat 0.1)))
                                    :reset-gate-w (array (take 10 (repeat 0.1)))}}
            :wr (matrix (partition 10 (take 100 (take 100 (repeat 0.1)))))
            :bias (array (take 10 (repeat -1)))
            :update-gate-wr  (matrix (partition 10 (take 100 (take 100 (repeat 0.1)))))
            :update-gate-bias (array (take 10 (repeat -1)))
            :reset-gate-wr (matrix (partition 10 (take 100 (take 100 (repeat 0.1)))))
            :reset-gate-bias (array (take 10 (repeat -1)))}
   :output {"prediction" {:w (array (take 10 (repeat 0.1)))
                          :bias  (array [-1])}}})



(deftest gru-test
  (testing "partial-state-sparse with set"
    (let [{:keys [hidden]} sample-w-network-sparse]
      (is (= (->> (partial-state-sparse #{"language"} (:sparses hidden)) (map vec))
             (take 3 (repeat (take 10 (repeat (double 0.1)))))))))
  (testing "partial-state-sparse with map"
    (let [{:keys [hidden]} sample-w-network-sparse]
      (is (= (->> (partial-state-sparse {"language" 1} (:sparses hidden)) (map vec))
             (->> (partial-state-sparse #{"language"} (:sparses hidden)) (map vec))
             (take 3 (repeat (take 10 (repeat (double 0.1)))))))
      (is (not= (->> (partial-state-sparse {"language" 2} (:sparses hidden)) (map vec))
                (->> (partial-state-sparse #{"language"}  (:sparses hidden)) (map vec))))))
  (testing "gru-activation"
    (let [result (gru-activation sample-w-network
                                 (float-array (take 3 (repeat 2)))
                                 (float-array (take 10 (repeat 2))))
          a (:activation result)
          s (:state result)]
      (is (= (mapv float (:gru a)) (take 10 (repeat (float 1.044987)))))
      (is (= (mapv float (:h a)) (take 10 (repeat (float 0.8521732)))))
      (is (= (mapv float (:update-gate a)) (take 10 (repeat (float 0.8320184)))))
      (is (= (mapv float (:reset-gate a)) (take 10 (repeat (float 0.8320184)))))

      (is (= (mapv float (:h-state s)) (take 10 (repeat (float 1.2640368)))))
      (is (= (mapv float (:update-gate  s)) (take 10 (repeat (float 1.6)))))
      (is (= (mapv float (:reset-gate s)) (take 10 (repeat (float 1.6)))))))

  (testing "gru-activation with sparse"
    (let [result (gru-activation sample-w-network-sparse
                                 {"natural" (float 2)}
                                 (float-array (take 10 (repeat 2))))
          a (:activation result)
          s (:state result)
          result2 (gru-activation sample-w-network
                                  (float-array [2 0 0])
                                  (float-array (take 10 (repeat 2))))
          a2 (:activation result2)
          s2 (:state result2)]
      (is (= (vec (:gru a))
             (vec (:gru a2))
             (take 10 (repeat (double 0.9450915798816111)))))
      (is (= (vec (:h a))
             (vec (:h a2))
             (take 10 (repeat (double 0.6273592696445066)))))
      (is (= (mapv float (:update-gate  s))
             (mapv float (:update-gate  s2))
             (take 10 (repeat (float 1.2)))))
      (is (= (mapv float (:reset-gate s))
             (mapv float (:reset-gate s2))
             (take 10 (repeat (float 1.2)))))))
  (testing "gru-fixed-time with dense input"
    (let [result (gru-fixed-time sample-w-network
                                 (float-array [1 0 -10]); as x-input
                                 #{"prediction1" "prediction2" "prediction3"}
                                 (float-array (take 10 (repeat 0))))
          f  (get-in result [:state :input])
          ss (get-in result [:state :hidden])]
      (is (= (vec f) (map float [1 0 -10])))
      (is (= (mapv float (:h-state ss)) (take 10 (repeat (float -1.9)))))
      (is (= (mapv float (:update-gate  ss)) (take 10 (repeat (float -1.9)))))
      (is (= (mapv float (:reset-gate ss)) (take 10 (repeat (float -1.9)))))
      (is (= (mapv float (:gru (:hidden (:activation result))))
             (take 10 (repeat (float -0.1244146)))))
      (is (= (->> result :activation :hidden :gru (mapv float))
             (take 10 (repeat (float -0.12441459680575118)))))
      (is (= (->> result :activation :output (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.24519333)
              "prediction1" (float 0.24519333)
              "prediction3" (float 0.24519333)}))))
  (testing "gru-model-output with sparse input"
    (let [result (gru-fixed-time sample-w-network-sparse
                                 {"language" (float 1)}
                                 #{"prediction1" "prediction2" "prediction3"}
                                 (float-array (take 10 (repeat 0))))
          f  (->> result :state :input)
          ss (->> result :state :hidden)
          result2 (gru-fixed-time sample-w-network
                                  (float-array [0 1 0])
                                  #{"prediction1" "prediction2" "prediction3"}
                                  (float-array (take 10 (repeat 0))))
          f2  (->> result2 :state :input)
          ss2 (->> result2 :state :hidden)]
      (is (= (vec f) [["language" (float 1)]]))
      (is (= (mapv float (:h-state ss))
             (mapv float (:h-state ss2))
             (take 10 (repeat (float -0.9)))))
      (is (= (mapv float (:update-gate  ss))
             (mapv float (:update-gate ss2))
             (take 10 (repeat (float -0.8999999985098839)))))
      (is (= (mapv float (:reset-gate ss))
             (mapv float (:reset-gate ss2))
             (take 10 (repeat (float -0.8999999985098839)))))))

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
             {"prediction2" (float 0.2075839) "prediction1" (float 0.2075839) "prediction3" (float 0.2075839)}))
      (is (= (->> it2 (reduce (fn [acc [i x]] (assoc acc i (float x))) {}))
             {"prediction2" (float 0.2077148) "prediction1" (float 0.2077148) "prediction3" (float 0.2077148)}))))

  (testing "forward in sparse model"
    (let [result1 (forward sample-w-network-sparse
                           [{"language" (float 1)} {"processing" (float 1)}]
                           [:skip #{"prediction1" "prediction2" "prediction3"}])
          result2 (forward sample-w-network
                           [(float-array [0 1 0]) (float-array [0 0 1])]
                           [:skip #{"prediction1" "prediction2" "prediction3"}])
          out1 (mapv float (:gru (:hidden (:activation (first result1)))))
          out2 (mapv float (:gru (:hidden (:activation (first result2)))))
          out3 (mapv float (:gru (:hidden (:activation (second result1)))))
          out4 (mapv float (:gru (:hidden (:activation (second result2)))))]
      (is (= out1 out2 (take 10 (repeat (float -0.20704626)))))
      (is (= out3 out4 (take 10 (repeat (float -0.33955097)))))))

  (testing "gru-delta"
    (let [result (gru-delta sample-w-network
                            (array :vectorz (repeat 10 100))
                            {:update-gate (array :vectorz (repeat 10 1.1))
                             :reset-gate (array :vectorz (repeat 10 1.2))
                             :h (array :vectorz (repeat 10 0.5))}
                            {:update-gate (array :vectorz (repeat 10 1.1))
                             :reset-gate (array :vectorz (repeat 10 1.2))
                             :h-state (array :vectorz (repeat 10 0.5))}
                            (array :vectorz (range 10)))]
      (is (= (mapv float (:update-gate-delta result))
             (map float [9.555864 -9.181125 -27.918112 -46.6551 -65.39209 -84.129074 -102.866066 -121.60306 -140.34004 -159.07703])))
      (is (= (mapv float (:reset-gate-delta result))
             (map float [0.0 15.389514 30.779028 46.168545 61.558056 76.94757 92.33709 107.7266 123.11611 138.50563])))
      (is (= (mapv float (:unit-delta result))
             (map float [86.50925 86.50925 86.50925 86.50925 86.50925 86.50925 86.50925 86.50925 86.50925 86.50925])))
      (is (= (mapv float (:hidden:t-1-delta result))
             (mapv float (repeat 10 88.30333))))))

  (testing "param-delta-sparse with set"
    (let [{:keys [w-delta update-gate-w-delta reset-gate-w-delta]}
          (-> (param-delta-sparse #{"processing"}
                                  (float-array (take 10 (repeat 1)))
                                  (float-array (take 10 (repeat 1)))
                                  (float-array (take 10 (repeat 1))))
              (get "processing"))]
      (is (= (vec w-delta)       (take 10 (repeat (float 1)))))
      (is (= (vec update-gate-w-delta)  (take 10 (repeat (float 1)))))
      (is (= (vec reset-gate-w-delta) (take 10 (repeat (float 1)))))))

  (testing "gru-param-delta"
    (let [it (forward sample-w-network (map float-array [[2 1 -1] [-2 0 2]]) [:skip #{"prediction1" "prediction2" "prediction3"}])
          result (gru-param-delta sample-w-network
                                  {:unit-delta       (float-array (take 10 (repeat 1)))
                                   :update-gate-delta (float-array (take 10 (repeat 1)))
                                   :reset-gate-delta (float-array (take 10 (repeat 1)))}
                                  (array :vectorz [2 1 -1])
                                  (:gru (:hidden (:activation (first it)))))]
      (is (= (map vec (:w-delta result))  (partition 3 (take 30 (flatten (repeat (map float [2.0 1.0 -1.0])))))))
      (is (= (map #(mapv float %) (:wr-delta result)) (partition 10 (take 100 (repeat (float -0.20586835))))))
      (is (= (map #(mapv float %) (:update-gate-w-delta result)) (partition 3 (take 30 (flatten (repeat (map float [2.0 1.0 -1.0])))))))
      (is (= (map #(mapv float %) (:update-gate-wr-delta result)) (partition 10 (take 100 (repeat (float -0.20586835))))))
      (is (= (map #(mapv float %) (:reset-gate-w-delta result)) (partition 3 (take 30 (flatten (repeat (map float [2.0 1.0 -1.0])))))))
      (is (= (map #(mapv float %) (:reset-gate-wr-delta result)) (partition 10 (take 100 (repeat (float -0.20586835))))))
      (is (= (vec (:bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:update-gate-bias-delta result)) (take 10 (repeat (float 1)))))
      (is (= (vec (:reset-gate-bias-delta result)) (take 10 (repeat (float 1)))))))
  (testing "gru-param-delta in sparse model"
    (let [it (forward sample-w-network-sparse [{"language" 1} {"processing" 1}] [:skip #{"prediction1" "prediction2" "prediction3"}])
          result (gru-param-delta sample-w-network-sparse
                                  {:unit-delta       (float-array (take 10 (repeat 1)))
                                   :update-gate-delta (float-array (take 10 (repeat 1)))
                                   :reset-gate-delta (float-array (take 10 (repeat 1)))}
                                  {"processing" 1}
                                  (:gru (:hidden (:activation (first it)))))
          it2 (forward sample-w-network [(array :vectorz [0 1 0]) (float-array [0 0 1])] [:skip #{"prediction1" "prediction2" "prediction3"}])
          result2 (gru-param-delta sample-w-network
                                   {:unit-delta       (float-array (take 10 (repeat 1)))
                                    :update-gate-delta (float-array (take 10 (repeat 1)))
                                    :reset-gate-delta (float-array (take 10 (repeat 1)))}
                                   (float-array [0 0 1])
                                   (:gru (:hidden (:activation (first it2)))))
          ws (get (:sparses-delta result) "processing")]
      (is (= (vec (:w-delta ws))
             (->> (vec (map #(nth % 2) (:w-delta result2))))))
      (is (= (vec (:update-gate-w-delta ws))
             (->> (vec (map #(nth % 2) (:update-gate-w-delta result2))))))
      (is (= (vec (:reset-gate-w-delta ws))
             (->> (vec (map #(nth % 2) (:reset-gate-w-delta result2))))))))

  (testing "bptt with sparse model"
    (let [{:keys [param-loss loss]} (bptt sample-w-network-sparse
                                          (forward sample-w-network-sparse
                                                   [{"language" (float 1)} {"processing" (float 1)}]
                                                   [:skip ["prediction1" "prediction3"]])
                                          [:skip {:pos ["prediction1"] :neg ["prediction3"]}])
          {hd :hidden-delta od :output-delta} param-loss
          it (:sparses-delta hd)]
      (is (= (count loss) 2))
      (is (= loss [{} {"prediction1" (double 0.7924160902111286) "prediction3" (double -0.20758390978887148)}]))
      (is (= (nil? (:w-delta (get it "natural")))))
      (is (= (row-count (:w-delta (get it "language"))) 10))
      (is (= (row-count (:w-delta (get it "processing"))) 10))))

  (testing "update-model! with dense model"
    (let [result (update-model! sample-w-network
                                (:param-loss (bptt sample-w-network
                                                   (forward sample-w-network
                                                            [(float-array [0 1 0]) (float-array [2 0 0])]
                                                            [["prediction1" "prediction3"] ["prediction2" "prediction3"]])
                                                   [{:pos ["prediction1"] :neg ["prediction3"]} {:pos ["prediction2"] :neg ["prediction3"]}]))
                                0.1)
          hd (:hidden result)]
      (is (= (row-count (:w hd)) 10))
      (is (= (row-count (:wr hd)) 10))
      (is (= (row-count (:update-gate-w hd)) 10))
      (is (= (row-count (:update-gate-wr hd)) 10))
      (is (= (row-count (:reset-gate-w hd)) 10))
      (is (= (row-count (:reset-gate-wr hd)) 10))

      (is (= (map #(map float %) (:w hd))
             (take 10 (repeat (map float [0.10162071 0.10406713 0.1])))))
      (is (= (map #(map float %) (:wr hd))
             (partition 10 (take 100 (repeat (float 0.09983222))))))
      (is (= (map #(map float %) (:update-gate-w hd))
             (take 10 (repeat (map float [0.13807967 0.11629634 0.1])))))
      (is (= (map #(map float %) (:update-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.09605788))))))
      (is (= (map #(map float %) (:reset-gate-w hd))
             (take 10 (repeat (map float [0.09993424 0.1 0.1])))))
      (is (= (map #(map float %) (:reset-gate-wr hd))
             (partition 10 (take 100 (repeat (float 0.10000681))))))
      (is (= (mapv float (:bias  hd)) (take 10 (repeat (float -0.9951225)))))
      (is (= (mapv float (:update-gate-bias hd)) (take 10 (repeat (float -0.9646638)))))
      (is (= (mapv float (:reset-gate-bias hd)) (take 10 (repeat (float -1.0000329)))))))

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
      (is (= (row-count (:w (get sparses "language"))) 10))
      (is (= (row-count (:update-gate-w (get sparses "language"))) 10))
      (is (= (row-count (:reset-gate-w (get sparses "language"))) 10))
      (is (= (row-count (:wr hd)) 10))
      (is (= (row-count (:update-gate-wr hd)) 10))
      (is (= (row-count (:reset-gate-wr hd)) 10))))

  (testing "init-model with dense input"
    (let [m (init-model {:input-items  nil
                         :output-items #{"A" "B" "C"}
                         :input-type :dense
                         :input-size 3
                         :hidden-size 10
                         :output-type :binary-classification})
          h (:hidden m)]
      (is (= (:rnn-type m) :gru))
      (is (not= :sparse (:input-type m)))
      (is (= 10 (row-count (:w h))))
      (is (= 10 (row-count (:wr h))))
      (is (= 10 (row-count (:update-gate-w h))))
      (is (= 10 (row-count (:update-gate-wr h))))
      (is (= 10 (row-count (:reset-gate-w h))))
      (is (= 10 (row-count (:reset-gate-wr h))))
      (is (= 10 (count (remove zero? (:bias h)))))
      (is (= 10 (count (remove zero? (:update-gate-bias h)))))
      (is (= 10 (count (remove zero? (:reset-gate-bias h)))))
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
      (is (= 10 (row-count (remove zero? (:w (get (:sparses h) "X"))))))
      (is (= 10 (row-count  (:wr h))))
      (is (= 10 (row-count (remove zero? (:update-gate-w (get (:sparses h) "Y"))))))
      (is (= 10 (row-count (:update-gate-wr h))))
      (is (= 10 (row-count (remove zero? (:reset-gate-w (get (:sparses h) "X"))))))
      (is (= 10 (row-count  (:reset-gate-wr h))))
      (is (= 10 (count (remove zero? (:bias h)))))
      (is (= 10 (count (remove zero? (:update-gate-bias h)))))
      (is (= 10 (count (remove zero? (:reset-gate-bias h)))))
      (let [{aw :w abias :bias} (get (:output m) "A")
            {bw :w bbias :bias} (get (:output m) "B")
            {cw :w cbias :bias} (get (:output m) "C")]
        (is (= 10 (ecount aw) (ecount bw) (ecount cw)))
        (is (= 1 (ecount abias) (ecount bbias) (ecount cbias))))))
  )
