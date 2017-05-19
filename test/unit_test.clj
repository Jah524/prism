(ns unit-test
  (:require
    [clojure.test :refer :all]
    [clojure.core.matrix :refer [array]]
    [matrix.default :refer [default-matrix-kit]]
    [prism.unit :refer :all]))

(deftest unit-test
  (testing "softmax"
    (is (= (mapv float (softmax default-matrix-kit (array (range 4))))
           (map float [0.032058604 0.087144315 0.23688282 0.6439143]))))
  )
