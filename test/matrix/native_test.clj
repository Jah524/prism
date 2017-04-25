(ns matrix.native-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [uncomplicate.neanderthal.native :refer [dv fv fge dge ftr]]
            [matrix.default :refer :all]
            [matrix.native :as n]))

(def a (float-array (range 50)))
(def x (float-array (range 5)))
(def y (float-array (range 0 50 10)))

(def A (n/transpose (dge 5 10 (range 50))))
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
    (is (= (vec (outer x y))
           (->> (mapv float-array (n/outer X Y))
                (mapv vec)
                flatten)
           [0.0 0.0 0.0 0.0 0.0
            0.0 10.0 20.0 30.0 40.0
            0.0 20.0 40.0 60.0 80.0
            0.0 30.0 60.0 90.0 120.0
            0.0 40.0 80.0 120.0 160.0])))
  (testing "transpose"
    (is (= (vec a)
           (->> (n/transpose A)
                (mapv float-array)
                (mapv vec)
                flatten)
           (map float
                [0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0
                 10.0 11.0 12.0 13.0 14.0 15.0 16.0 17.0 18.0 19.0
                 20.0 21.0 22.0 23.0 24.0 25.0 26.0 27.0 28.0 29.0
                 30.0 31.0 32.0 33.0 34.0 35.0 36.0 37.0 38.0 39.0
                 40.0 41.0 42.0 43.0 44.0 45.0 46.0 47.0 48.0 49.0]))))

  (testing "gemv"
    (is (= (vec (gemv a x))
           (vec (float-array (n/gemv A X))))))
  )
