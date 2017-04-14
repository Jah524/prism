(ns nlp.rnnlm-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [prism.nlp.rnnlm :refer :all]))

(def sample-wl {"A" 5 "B" 5 "C" 5 "D" 5 "<unk>" 10})

(deftest word2ec-test
  (testing "init-rnnlm-model"
    (let [{:keys [hidden wl input-type output-type]} (init-rnnlm-model {"A" 12 "B" 345 "C" 42 "<unk>" 0} 10)
          {:keys [unit-num]} hidden]
      (is (= unit-num 10))
      (is (= input-type :sparse))
      (is (= output-type :binary-classification))
      (is (= (count (keys wl)) 4))))
  (testing "convert-rare-word-to-unk"
    (is (= (convert-rare-word-to-unk {"A" 10 "B" 20} "A") "A"))
    (is (= (convert-rare-word-to-unk {"A" 10 "B" 20} "X") "<unk>")))
  (testing "tok->rnnlm-pairs"
    (is (= (tok->rnnlm-pairs sample-wl "A B C D <unk>")
           {:x [#{"A"} #{"B"} #{"C"} #{"D"} #{"<unk>"}],
            :y [{:pos #{"B"}} {:pos #{"C"}} {:pos #{"D"}} {:pos #{"<unk>"}} {:pos #{"<eos>"}}]}))
    (is (= (tok->rnnlm-pairs sample-wl "A   B")
           {:x [#{"A"} #{"B"}] :y [{:pos #{"B"}} {:pos #{"<eos>"}}]}))
    (is (= (tok->rnnlm-pairs sample-wl "A   X B ")
           {:x [#{"A"} #{"<unk>"} #{"B"}]
            :y [{:pos #{"<unk>"}} {:pos #{"B"}} {:pos #{"<eos>"}}]})))

  (testing "add-negatives"
    (let [rnnlm-pair (tok->rnnlm-pairs sample-wl "A B X   D <unk>  ")]
      (is (= (add-negatives rnnlm-pair 2 ["AAA" "BBB" "CCC" "DDD" "EEE" "FFF" "GGG" "HHH" "III" "JJJ"])
             {:x [#{"A"} #{"B"} #{"<unk>"} #{"D"} #{"<unk>"}],
              :y [{:pos #{"B"}, :neg #{"BBB" "AAA"}}
                  {:pos #{"<unk>"}, :neg #{"DDD" "CCC"}}
                  {:pos #{"D"}, :neg #{"EEE" "FFF"}}
                  {:pos #{"<unk>"}, :neg #{"GGG" "HHH"}}
                  {:pos #{"<eos>"}, :neg #{"III" "JJJ"}}]}))
      (is (thrown? Exception (add-negatives rnnlm-pair  ["AAA" "BBB" "CCC" "DDD" "EEE" "FFF" "GGG" "HHH" "III" "JJJ"])))))
  )
