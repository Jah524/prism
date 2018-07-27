(ns unit-test
  (:require
    [clojure.test :refer :all]
    [clojure.core.matrix :refer [array ecount row-count]]
    [prism.unit :refer :all]))

(deftest unit-test
  (testing "init-orthogonal-matrix"
    (let [result (init-orthogonal-matrix 5)]
      (is (= (ecount result) 25))
      (is (= (row-count result) 5))))
  (testing "multi-class-prob"
    (is (= (multi-class-prob (array (range 4))
                             {"A" {:w (array (repeat 4 0.1)) :bias (array [-1])}
                              "B" {:w (array (repeat 4 0.1)) :bias (array [-1])}
                              "C" {:w (array (repeat 4 0.2)) :bias (array [1])}})
           {"A" (double 0.06466741726589852),
            "B" (double 0.06466741726589852),
            "C" (double 0.8706651654682029)})))
  (testing "multi-classification-error"
    (is (= (multi-classification-error {"A" 0.2 "B" 0.5 "C" 0.1} "B")
           {"B" 0.5, "A" -0.2, "C" -0.1})))
  (testing "binary-classification-error"
    (is (= (binary-classification-error {"A" 0.2 "B" 0.5 "C" 0.1} {:pos #{"A" "B"} :neg #{"C"}})
           {"B" 0.5, "A" 0.8, "C" -0.1})))
  (testing "prediction-error"
    (is (= (prediction-error {"A" 0.2 "B" 0.5 "C" 0.1} {"A" 1 "B" 1 "C" 1})
           {"A" 0.8, "B" 0.5, "C" 0.9})))
  (testing "merge-param!"
    (is (= (merge-param! (array [1 2 3]) (array [1 2 3]))
           (array [2 4 6])))
    (is (= (merge-param! {"A" 0.2} {"A" -0.5 "B" 0.1})
           {"A" -0.3 "B" 0.1})))
  )
