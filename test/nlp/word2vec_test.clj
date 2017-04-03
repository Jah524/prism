(ns nlp.word2vec-test
  (:require [clojure.test :refer :all]
            [clojure.string :refer [split]]
            [clojure.pprint :refer [pprint]]
            [sai-ai.nlp.word2vec :refer :all]))

(deftest word2ec-test
  (testing "subsampling"
    (is (= (subsampling "A" 0.001 1.0e-3)
           "A")))
  (testing "sg-windows"
    (is (= (count (sg-windows ["A" "B" "C" "D" "E" "F" "G"] 3))
           7)))
  (testing "skip-gram-training-pair"
    (let [coll (skip-gram-training-pair {"A" 1 "B" 20 "C" 30 "common word" 10000 "D" 10 "E" 10} 10071 ["A" "B" "common word" "C" "D" "E"]
                                        {:window-size 2})]
      (is (not (true? (->> coll flatten (some #(or (= % "<bos>") (= % "<eos>")))))))
      (is (= 1 (count (ffirst coll))))
      (is (not (zero? (count (second (first coll))))))))
  (testing "uniform->cum-uniform"
    (= (uniform->cum-uniform {"A" 2 "B" 3 "C" 1 "D" 7 "E" 2 "F" 15})
       [["F" 15.0] ["D" 22.0] ["B" 25.0] ["A" 27.0] ["E" 29.0] ["C" 30.0]]))
  (testing "uniform-sampling"
    (is (= (uniform-sampling (into-array [["D" 7] ["B" 10] ["A" 12] ["C" 13]]) [6])
           ["D"]))
    (is (= (uniform-sampling (into-array [["D" 7] ["B" 10] ["A" 12] ["C" 13]]) [7])
           ["B"]))
    (is (= (uniform-sampling (into-array [["D" 7] ["B" 10] ["A" 12] ["C" 13]]) [12])
           ["C"]))
    (is (= (uniform-sampling (into-array [["D" 7] ["B" 10] ["A" 12] ["C" 13]]) [12 6 2 7])
           ["D" "D" "B" "C"])))
  (testing "get-negatives"
    (is (= 20 (count (get-negatives (into-array [["D" 7] ["B" 10] ["A" 12] ["C" 13]]) 20)))))
  (testing "init-w2v-model"
    (let [model (init-w2v-model {"A" 1 "B" 1 "C" 1 "<unk>" 0} 2)]
      (is (= (count (:w (:embedding model))) 4))
      (is (= (count (:w (:output model)))    4))
      (is (= (count (:bias (:embedding model))) 2))
      (is (= (count (:bias (:output model)))    4))))
  (let [tiny (init-w2v-model {"A" 1 "B" 1  "C" 1 "D" 1 "E" 1 "F" 1 "G" 1 "<unk>" 0} 10)]
    (testing "hidden-activation"
      (->> (map #(aget (hidden-activation tiny "B") %) (range 10))
           (every? #(not (zero? %)))))
    (testing "negative-sampling"
      (let [coll (negative-sampling tiny (float-array (take 10 (repeat 1))) ["A" "C"] ["A" "B" "C"])]
        (is (= (->> coll (map first)) ["A" "C" "B"]))
        (let [vs (->> coll (map second))]
          (is (> (nth vs 0) 0))
          (is (> (nth vs 1) 0))
          (is (< (nth vs 2) 0)))))
    (testing "some-hot-bp"
      (let [coll (map #(aget ^floats (some-hot-bp tiny {"A" 0.5108695 "C" 0.498649 "D" -0.50098985 "<unk>" -0.5097836}) %) (range 10))]
        (is (= (count coll) 10))
        (is (not (zero? (count (remove #(zero? %) coll)))))))
    (testing "output-param-delta"
      (let [coll (map #(aget ^floats (second (first (output-param-delta tiny
                                                                        {"A" 0.5108695 "C" 0.498649 "D" -0.50098985 "<unk>" -0.5097836}
                                                                        (float-array (range 1 11))))) %)
                      (range 10))]
        (is (= 10 (count coll)))
        (is (not (zero? (count (remove #(zero? %) coll))))))))
  (testing "update-output-params!"
    (let [minimum-model {:hidden-size 10 :output {:bias {"A" (float-array [10])} :w {"A" (float-array (range 10))}}}
          word-w-delta-list {"A" (float-array (range 10))}
          word-bias-delta-list {"A" 1}
          result (update-output-params! minimum-model word-w-delta-list word-bias-delta-list 0.01)]
      (is (= [(float 10.01)]
             (vec (get (get-in result [:output :bias]) "A"))))
      (is (= (map float [0.0, 1.01, 2.02, 3.03, 4.04, 5.05, 6.06, 7.07, 8.08, 9.09])
             (vec (get (get-in result [:output :w]) "A"))))))
  (testing "update-embedding-bias!"
    (let [minimum-model {:hidden-size 10 :embedding {:bias (float-array (range 10))}}
          result (update-embedding-bias! minimum-model (float-array (range 10 20)) 0.01)]
      (is (= (map float [0.1, 1.11, 2.12, 3.13, 4.14, 5.15, 6.16, 7.17, 8.18, 9.19])
             (vec (get-in result [:embedding :bias]))))))
  (testing "update-embedding!"
    (let [minimum-model {:hidden-size 10 :embedding {:w {"A" (float-array (range 10))}}}
          result (update-embedding! minimum-model ["A" (float-array (range 10))] 0.01)]
      (is (= (map float [0.0, 1.01, 2.02, 3.03, 4.04, 5.05, 6.06, 7.07, 8.08, 9.09])
             (vec (get (get-in result [:embedding :w]) "A"))))))
      )
