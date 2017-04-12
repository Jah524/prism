(ns nlp.word2vec-test
  (:require [clojure.test :refer :all]
            [clojure.string :refer [split]]
            [clojure.pprint :refer [pprint]]
            [prism.nlp.word2vec :refer :all]))

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
  (testing "init-w2v-model"
    (let [{:keys [hidden wl input-type output-type]} (init-w2v-model {"A" 12 "B" 345 "C" 42} 10)
          {:keys [unit-num]} hidden]
      (is (= unit-num 10))
      (is (= input-type :sparse))
      (is (= output-type :binary-classification))
      (is (= (count (keys wl)) 3)))))


