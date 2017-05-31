(ns nlp.word2vec-test
  (:require
    [clojure.test :refer :all]
    [clojure.string :refer [split]]
    [clojure.pprint :refer [pprint]]
    [clojure.core.matrix :refer [clone array]]
    [prism.util :refer [make-wc similarity]]
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
    (let [{:keys [hidden hidden-size wc input-type output-type]} (init-w2v-model {"A" 12 "B" 345 "C" 42} 10)]
      (is (= hidden-size 10))
      (is (= output-type :binary-classification))
      (is (= (count (keys wc)) 3))))
  (let [target-tok "test/nlp/example.tok"
        wc (make-wc target-tok {:interval-ms 300 :workers 1 :min-count 1})
        w2v (init-w2v-model wc 10)
        A (clone (get-in w2v [:hidden :w "A"]))
        D (clone (get-in w2v [:hidden :w "D"]))
        d (clone (get-in w2v [:hidden :w "d"]))
        X (clone (get-in w2v [:hidden :w "X"]))]
    (testing "train-word2vec!"
      (train-word2vec! w2v target-tok {:initial-learning-rate 0.01 :min-learning-rate 0.005 :nagetaive 10 :workers 1 :interval-ms 500 :sample 0.1})
      (is (not= (vec D)
                (vec (get-in w2v [:hidden :sparses "D"])))))
    (testing "most-sim-in-model"
      (let [result (most-sim-in-model w2v "D" 3)
            sims (->> result (map #(get % :x)) set)]
        (is (= (:x (first result)) "D")))))

  (testing "most-sim"
    (let [em {"A" (array (range 10))
              "B" (array (repeat 10 0.3))
              "C" (array (range 10 20))
              "X" (array (range 10))}]
      (is (= (most-sim em "X" ["A" "B" "C" ] 3 false)
             [{:x "A", :sim (float 1)} {:x "C", :sim (float 0.9314063)} {:x "B", :sim (float 0.8429272081702948)}])))))
