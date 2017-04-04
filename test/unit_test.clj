(ns unit-test
  (:require [clojure.test :refer :all]
            [sai-ai.unit :refer :all]))

(deftest unit-test
  (testing "model-rand"
    (let [samples (take 100000 (repeatedly (fn [] (int (model-rand)))))]
      (is (nil? (some (partial <= 0.08) samples)))
      (is (nil? (some #(> -0.08 %) samples))))))
