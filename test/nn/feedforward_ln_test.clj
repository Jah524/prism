(ns nn.feedforward-ln-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.core.matrix :refer [mget array matrix ecount]]
            [matrix.default :refer [default-matrix-kit]]
            [prism.nn.feedforward-ln :refer :all]))


(def sample-model2 ;3->2->3
  {:matrix-kit default-matrix-kit
   :input-type  :dense
   :output-type :prediction
   :input-size 3
   :hidden-size 2
   :hidden {:w (matrix [[0.1 0.1 0.1] [0.2 0.2 0.2]])
            :bias (array [1 1])
            :activation :sigmoid}
   :output {"prediction1" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction2" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction3" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}}})


(def sample-model2:sparse ;3->2->3
  {:matrix-kit default-matrix-kit
   :input-type  :sparse
   :output-type :prediction
   :input-size 3
   :hidden-size 2
   :hidden {:sparses {"natural" (array [0.1 0.2]) "language" (array [0.1 0.2]) "processing" (array [0.1 0.2])}
            :bias (array [1 1])
            :activation :sigmoid}
   :output {"prediction1" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction2" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}
            "prediction3" {:w (array (take 2 (repeat 1)))
                           :bias (array [1])}}})

(deftest feedforward-ln-test
  (let [model (assoc sample-model2 :hidden (assoc (:hidden sample-model2) :gain (array [1 4])))]
    (testing "network-output with dense input"
      (let [{:keys [hidden output]} (:activation (network-output model (float-array [0 1 0]) #{"prediction1" "prediction3"}))]
        (is (= (mapv double hidden)
               (mapv double [0.5727043151855469 0.9787189364433289])))
        (is (= output
               {"prediction1" (double 2.5514232516288757), "prediction3" (double 2.5514232516288757)})))))
  (let [model (assoc sample-model2:sparse :hidden (assoc (:hidden sample-model2:sparse) :gain (array [1 4])))]
    (testing "back-propagation with sparse vector"
      (let [{:keys [param-loss loss]} (back-propagation model
                                                        (network-output model {"language" 1}  #{"prediction1" "prediction2" "prediction3"})
                                                        {"prediction1" 2 "prediction2" 1 "prediction3" 2})
            {:keys [output-delta hidden-delta]} param-loss]
        (is (= (vec (get (:sparses-delta hidden-delta) "language"))
               (map double [-1.5146324533317927 1.5146324533317939])))
        (is (= (vec (:gain-delta hidden-delta))
               (map double [0.45929214783234534 -0.03909141429149655])))
        (is (= (vec (:bias-delta hidden-delta))
               (map double [-0.649537184555971 -0.05528360826337986]))))))
  (testing "update-model!"
    (let [model (assoc sample-model2:sparse :hidden (assoc (:hidden sample-model2:sparse) :gain (array [1 4])))
          {:keys [param-loss loss]} (back-propagation model
                                                      (network-output model {"language" 1}  #{"prediction1" "prediction2" "prediction3"})
                                                      {"prediction1" 2 "prediction2" 1 "prediction3" 2})
          {:keys [hidden output]} (update-model! model param-loss 0.1)]
      (is (= (mapv double (get (:sparses hidden) "natural")) (map double [0.1 0.2])));not changed
      (is (= (mapv double (get (:sparses hidden) "language")) (map double [-0.05146324533317928 0.3514632453331794])))
      (is (= (mapv double (get (:sparses hidden) "processing")) (map double [0.1 0.2])));not changed
      (is (= (mapv double (:bias hidden)) (map double [0.935046281544403 0.994471639173662])))
      (is (= (mapv double (:gain hidden)) (map double [1.0459292147832346 3.9960908585708506])))))
  ;;
  (testing "init-model"
    (is (= (-> (init-model {:input-items #{"natural" "language" "processing"}
                            :input-size nil
                            :hidden-size 3
                            :output-type :binary-classification
                            :output-items #{"prediction"}
                            :activation :sigmoid})
               :hidden
               :gain
               ecount)
           3))))
