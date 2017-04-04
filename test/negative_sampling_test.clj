(ns negative-sampling-test
  (:require [clojure.test :refer :all]
            [sai-ai.negative-sampling :refer :all]))

(deftest negative-sampling-test
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
    (is (= 20 (count (get-negatives (into-array [["D" 7] ["B" 10] ["A" 12] ["C" 13]]) 20))))))
