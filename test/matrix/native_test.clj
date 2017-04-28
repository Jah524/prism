(ns matrix.native-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [uncomplicate.neanderthal.native :refer [dv fv fge dge ftr]]
            [matrix.default :refer :all]
            [matrix.native :as n]))

(def a (object-array (map float-array (partition 5 (range 20)))))
(def x (float-array (range 5)))
(def y (float-array (range 0 50 10)))

(def A (dge 4 5 [0 5 10 15 1 6 11 16 2 7 12 17 3 8 13 18 4 9 14 19]))
(def X (dv (range 5)))
(def Y (dv (range 0 50 10)))

(deftest ^:native native-matrix-test
  (testing "sum"
    (is (= (vec (sum x y))
           (vec (float-array (n/sum X Y)))
           [0.0 11.0 22.0 33.0 44.0])))
  (testing "minus"
    (is (= (vec (minus x y))
           (vec (float-array (n/minus X Y)))
           [0.0 -9.0 -18.0 -27.0 -36.0])))
  (testing "scal"
    (is (= (vec (scal 10 x))
           (vec (float-array (n/scal 10 X)))
           [0.0 10.0 20.0 30.0 40.0])))
  (testing "times"
    (is (= (vec (times x y))
           (vec (float-array (n/times X Y)))
           [0.0 10.0 40.0 90.0 160.0])))
  (testing "dot"
    (is (= (dot x y)
           (n/dot X Y)
           (float 300))))
  (testing "outer"
    (is (= (map vec (outer x y))
           (map vec (n/outer X Y))
           [[0.0 0.0 0.0 0.0 0.0]
            [0.0 10.0 20.0 30.0 40.0]
            [0.0 20.0 40.0 60.0 80.0]
            [0.0 30.0 60.0 90.0 120.0]
            [0.0 40.0 80.0 120.0 160.0]])))
  (testing "transpose"
    (is (= (->> (n/transpose A)
                (map #(map float %)))
           ;;                      (transpose a)))

           (map #(map float %)
                [[0 5 10 15] [1 6 11 16] [2 7 12 17] [3 8 13 18] [4 9 14 19]]))))

  (testing "gemv"
    (is (= (vec (gemv a x))
           (seq (n/gemv A X)))))
  )
