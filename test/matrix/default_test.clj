(ns matrix.default-test
  (:require [clojure.test :refer :all]
            [matrix.default :refer :all]))


(deftest matrix-test
  (testing "transpose"
    (let [it (float-array (range 25))
          result  (transpose 5 it)]
      (is (= (vec result)
             (map float [0 5 10 15 20 1 6 11 16 21 2 7 12 17 22 3 8 13 18 23 4 9 14 19 24])))))
  (testing "default gemv"
    (let [mat (float-array [1 2 3, 4 5 6, 7 8 9, 10 11 12])
          v   (float-array [1 1 1])
          result   [6.0 15.0 24.0, 33.0]]
      (is (= (vec (gemv mat v)) result))))
  )
