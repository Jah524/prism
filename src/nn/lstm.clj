(ns nn.lstm
  (:require
    [matrix.default :refer [transpose sum times outer minus] :as default]
    [shiki.unit :refer [sigmoid tanh activation derivative unit-input model-rand]]
    [clj-time.format :as f]
    [clj-time.local  :as l]))


(defn standard-activation [input-list model-layer & [lstm-option]]
  (let [gemv (if-let [it (:gemv lstm-option)] it default/agemv)
        w (:w model-layer)
        v (gemv w input-list)
        w-num (count w)
        unit-num (:unit-num model-layer)
        state (float-array unit-num)
        bias (:bias model-layer)
        _ (dotimes [x unit-num] (aset ^floats state x (float (+ (aget ^floats v x) (aget ^floats bias x)))))
        activation (activation state (:activate-fn model-layer))]
    {:activation activation :state state}))

(defn lstm-activation [model-layer bottom-input-list recurrent-input-list previous-cell-state & [lstm-option]]
  (let [gemv (if-let [it (:gemv lstm-option)] it default/agemv)
        {:keys [block-wr block-bias input-gate-wr input-gate-bias input-gate-peephole
                forget-gate-wr forget-gate-bias forget-gate-peephole
                output-gate-wr output-gate-bias output-gate-peephole peephole unit-num sparse?
                sparses]} model-layer
        [block' input-gate' forget-gate' output-gate']
        (if sparse?
          (->> (vec bottom-input-list)
               (mapv (fn [[sparse-k v]]
                       (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses sparse-k)
                             v-arr (float-array (take unit-num (repeat v)))]
                         [(times block-w v-arr) (times input-gate-w v-arr) (times forget-gate-w v-arr) (times output-gate-w v-arr)])))
;               (apply mapv #(println (vec %))))
               (apply mapv sum))

          (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} model-layer
                lstm-mat [block-w input-gate-w forget-gate-w output-gate-w]]
            (mapv #(gemv % bottom-input-list) lstm-mat)))
        lstm-mat-r  [block-wr input-gate-wr forget-gate-wr output-gate-wr]
        [block-r' input-gate-r' forget-gate-r' output-gate-r'] (mapv #(gemv % recurrent-input-list) lstm-mat-r)
        block       (sum block' block-r' block-bias)
        input-gate  (if (contains? peephole :input-gate)
                      (sum (sum input-gate' input-gate-r' input-gate-bias) (times input-gate-peephole previous-cell-state))
                      (sum input-gate' input-gate-r' input-gate-bias))
        forget-gate (if (contains? peephole :forget-gate)
                      (sum (sum forget-gate' forget-gate-r' forget-gate-bias) (times forget-gate-peephole previous-cell-state))
                      (sum forget-gate' forget-gate-r' forget-gate-bias))
        output-gate (if (contains? peephole :output-gate)
                      (sum (sum output-gate' output-gate-r' output-gate-bias) (times output-gate-peephole previous-cell-state))
                      (sum output-gate' output-gate-r' output-gate-bias))
        cell-state  (float-array unit-num)
        _ (dotimes [x unit-num]
            (aset ^floats cell-state x
                  (float (+ (* (tanh (aget ^floats block x)) (sigmoid (aget ^floats input-gate x)))
                            (* (sigmoid (aget ^floats forget-gate x)) (aget ^floats previous-cell-state x))))))
        lstm (float-array unit-num)
        _ (dotimes [x unit-num]
            (aset ^floats lstm x (float (* (sigmoid (aget ^floats output-gate x)) (tanh (aget ^floats cell-state x))))))]
    {:activation lstm
     :state {:lstm lstm :block block :input-gate input-gate :forget-gate forget-gate :output-gate output-gate :cell-state cell-state}}))

(defn lstm-model-output
  [model x-input previous-hidden-output-layers previous-cell-state-list & [lstm-option]]
  (let [deep-model? (= :deep (:model-type model))]
    (loop [layers (:layers model),
           previous-hidden-output-layers previous-hidden-output-layers,
           previous-cell-state-list previous-cell-state-list,
           activation-acc [x-input],
           state-acc [x-input]]
      (if-let [layer (first layers)]
        (let [previous-hidden-output-layer (first previous-hidden-output-layers)
              previous-cell-state (first previous-cell-state-list)
              {:keys [activation state]} (if (= :lstm (:unit-type layer))
                                           (if deep-model?
                                             (lstm-activation layer
                                                              (float-array (concat (when-not (= [x-input] activation-acc) ; at least one hidden layer
                                                                                    (last activation-acc)) ; x-input
                                                                                  (first activation-acc)))
                                                              previous-hidden-output-layer previous-cell-state lstm-option)
                                             (lstm-activation layer (first activation-acc) previous-hidden-output-layer previous-cell-state lstm-option))
                                           (if deep-model?
                                             (standard-activation (float-array (apply concat (rest (reverse activation-acc)))) layer lstm-option)
                                             (standard-activation (first activation-acc) layer lstm-option)))]
          (recur (rest layers)
                 (rest previous-hidden-output-layers)
                 (rest previous-cell-state-list)
                 (cons activation activation-acc)
                 (cons state state-acc)))
        {:activation (vec (reverse activation-acc))
         :state (vec (reverse state-acc))}))))

(defn sequential-output [model x-seq & [lstm-option]]
  (let [layers (:layers model)
        hidden-num-list (map :unit-num (butlast layers))]
    (loop [x-seq x-seq,
           previous-hidden-output-list (map #(float-array %) hidden-num-list)
           previous-cell-state-list (map #(float-array %) hidden-num-list)
           acc []]
      (if-let [x-list (first x-seq)]
        (let [model-output (lstm-model-output model x-list previous-hidden-output-list previous-cell-state-list lstm-option)
              {:keys [activation state]} model-output]
          (recur (rest x-seq)
                 (rest (butlast activation));omit input output
                 (map :cell-state (rest (butlast state)));omit input output
                 (cons model-output acc)))
        (vec (reverse acc))))))


;;;;    Back Propagation Through Time    ;;;;


(defn lstm-delta
  "Propagation in a unit"
  [propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
   peephole peephole-w-input-gate peephole-w-forget-gate peephole-w-output-gate]
  (let [unit-num (count propagated-delta)
        output-gate-delta (float-array unit-num)
        _dog (derivative (:output-gate lstm-state) :sigmoid)
        _cell-state (:cell-state lstm-state)
        _ (dotimes [x unit-num] (aset ^floats output-gate-delta x
                                      (float (* (aget ^floats _dog x) (tanh (aget ^floats _cell-state x)) (aget ^floats propagated-delta x)))))
        cell-state-delta (float-array unit-num)
        _og (:output-gate lstm-state)
        _dcs (derivative (:cell-state lstm-state) :tanh)
        _fg:t+1 (:forget-gate lstm-state:t+1)
        _csd:t+1 (:cell-state-delta self-delta:t+1)
        _pig (if (contains? peephole :input-gate)  peephole-w-input-gate  (float-array unit-num))
        _pfg (if (contains? peephole :forget-gate) peephole-w-forget-gate (float-array unit-num))
        _pog (if (contains? peephole :output-gate) peephole-w-output-gate (float-array unit-num))
        _igd:t+1 (:input-gate-delta self-delta:t+1)
        _fgd:t+1 (:forget-gate-delta self-delta:t+1)
        _ (dotimes [x unit-num] (aset ^floats cell-state-delta x
                                      (float (+ (* (sigmoid (aget ^floats _og x)) (aget ^floats _dcs x) (aget ^floats propagated-delta x))
                                                (* (sigmoid (aget ^floats _fg:t+1 x)) (aget ^floats _csd:t+1 x))
                                                (* (aget ^floats _pig x) (aget ^floats _igd:t+1 x))
                                                (* (aget ^floats _pfg x) (aget ^floats _fgd:t+1 x))
                                                (* (aget ^floats _pog x) (aget ^floats output-gate-delta x))))))
        block-delta (float-array unit-num)
        _ig (:input-gate lstm-state)
        _db (derivative (:block lstm-state) :tanh)
        _ (dotimes [x unit-num] (aset ^floats block-delta x
                                      (float (* (sigmoid (aget ^floats _ig x)) (aget ^floats _db x) (aget ^floats cell-state-delta x)))))
        forget-gate-delta (float-array unit-num)
        _dfg (derivative (:forget-gate lstm-state) :sigmoid)
        _ (dotimes [x unit-num] (aset ^floats forget-gate-delta x
                                      (float (* (aget ^floats _dfg x) (aget ^floats cell-state:t-1 x) (aget ^floats cell-state-delta x)))))
        input-gate-delta (float-array unit-num)
        _dig (derivative (:input-gate lstm-state) :sigmoid)
        _b (:block lstm-state)
        _ (dotimes [x unit-num] (aset ^floats input-gate-delta x
                                      (float (* (aget ^floats _dig x) (tanh (aget ^floats _b x)) (aget ^floats cell-state-delta x)))))]
    {:output-gate-delta output-gate-delta :cell-state-delta cell-state-delta :block-delta block-delta
     :forget-gate-delta forget-gate-delta :input-gate-delta input-gate-delta}))


(defn hidden-layer-delta
  "
  propagation for a layer
  offset-output equals to all-hidden-bottom-unit-num in deep-model, also equals hidden-bottom-unit-num in stack-model
  offset-above  equals to all-bottom-unit-num (include self) in deep-model, also equals to 0 in stack-model
  offset-self   equals to all-bottom-unit-num (exclude self) in deep-model, also equals to bottom-unit-num in stack-model
  "
  [highest-hidden? model-type output-layer above-layer self-layer offset-output offset-above output-delta above-delta self-delta-list:t+1 & [lstm-option]]
  (let [gemv (if-let [it (:gemv lstm-option)] it default/agemv)
        output-w   (:w output-layer)
        {above-block-w :block-w above-input-gate-w :input-gate-w above-forget-gate-w :forget-gate-w above-output-gate-w :output-gate-w} above-layer
        {above-block-delta :block-delta above-input-gate-delta :input-gate-delta above-forget-gate-delta :forget-gate-delta above-output-gate-delta :output-gate-delta} above-delta
        {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr peephole]} self-layer
        {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} self-delta-list:t+1
        output-num (:unit-num output-layer)
        above-num  (:unit-num above-layer)
        self-num   (:unit-num self-layer)
        need-output? (or highest-hidden? (= :deep model-type))
        need-above?  (not highest-hidden?)
        output-range (if need-output? (* self-num output-num) 0)
        above-range (if need-above? (* self-num above-num 4) 0)
        _tmp (float-array  (+ output-range above-range (* self-num self-num 4)))
        ; (+ self->output ,self->above 1 block with 3 gates and self->self 1 block with 3 gates)
        ;self->output
        _ (when need-output?
            (dotimes [x output-num]
              (dotimes [y self-num]
                (aset ^floats _tmp (+ (* x self-num) y) (aget ^floats output-w (+ (* x self-num) offset-output y))))))
        ;self->above
        _ (when need-above?
            (dotimes [x above-num]
              (dotimes [y self-num]
                (aset ^floats _tmp (+ output-range (* x self-num) y) (aget ^floats above-block-w (+ (* x self-num) offset-above y)))
                (aset ^floats _tmp (+ output-range (* self-num above-num)   (* x self-num) y) (aget ^floats above-input-gate-w  (+ (* x self-num) offset-above y)))
                (aset ^floats _tmp (+ output-range (* self-num above-num 2) (* x self-num) y) (aget ^floats above-forget-gate-w (+ (* x self-num) offset-above y)))
                (aset ^floats _tmp (+ output-range (* self-num above-num 3) (* x self-num) y) (aget ^floats above-output-gate-w (+ (* x self-num) offset-above y))))))
        ;self->self
        _ (dotimes [x self-num]
            (dotimes [y self-num]
              (aset ^floats _tmp (+ output-range above-range (* x self-num) y) (aget ^floats block-wr (+ (* x self-num) y)))
              (aset ^floats _tmp (+ output-range above-range (* self-num self-num)   (* x self-num) y) (aget ^floats input-gate-wr  (+ (* x self-num) y)))
              (aset ^floats _tmp (+ output-range above-range (* self-num self-num 2) (* x self-num) y) (aget ^floats forget-gate-wr (+ (* x self-num) y)))
              (aset ^floats _tmp (+ output-range above-range (* self-num self-num 3) (* x self-num) y) (aget ^floats output-gate-wr (+ (* x self-num) y)))))
        w (default/atranspose self-num _tmp)
        output-range (if need-output? output-num 0)
        above-range (if need-above? (* above-num 4) 0)
        delta-list (float-array (+ (if need-output? output-num 0) (if need-above? (* above-num 4) 0) (* self-num 4)))
        ;output-delta
        _ (when need-output? (dotimes [x output-num] (aset ^floats delta-list x (aget ^floats output-delta x))))
        ;above-delta
        _ (when need-above?  (dotimes [x above-num]
                               (aset ^floats delta-list (+ output-range x) (aget ^floats above-block-delta x))
                               (aset ^floats delta-list (+ output-range above-num x) (aget ^floats above-input-gate-delta x))
                               (aset ^floats delta-list (+ output-range (* above-num 2) x) (aget ^floats above-forget-gate-delta x))
                               (aset ^floats delta-list (+ output-range (* above-num 3) x) (aget ^floats above-output-gate-delta x))))
        ;self-delta
        _ (dotimes [x self-num]
            (aset ^floats delta-list (+ output-range above-range x) (aget ^floats block-delta x))
            (aset ^floats delta-list (+ output-range above-range self-num x) (aget ^floats input-gate-delta x))
            (aset ^floats delta-list (+ output-range above-range (* self-num 2) x) (aget ^floats forget-gate-delta x))
            (aset ^floats delta-list (+ output-range above-range (* self-num 3) x) (aget ^floats output-gate-delta x)))]
    (gemv w delta-list)))


(defn time-fixed-back-propagation
  "back propagation for fixed time"
  [model time-fixed-output-net:t+1 time-fixed-output-net:t time-fixed-output-net:t-1 output-delta self-delta-list:t+1 & [lstm-option]]
  (let [output-layer (last (:layers model))
        deep-model? (= :deep (:model-type model))]
    (loop [layers' (reverse (:layers model)),
           time-fixed-state-net:t+1' (reverse (:state time-fixed-output-net:t+1)),
           time-fixed-state-net:t'   (reverse (:state time-fixed-output-net:t)),
           time-fixed-state-net:t-1' (reverse (:state time-fixed-output-net:t-1)),
           self-delta-list:t+1' (reverse self-delta-list:t+1),
           all-above-nums [];[<-bottom top->]
           all-bottom-nums' (rest (rest (reverse (:unit-nums model))));omit output and self-hidden unit-num
           above-delta output-delta,
           delta-acc []]
      (if (first (rest layers'))
        (let [above-layer (first layers')
              self-layer (second layers')
              self-delta:t+1 (first self-delta-list:t+1')
              propagated-delta (if (= :output (:layer-type above-layer))
                                 (hidden-layer-delta true (:model-type model) output-layer nil self-layer
                                                     (if deep-model? (reduce + (butlast all-bottom-nums')) 0) nil
                                                     output-delta nil self-delta:t+1 lstm-option)
                                 (hidden-layer-delta false (:model-type model) output-layer above-layer self-layer
                                                     (if deep-model? (reduce + all-above-nums) nil) (if deep-model? (last all-bottom-nums') 0)
                                                     output-delta above-delta self-delta:t+1 lstm-option))
              lstm-state:t+1 (first (rest time-fixed-state-net:t+1'))
              lstm-state     (first (rest time-fixed-state-net:t'))
              cell-state:t-1 (:cell-state (first (rest time-fixed-state-net:t-1')))
              {:keys [peephole input-gate-w forget-gate-w output-gate-w unit-num input-gate-peephole forget-gate-peephole output-gate-peephole]} self-layer
              lstm-delta (lstm-delta propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                     peephole input-gate-peephole forget-gate-peephole output-gate-peephole)]
          (recur (rest layers')
                 (rest time-fixed-state-net:t+1')
                 (rest time-fixed-state-net:t')
                 (rest time-fixed-state-net:t-1')
                 (rest self-delta-list:t+1')
                 (cons (first all-bottom-nums') all-above-nums)
                 (rest all-bottom-nums')
                 lstm-delta
                 (cons lstm-delta delta-acc)))
        (reverse delta-acc)))))


(defn param-delta
  "for standard-unit at output layer"
  [delta-list bottom-layer-output]
  {:w-delta    (outer delta-list bottom-layer-output)
   :bias-delta delta-list})

(->> (range 0 31) (filter #(zero? (mod % 3))) (map dec) (filter pos?))
(->> (take-nth 3 (drop 2 [1 10 48 26 39 17 49 84 18])))
(defn lstm-param-delta
  [model lstm-layers-delta time-fixed-output-net time-fixed-output-net:t-1']
  (let [deep-model? (= :deep (:model-type model))
        x-input     (first (:activation time-fixed-output-net))]
    (loop [layers (:layers model);bottom to top
           time-fixed-output-net     (butlast (:activation time-fixed-output-net))     ;omit output-layer
           time-fixed-output-net:t-1 (butlast (:activation time-fixed-output-net:t-1'));
           time-fixed-state-net:t-1  (butlast (:state      time-fixed-output-net:t-1'));
           lstm-layers-delta    lstm-layers-delta
           acc nil]
      (if (second layers)
        (let [{:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} (first lstm-layers-delta)
              self-layer (first layers)
              self-num (:unit-num self-layer)
              sparse? (:sparse? self-layer)
              bottom-output   (first time-fixed-output-net)
              self-output:t-1 (second time-fixed-output-net:t-1)
              self-state:t-1  (second time-fixed-state-net:t-1)
              _xin (if (and deep-model? (not= 0 (count acc))) (count x-input) 0); if deep and not lowest-hidden-layer
              _bon (count bottom-output)
              input-num  (+ _xin _bon)

              block-wr-delta (outer block-delta self-output:t-1)
              input-gate-wr-delta (outer input-gate-delta self-output:t-1)
              forget-gate-wr-delta (outer forget-gate-delta self-output:t-1)
              output-gate-wr-delta (outer output-gate-delta self-output:t-1)
              peephole (:peephole self-layer)
              peephole-input-gate (when (contains? peephole :input-gate)   (times input-gate-delta  (:cell-state self-state:t-1)))
              peephole-forget-gate (when (contains? peephole :forget-gate) (times forget-gate-delta (:cell-state self-state:t-1)))
              peephole-output-gate (when (contains? peephole :output-gate) (times output-gate-delta (:cell-state self-state:t-1)))
              template {:block-wr-delta block-wr-delta :input-gate-wr-delta input-gate-wr-delta
                        :forget-gate-wr-delta forget-gate-wr-delta :output-gate-wr-delta output-gate-wr-delta
                        :block-bias-delta block-delta
                        :input-gate-bias-delta input-gate-delta
                        :forget-gate-bias-delta forget-gate-delta
                        :output-gate-bias-delta output-gate-delta
                        :peephole-input-gate-delta peephole-input-gate :peephole-forget-gate-delta peephole-forget-gate
                        :peephole-output-gate-delta peephole-output-gate}
              param-delta (if sparse?
                            (let [sparses-delta (reduce (fn [acc [sparse-k v]]
                                                          (let [v-arr (float-array (take self-num (repeat v)))]
                                                            (assoc acc sparse-k {:block-w-delta       (times block-delta v-arr)
                                                                                 :input-gate-w-delta  (times input-gate-delta v-arr)
                                                                                 :forget-gate-w-delta (times forget-gate-delta v-arr)
                                                                                 :output-gate-w-delta (times output-gate-delta v-arr)})))
                                                        {}
                                                        x-input)]
                              (-> template (assoc :sparses-delta sparses-delta)))
                            (let [input-list (float-array input-num)
                                  _ (dotimes [x _xin] (aset ^floats input-list x (aget ^floats x-input x)))
                                  _ (dotimes [x _bon] (aset ^floats input-list (+ _xin x) (aget ^floats bottom-output x)))
                                  block-w-delta  (outer block-delta input-list)
                                  input-gate-w-delta  (outer input-gate-delta input-list)
                                  forget-gate-w-delta  (outer forget-gate-delta input-list)
                                  output-gate-w-delta  (outer output-gate-delta input-list)]
                              (-> template (assoc
                                             :block-w-delta block-w-delta :input-gate-w-delta input-gate-w-delta
                                             :forget-gate-w-delta forget-gate-w-delta :output-gate-w-delta output-gate-w-delta))))]
          (recur (rest layers)
                 (rest time-fixed-output-net)
                 (rest time-fixed-output-net:t-1)
                 (rest time-fixed-state-net:t-1)
                 (rest lstm-layers-delta)
                 (cons param-delta acc)))
        (reverse acc)))))

(defn lstm-delta-zeros [model]
  (->> (:unit-nums model)
       butlast
       (mapv (fn[x]
               {:block-delta      (float-array x)
                :input-gate-delta  (float-array x)
                :forget-gate-delta (float-array x)
                :output-gate-delta (float-array x)
                :cell-state-delta  (float-array x)}))))

(defn zeros-output-layer [output-layer]
  (float-array (:unit-num output-layer)))

(defn lstm-zeros [model]
  {:activation (map (fn [{n :unit-num}] (float-array (take n (repeat 0))))
                    (cons {:unit-num 0}(:layers model)))
   :state (map (fn [{n :unit-num}] {:cell-state  (float-array n)
                                    :forget-gate (float-array n)})
               (cons {:unit-num 0}(:layers model)))})


(defn bptt-partial [model output-net-seq output-layer-delta & [lstm-option]]
  (let [bottom-layer-output (if (= :deep (:model-type model))
                              (float-array (apply concat (rest (butlast (:activation (last output-net-seq))))));all-hidden-bottom
                              (last (butlast (:activation (last output-net-seq)))));bottom
        output-param-delta (param-delta output-layer-delta bottom-layer-output)
        pseudo-output-net:t+1 (lstm-zeros model);{:state (map (fn [layer] {:forget-gate (take (:unit-num layer) (repeat 0))}) (:layers model))}
        pseudo-output-net:t-1 (lstm-zeros model)
        seq-length (count output-net-seq)]
    (loop [output-net-seq' (cons pseudo-output-net:t+1 (reverse (cons pseudo-output-net:t-1 output-net-seq))),
           output-layer-delta output-layer-delta,
           self-delta-list:t+1, (lstm-delta-zeros model)
           lstm-param-delta-acc nil,
           counter 0]
      (if (< counter (min (get lstm-option :truncate seq-length) seq-length))
        (let [time-fixed-output-net:t+1  (first  output-net-seq')
              time-fixed-output-net      (second output-net-seq')
              time-fixed-output-net:t-1  (nth output-net-seq' 2)
              lstm-layers-delta (time-fixed-back-propagation model time-fixed-output-net:t+1 time-fixed-output-net time-fixed-output-net:t-1
                                                             output-layer-delta self-delta-list:t+1 lstm-option)
              lstm-layers-param-delta  (lstm-param-delta model lstm-layers-delta time-fixed-output-net time-fixed-output-net:t-1)]
          (recur (rest output-net-seq')
                 (zeros-output-layer (last (:layers model)))
                 lstm-layers-delta
                 (if (nil? lstm-param-delta-acc)
                   lstm-layers-param-delta
                   (mapv (fn [acc-layer delta-layer]
                           (merge-with #(if (map? %1)
                                          (merge-with (fn [accw dw]
                                                        (merge-with (fn [acc d]
                                                                      (sum acc d))
                                                                    accw dw))
                                                      %1 %2)
                                          (sum %1 %2))
                                       acc-layer delta-layer))
                         lstm-param-delta-acc lstm-layers-param-delta))
                 (inc counter)))
        (-> lstm-param-delta-acc vec (conj output-param-delta))))))
;(merge-with #(merge-with + %1 %2) {:a {:c 1}} {:a {:c 10}})

(defn bptt [model training-x-seq training-y-seq & [lstm-option]]
  (loop [model-output-seq  (sequential-output model training-x-seq lstm-option)
         training-x-seq' (reverse training-x-seq)
         training-y-seq' (reverse training-y-seq)
         delta-list-acc nil]
    (if-let [training-y (first training-y-seq')]
      (if (= :pass training-y)
        (recur (butlast model-output-seq)
               (rest training-x-seq')
               (rest training-y-seq')
               delta-list-acc)
        (let [model-output (last (:activation (last model-output-seq)))
              model-state  (last (:state (last model-output-seq)))
              output-delta (minus training-y model-output)
              delta-list (bptt-partial model model-output-seq output-delta lstm-option)]
          (recur (butlast model-output-seq)
                 (rest training-x-seq')
                 (rest training-y-seq')
                 (if (nil? delta-list-acc)
                   delta-list
                   (mapv (fn [acc-layer delta-layer]
                           (merge-with #(if (map? %1)
                                          (merge-with (fn [accw dw]
                                                        (merge-with (fn [acc d]
                                                                      (sum acc d))
                                                                    accw dw))
                                                      %1 %2)
                                          (sum %1 %2))
                                       acc-layer delta-layer))
                         delta-list-acc delta-list)))))
      delta-list-acc)))


(defn update-model
  "param-delta-list: [hidden->...->hidden->output]"
  [model param-delta-list learning-rate]
  (let [output-layer (last (:layers model))
        {:keys [w-delta bias-delta]} (last param-delta-list)
        w-num (count w-delta)
        unit-num (:unit-num output-layer)
        _ (dotimes [x w-num]
            (aset ^floats (:w output-layer) x (float (+ (aget ^floats (:w output-layer) x) (* learning-rate (aget ^floats w-delta x))))))
        _ (dotimes [x unit-num]
            (aset ^floats (:bias output-layer) x (float (+ (aget ^floats (:bias output-layer) x) (* learning-rate (aget ^floats bias-delta x))))))]
    (loop [layers (:layers model)
           param-delta-list param-delta-list]
      (if (second layers)
        (let [layer (first layers)
              {:keys [block-w-delta block-wr-delta block-bias-delta input-gate-w-delta input-gate-wr-delta input-gate-bias-delta
                      forget-gate-w-delta forget-gate-wr-delta forget-gate-bias-delta output-gate-w-delta output-gate-wr-delta output-gate-bias-delta
                      peephole-input-gate-delta peephole-forget-gate-delta peephole-output-gate-delta sparses-delta]} (first param-delta-list)
              {:keys [block-w block-wr block-bias input-gate-w input-gate-wr input-gate-bias
                      forget-gate-w forget-gate-wr forget-gate-bias output-gate-w output-gate-wr output-gate-bias
                      input-gate-peephole forget-gate-peephole output-gate-peephole
                      unit-num peephole sparse? sparses]} layer
              wrn (* unit-num unit-num)
              bn (:unit-num layer)
              ;update for bottom connection
              _ (if sparse?
                  (->> sparses-delta
                       (mapv (fn [[sparse-k lstm-w-delta]]
                               (let [{:keys [block-w-delta input-gate-w-delta forget-gate-w-delta output-gate-w-delta]} lstm-w-delta
                                     {:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses sparse-k)]
                                 (dotimes [x unit-num]
                                   (aset ^floats block-w x (float (+ (aget ^floats block-w x) (* learning-rate (aget ^floats block-w-delta x)))))
                                   (aset ^floats input-gate-w x (float (+ (aget ^floats input-gate-w x) (* learning-rate (aget ^floats input-gate-w-delta x)))))
                                   (aset ^floats forget-gate-w x (float (+ (aget ^floats forget-gate-w x) (* learning-rate (aget ^floats forget-gate-w-delta x)))))
                                   (aset ^floats output-gate-w x (float (+ (aget ^floats output-gate-w x) (* learning-rate (aget ^floats output-gate-w-delta x))))))))))
                  (let [wn  (count block-w-delta)]
                    (dotimes [x wn]
                      (aset ^floats block-w x (float (+ (aget ^floats block-w x) (* learning-rate (aget ^floats block-w-delta x)))))
                      (aset ^floats input-gate-w x (float (+ (aget ^floats input-gate-w x) (* learning-rate (aget ^floats input-gate-w-delta x)))))
                      (aset ^floats forget-gate-w x (float (+ (aget ^floats forget-gate-w x) (* learning-rate (aget ^floats forget-gate-w-delta x)))))
                      (aset ^floats output-gate-w x (float (+ (aget ^floats output-gate-w x) (* learning-rate (aget ^floats output-gate-w-delta x))))))))
              ;update for recurrent connection
              _ (dotimes [x wrn]
                  (aset ^floats block-wr x (float (+ (aget ^floats block-wr x) (* learning-rate (aget ^floats block-wr-delta x)))))
                  (aset ^floats input-gate-wr x (float (+ (aget ^floats input-gate-wr x) (* learning-rate (aget ^floats input-gate-wr-delta x)))))
                  (aset ^floats forget-gate-wr x (float (+ (aget ^floats forget-gate-wr x) (* learning-rate (aget ^floats forget-gate-wr-delta x)))))
                  (aset ^floats output-gate-wr x (float (+ (aget ^floats output-gate-wr x) (* learning-rate (aget ^floats output-gate-wr-delta x))))))
              ;update lstm biases
              _ (dotimes [x bn]
                  (aset ^floats block-bias x (float (+ (aget ^floats block-bias x) (* learning-rate (aget ^floats block-bias-delta x)))))
                  (aset ^floats input-gate-bias x (float (+ (aget ^floats input-gate-bias x) (* learning-rate (aget ^floats input-gate-bias-delta x)))))
                  (aset ^floats forget-gate-bias x (float (+ (aget ^floats forget-gate-bias x) (* learning-rate (aget ^floats forget-gate-bias-delta x)))))
                  (aset ^floats output-gate-bias x (float (+ (aget ^floats output-gate-bias x) (* learning-rate (aget ^floats output-gate-bias-delta x)))))
                  ;and peephole
                  (when (contains? peephole :input-gate)  (aset ^floats input-gate-peephole  x (float (+ (aget ^floats input-gate-peephole x)  (* learning-rate (aget ^floats peephole-input-gate-delta  x))))))
                  (when (contains? peephole :forget-gate) (aset ^floats forget-gate-peephole x (float (+ (aget ^floats forget-gate-peephole x) (* learning-rate (aget ^floats peephole-forget-gate-delta x))))))
                  (when (contains? peephole :output-gate) (aset ^floats output-gate-peephole x (float (+ (aget ^floats output-gate-peephole x) (* learning-rate (aget ^floats peephole-output-gate-delta x)))))))]
          (recur (rest layers)
                 (rest param-delta-list)))
        model))))

(defn random-array [n]
  (let [it (float-array n)]
    (dotimes [x n] (aset ^floats it x (model-rand)))
    it))

(defn init-lstm-model
  "caution: sparse model doesn't support multiple layered model yet"
  [model-design & [all-sparse-items]]
  (let [input-layer-n (:unit-num (first (:layers model-design)))
        deep-model?  (= :deep (:model-type model-design))
        sparse? (= :sparse (:input-type model-design))]
    (loop [layers-info (:layers model-design),
           acc []]
      (if-let [layer-info (first (rest layers-info))]
        (let [{self-layer-n :unit-num a :activate-fn layer-type :layer-type unit-type :unit-type peephole :peephole} layer-info
              {bottom-layer-n :unit-num lt :layer-type} (first layers-info)]
          (recur (rest layers-info)
                 (cons (cond (= :lstm unit-type)
                             (let [sparse-w? (or (and (= :lstm unit-type) sparse? deep-model?)    ;for every deep layer with sparse
                                                 (and (= :lstm unit-type) sparse? (= lt :input))) ;or lowest stack with sparse
                                   bottom-w-num (if deep-model?
                                                  (+ input-layer-n (if (empty? acc) 0 bottom-layer-n))
                                                  bottom-layer-n)
                                   bwr (random-array (* self-layer-n self-layer-n));for recurrent connection
                                   bb  (random-array self-layer-n)
                                   iwr (random-array (* self-layer-n self-layer-n))
                                   ib  (random-array self-layer-n)
                                   ip  (random-array (if (contains? peephole :input-gate) self-layer-n 0))
                                   fwr (random-array (* self-layer-n self-layer-n))
                                   fb  (random-array self-layer-n)
                                   fp  (random-array (if (contains? peephole :forget-gate) self-layer-n 0))
                                   owr (random-array (* self-layer-n self-layer-n))
                                   ob  (random-array self-layer-n)
                                   op  (random-array (if (contains? peephole :output-gate) self-layer-n 0))
                                   template {:layer-type layer-type
                                             :unit-num self-layer-n
                                             :unit-type :lstm
                                             :block-wr       bwr   :block-bias           bb
                                             :input-gate-wr  iwr   :input-gate-bias      ib
                                             :forget-gate-wr fwr   :forget-gate-bias     fb
                                             :output-gate-wr owr   :output-gate-bias     ob
                                             :input-gate-peephole  ip  :forget-gate-peephole fp
                                             :output-gate-peephole op  :peephole peephole}]
                               (if sparse-w?
                                 (let [sparses (reduce (fn [acc sparse]
                                                         (assoc acc sparse {:block-w       (random-array self-layer-n)
                                                                            :input-gate-w  (random-array self-layer-n)
                                                                            :forget-gate-w (random-array self-layer-n)
                                                                            :output-gate-w (random-array self-layer-n)}))
                                                       {} all-sparse-items)]
                                   (-> template (assoc :sparses sparses :sparse? true)))
                                 (let [bw  (random-array (* self-layer-n bottom-w-num));for bottom connection
                                       iw  (random-array (* self-layer-n bottom-w-num))
                                       fw  (random-array (* self-layer-n bottom-w-num))
                                       ow  (random-array (* self-layer-n bottom-w-num))]
                                   (-> template (assoc :block-w bw :input-gate-w iw  :forget-gate-w fw :output-gate-w ow)))))

                             (= layer-type :hidden);equals to traditional RNN
                             :not-supported-yet
                             :else
                             (let [bottom-w-num (if deep-model?
                                                  (->> (:layers model-design) (filter #(= :hidden (:layer-type %))) (map :unit-num) (reduce +))
                                                  bottom-layer-n)
                                   w (random-array (* self-layer-n bottom-w-num))
                                   bias (random-array self-layer-n)]
                               {:activate-fn a
                                :layer-type layer-type
                                :unit-num self-layer-n
                                :w w
                                :bias bias}))
                       acc)))
        (-> model-design
            (assoc :unit-nums (mapv :unit-num  (:layers model-design)))
            (assoc :layers (reverse acc)))))))
