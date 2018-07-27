(ns optimizer-test
  (:require
    [clojure.test :refer :all]
    [clojure.core.matrix :refer [array set-current-implementation]]
    [prism.optimizer :refer :all]))


(set-current-implementation :vectorz)

(deftest optimizer-test
  (testing "clip!"
    ; should be clipped
    (is (= (map float (clip! 1 (array [0.01 0.88 -0.96 -0.12 0.43])))
           (map float [0.007263561 0.63919336 -0.6973018 -0.087162726 0.3123331])))
    ; should not be clipped
    (is (= (map float (clip! 1 (array [0.04 0.02 -0.02 -0.05 0.01])))
           (map float [0.04 0.02 -0.02 -0.05 0.01]))))
  (testing "sgd!"
    (is (= (map float (sgd! 0.05 (array (range 10)) (array (repeat 10 1))))
           (map float [0.05 1.05 2.05 3.05 4.05 5.05 6.05 7.05 8.05 9.05]))))
  (testing "update-param!"
    (is (= (map float (update-param! :sgd 0.05 (array (range 10)) (array (repeat 10 1))))
           (map float [0.05 1.05 2.05 3.05 4.05 5.05 6.05 7.05 8.05 9.05])))))

