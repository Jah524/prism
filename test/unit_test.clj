(ns unit-test
  (:require [clojure.test :refer :all]
            [matrix.default :refer [default-matrix-kit]]
            [prism.unit :refer :all]))

(deftest unit-test
  (testing "softmax"
    (is (= (vec (softmax default-matrix-kit (float-array (range 10))))
           (map float [7.8013414E-5 2.1206244E-4 5.764455E-4 0.0015669414 0.004259388 0.011578218 0.031472858 0.0855521 0.23255472 0.6321493]))))
  )
