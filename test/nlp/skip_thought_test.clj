(ns nlp.skip-thought-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [prism.nlp.skip-thought :refer :all]))

(def example-wc {"A" 123 "B" 321 "C" 432 "D" 12 "E" 432})

(deftest skip-thought-test
  ;;   (testing "init-skip-thought-model"
  ;;     (let [{:keys [hidden hidden-size wc input-type output-type]} (init-rnnlm-model {"A" 12 "B" 345 "C" 42 "<unk>" 0} 10 :lstm)]
  ;;       (is (= hidden-size 10))
  ;;       (is (= output-type :binary-classification))
  ;;       (is (= (count (keys wc)) 4))))
  (testing "convert-rare-word-to-unk"
    (is (= (convert-rare-word-to-unk {"A" 10 "B" 20} "A") "A"))
    (is (= (convert-rare-word-to-unk {"A" 10 "B" 20} "X") "<unk>")))

  (testing "line->skip-thought-pairs"
    (is (= (line->skip-thought-pairs example-wc "A B A <eos> E X A D <eos>  D B C")
           [{:encoder-x      ["A" "B" "A"],
             :decoder-prev-x nil,
             :decoder-prev-y nil,
             :decoder-next-x ["<go>" "E" "<unk>" "A" "D"],
             :decoder-next-y ["E" "<unk>" "A" "D" "<eos>"]}
            {:encoder-x ["E" "<unk>" "A" "D"],
             :decoder-prev-x ["<go>" "A" "B" "A"],
             :decoder-prev-y ["A" "B" "A" "<eos>"],
             :decoder-next-x ["<go>" "D" "B" "C"],
             :decoder-next-y ["D" "B" "C" "<eos>"]}
            {:encoder-x ["D" "B" "C"],
             :decoder-prev-x ["<go>" "E" "<unk>" "A" "D"],
             :decoder-prev-y ["E" "<unk>" "A" "D" "<eos>"],
             :decoder-next-x nil,
             :decoder-next-y nil}])))

  (testing "line->skip-thought-pairs with invalid line"
    (is (= (line->skip-thought-pairs {"A" 123 "B" 321 "C" 432 "D" 12 "E" 432} "A B C")
           :skip)))
  (testing "add-negatives"
    (let [target1 {:encoder-x ["E" "<unk>" "A" "D"],
                   :decoder-prev-x ["<go>" "A" "B" "A"],
                   :decoder-prev-y ["A" "B" "A" "<eos>"],
                   :decoder-next-x ["<go>" "D" "B" "C"],
                   :decoder-next-y ["D" "B" "C" "<eos>"]}
          target2 {:encoder-x      ["A" "B" "A"],
                   :decoder-prev-x nil,
                   :decoder-prev-y nil,
                   :decoder-next-x ["<go>" "E" "<unk>" "A" "D"],
                   :decoder-next-y ["E" "<unk>" "A" "D" "<eos>"]}
          target3 {:encoder-x ["D" "B" "C"],
                   :decoder-prev-x ["<go>" "E" "<unk>" "A" "D"],
                   :decoder-prev-y ["E" "<unk>" "A" "D" "<eos>"],
                   :decoder-next-x nil,
                   :decoder-next-y nil}]
      (is (= (add-negatives target1 2 (->> (range 16) (map str)))
             {:encoder-x ["E" "<unk>" "A" "D"],
              :decoder-prev-x ["<go>" "A" "B" "A"],
              :decoder-prev-y [{:pos #{"A"},     :neg #{"1" "0"}}
                               {:pos #{"B"},     :neg #{"3" "2"}}
                               {:pos #{"A"},     :neg #{"4" "5"}}
                               {:pos #{"<eos>"}, :neg #{"7" "6"}}],
              :decoder-next-x ["<go>" "D" "B" "C"],
              :decoder-next-y [{:pos #{"D"},     :neg #{"9" "8"}}
                               {:pos #{"B"},     :neg #{"11" "10"}}
                               {:pos #{"C"},     :neg #{"12" "13"}}
                               {:pos #{"<eos>"}, :neg #{"14" "15"}}]}))
      (is (= (add-negatives target2 2 (->> (range 10) (map str)))
             {:encoder-x ["A" "B" "A"],
              :decoder-prev-x nil,
              :decoder-prev-y [],
              :decoder-next-x ["<go>" "E" "<unk>" "A" "D"],
              :decoder-next-y [{:pos #{"E"}, :neg #{"1" "0"}}
                               {:pos #{"<unk>"}, :neg #{"3" "2"}}
                               {:pos #{"A"}, :neg #{"4" "5"}}
                               {:pos #{"D"}, :neg #{"7" "6"}}
                               {:pos #{"<eos>"}, :neg #{"9" "8"}}]}))
      (is (= (add-negatives target3 2 (->> (range 10) (map str)))
             {:encoder-x ["D" "B" "C"],
              :decoder-prev-x ["<go>" "E" "<unk>" "A" "D"],
              :decoder-prev-y [{:pos #{"E"}, :neg #{"1" "0"}}
                               {:pos #{"<unk>"}, :neg #{"3" "2"}}
                               {:pos #{"A"}, :neg #{"4" "5"}}
                               {:pos #{"D"}, :neg #{"7" "6"}}
                               {:pos #{"<eos>"}, :neg #{"9" "8"}}],
              :decoder-next-x nil,
              :decoder-next-y []}))
      (is (thrown? Exception (add-negatives target1 ["x"])))))
  )
