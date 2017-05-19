(ns matrix.default-test
  (:require
    [clojure.test :refer :all]
    [clojure.core.matrix :refer [array matrix set-current-implementation]]
    [clojure.core.matrix.operators :as o]
    [matrix.default :refer :all]))

(set-current-implementation :vectorz)

(def x (array (range 5)))
(def y (array (range 0 50 10)))

(deftest matrix-test
  (let [{:keys [sum plus minus scal times dot outer transpose gemv clip! sigmoid transpose alter-vec rewrite-vector!]} default-matrix-kit]
    (testing "sum"
      (is (= (sum (array (range 10)))
             (float 45))))
    (testing "plus"
      (is (= (vec (plus x y))
             [0.0 11.0 22.0 33.0 44.0]))
      (is (= (plus x x x x)
             (o/* x 4))))
    (testing "minus"
      (is (= (vec (minus x y))
             [0.0 -9.0 -18.0 -27.0 -36.0]))
      (is (= (seq (minus x x x x))
             (seq (o/- (o/* x 2))))))
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
      (is (= (map vec  (transpose (matrix (partition 5 (range 20)))))
             (map #(map float %) [[0 5 10 15] [1 6 11 16] [2 7 12 17] [3 8 13 18] [4 9 14 19]]))))
    (testing "gemv"
      (let [mat (matrix [[1 2 3], [4 5 6], [7 8 9], [10 11 12]])
            v   (array [1 1 1])
            result   [6.0 15.0 24.0, 33.0]]
        (is (= (vec (gemv mat v)) result))))
    (testing "clip!"
      (is (= (map float (clip! 25 (array [1 -3 -30 30 24 -24.9])))
             (map float [1 -3 -25 25 24 -24.9]))))
    (testing "rewrite-vector!"
      (is (= (map float (rewrite-vector! 0.05 (array (range 10)) (array (repeat 10 1))))
             (map float [0.05 1.05 2.05 3.05 4.05 5.05 6.05 7.05 8.05 9.05]))))
    (testing "alter-vec"
      (is (= (seq (alter-vec x sigmoid))
             (mapv float [0.5,0.7310585975646973,0.8807970881462097,0.9525741338729858,0.9820137619972229]))))

    ))
