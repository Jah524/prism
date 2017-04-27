(ns matrix.default-test
  (:require [clojure.test :refer :all]
            [matrix.default :refer :all]))


(def x (float-array (range 5)))
(def y (float-array (range 0 50 10)))

(deftest matrix-test
  (testing "sum"
    (is (= (vec (sum x y))
           [0.0 11.0 22.0 33.0 44.0])))
  (testing "minus"
    (is (= (vec (minus x y))
           [0.0 -9.0 -18.0 -27.0 -36.0])))
  (testing "scal"
    (is (= (vec (scal 10 x))
           [0.0 10.0 20.0 30.0 40.0])))
  (testing "times"
    (is (= (vec (times x y))
           [0.0 10.0 40.0 90.0 160.0])))
  (testing "dot"
    (is (= (dot x y)
           (float 300))))
  (testing "outer"
    (is (= (map vec (outer x y))
           [[0.0 0.0 0.0 0.0 0.0]
            [0.0 10.0 20.0 30.0 40.0]
            [0.0 20.0 40.0 60.0 80.0]
            [0.0 30.0 60.0 90.0 120.0]
            [0.0 40.0 80.0 120.0 160.0]])))
  (testing "transpose"
    (is (= (map vec  (transpose (object-array (map float-array (partition 5 (range 20))))))
           (map #(map float %) [[0 5 10 15] [1 6 11 16] [2 7 12 17] [3 8 13 18] [4 9 14 19]]))))
  (testing "gemv"
    (let [mat (object-array (map float-array [[1 2 3], [4 5 6], [7 8 9], [10 11 12]]))
          v   (float-array [1 1 1])
          result   [6.0 15.0 24.0, 33.0]]
      (is (= (vec (gemv mat v)) result))))
  )
