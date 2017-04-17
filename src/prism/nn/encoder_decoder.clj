(ns prism.nn.encoder-decoder
  (:require
    [clojure.pprint :refer [pprint]]
    [matrix.default :refer [transpose sum times outer minus] :as default]
    [prism.unit :refer [tanh sigmoid random-array]]
    [prism.nn.lstm :as lstm]))


(defn encoder-forward [encoder x-seq & [option]]
  (let [hidden-size (:unit-num (:hidden encoder))]
    (loop [x-seq x-seq,
           previous-activation (float-array hidden-size),
           previous-cell-state (float-array hidden-size),
           acc []]
      (if-let [x-input (first x-seq)]
        (let [model-output (lstm/lstm-activation encoder x-input previous-activation previous-cell-state option)
              {:keys [activation state]} model-output]
          (recur (rest x-seq)
                 activation
                 (:cell-state state)
                 (cons model-output acc)))
        (vec (reverse acc))))))


(defn decoder-lstm-activation [model x-input recurrent-input-list encoder-input previous-cell-state & [lstm-option]]
  (let [gemv (if-let [it (:gemv lstm-option)] it default/gemv)
        lstm-layer (:hidden model)
        {:keys [block-wr block-bias input-gate-wr input-gate-bias input-gate-peephole
                forget-gate-wr forget-gate-bias forget-gate-peephole
                output-gate-wr output-gate-bias output-gate-peephole peephole unit-num
                block-we input-gate-we forget-gate-we output-gate-we ;; encoder connection
                sparses]} lstm-layer
        [block' input-gate' forget-gate' output-gate']
        (if (= (:input-type model) :sparse)
          (lstm/partial-state-sparse x-input sparses unit-num)
          (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} lstm-layer
                lstm-mat [block-w input-gate-w forget-gate-w output-gate-w]]
            (mapv #(gemv % x-input) lstm-mat)))
        ;; recurrent-connection
        lstm-mat-r  [block-wr input-gate-wr forget-gate-wr output-gate-wr]
        [block-r' input-gate-r' forget-gate-r' output-gate-r'] (mapv #(gemv % recurrent-input-list) lstm-mat-r)
        ;; encoder connection
        lstm-mat-e  [block-we input-gate-we forget-gate-we output-gate-we]
        [block-e' input-gate-e' forget-gate-e' output-gate-e'] (mapv #(gemv % encoder-input) lstm-mat-e)
        ;; state of each gates
        block       (sum block'       block-r'       block-e'       block-bias)
        input-gate  (sum input-gate'  input-gate-r'  input-gate-e'  input-gate-bias  (times input-gate-peephole  previous-cell-state))
        forget-gate (sum forget-gate' forget-gate-r' forget-gate-e' forget-gate-bias (times forget-gate-peephole previous-cell-state))
        output-gate (sum output-gate' output-gate-r' forget-gate-e' output-gate-bias (times output-gate-peephole previous-cell-state))
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


(defn decoder-output-activation
  [decoder decoder-hidden-list encoder-input previous-input sparse-outputs & [lstm-option]]
  (let [{:keys [output-type output]} decoder
        activation-function (condp = output-type :binary-classification sigmoid :prediction identity)]
    (if (= output-type :multi-class-classification)
      :FIXME
      (reduce (fn [acc s]
                (let [{:keys [w bias encoder-w previous-input-w]} (get output s)]
                  (assoc acc s (activation-function (+ (reduce + (times w decoder-hidden-list)) ;; use areduce fixme
                                                       (aget ^floats bias 0)
                                                       (reduce + (times encoder-w encoder-input))
                                                       (reduce + (times previous-input-w previous-input)))))))
              {}
              (vec sparse-outputs)))))


(defn decoder-activation-time-fixed
  [decoder x-input sparse-outputs previous-hidden-output encoder-input previous-input previous-cell-state & [option]]
  (let [{:keys [activation state]} (decoder-lstm-activation decoder x-input previous-hidden-output encoder-input previous-cell-state option)
        output (if (= :skip sparse-outputs) :skipped (decoder-output-activation decoder activation encoder-input previous-input sparse-outputs option))]
    {:activation {:input x-input :hidden activation :output output}
     :state  {:input x-input :hidden state}}))

(defn decoder-forward [decoder x-seq encoder-input output-items-seq & [option]]
  (let [hidden-size (:unit-num (:hidden decoder))]
    (loop [x-seq x-seq,
           output-items-seq output-items-seq,
           previous-hidden-output (float-array hidden-size),
           previous-input (float-array (:input-size decoder))
           previous-cell-state    (float-array hidden-size),
           acc []]
      (if-let [x-list (first x-seq)]
        (let [decoder-output (decoder-activation-time-fixed decoder x-list (first output-items-seq) previous-hidden-output encoder-input previous-input previous-cell-state option)
              {:keys [activation state]} decoder-output]
          (recur (rest x-seq)
                 (rest output-items-seq)
                 (:hidden activation)
                 x-list
                 (:cell-state (:hidden state))
                 (cons decoder-output acc)))
        (vec (reverse acc))))))


;;   BPTT   ;;


(defn decoder-output-param-delta
  [item-delta-pairs decoder-hidden-size hidden-activation encoder-size encoder-input input-size previous-input]
  (->> item-delta-pairs
       (reduce (fn [acc [item delta]]
                 (assoc acc item {:w-delta    (times (float-array (repeat decoder-hidden-size delta)) hidden-activation)
                                  :bias-delta (float-array [delta])
                                  :encoder-w-delta (times (float-array (repeat encoder-size delta)) encoder-input)
                                  :previous-input-w-delta (times (float-array (repeat input-size delta)) previous-input)}))
               {})))
(comment
  (defn decoder-bptt
    [decoder activation output-items-seq & [option]]
    (let [gemv (if-let [it (:gemv option)] it default/gemv)
          {:keys [output hidden]} decoder
          {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr
                  input-gate-peephole forget-gate-peephole output-gate-peephole
                  unit-num]} hidden]
      ;looping latest to old
      (loop [output-items-seq (reverse output-items-seq)
             propagated-hidden-to-hidden-delta nil,
             output-seq (reverse activation)
             self-delta:t+1 (lstm/lstm-delta-zeros unit-num)
             lstm-state:t+1 (gate-zeros unit-num)
             output-acc nil
             hidden-acc nil]
        (cond
          (and (= :skip (first output-items-seq)) (nil? propagated-hidden-to-hidden-delta))
          (recur (rest output-items-seq)
                 nil
                 (rest output-seq)
                 (lstm-delta-zeros unit-num)
                 (lstm/gate-zeros unit-num)
                 nil
                 nil)
          (first output-seq)
          (let [{:keys [pos neg]} (first output-items-seq);used when binary claassification
                output-delta (condp = (:output-type model)
                               :binary-classification
                               (binary-classification-error (:output (:activation (first output-seq))) pos neg)
                               :prediction
                               (prediction-error  (:output (:activation (first output-seq))) (first output-items-seq)))
                output-param-delta (output-param-delta output-delta unit-num (:hidden (:activation (first output-seq))))
                propagated-output-to-hidden-delta (when-not (= :skip (first output-items-seq))
                                                    (->> output-delta
                                                         (map (fn [[item delta]]
                                                                (let [w (:w (get output item))
                                                                      v (float-array (repeat unit-num delta))]
                                                                  (times w v))))
                                                         (apply sum)))
                ;merging delta: hidden-to-hidden + above-to-hidden
                summed-propagated-delta (cond (and (not= :skip (first output-items-seq)) propagated-hidden-to-hidden-delta)
                                              (sum propagated-hidden-to-hidden-delta propagated-output-to-hidden-delta)
                                              (= :skip (first output-items-seq))
                                              propagated-hidden-to-hidden-delta
                                              (nil? propagated-hidden-to-hidden-delta)
                                              propagated-output-to-hidden-delta)
                ;hidden delta
                lstm-state (:hidden (:state (first output-seq)))
                cell-state:t-1 (or (:cell-state (second (:state (second output-seq)))) (float-array unit-num))
                lstm-part-delta (lstm-part-delta unit-num summed-propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                                 input-gate-peephole forget-gate-peephole output-gate-peephole)
                x-input (:input (:activation (first output-seq)))
                self-activation:t-1 (or (:hidden (:activation (second output-seq)))
                                        (float-array unit-num));when first output time (last time of bptt
                self-state:t-1      (or (:hidden (:state      (second output-seq)))
                                        {:cell-state (float-array unit-num)});when first output time (last time of bptt)
                lstm-param-delta (lstm-param-delta model lstm-part-delta x-input self-activation:t-1 self-state:t-1)
                {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
                propagated-hidden-to-hidden-delta:t-1 (->> (map (fn [w d]
                                                                  (gemv (transpose unit-num w) d))
                                                                [block-wr    input-gate-wr     forget-gate-wr    output-gate-wr]
                                                                [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                                           (apply sum))]
            (recur (rest output-items-seq)
                   propagated-hidden-to-hidden-delta:t-1
                   (rest output-seq)
                   lstm-part-delta
                   (:hidden (:state (first output-seq)))
                   (if (nil? output-acc)
                     output-param-delta
                     (merge-with #(if (map? %1); if sparses
                                    (merge-with (fn [accw dw]
                                                  (sum accw dw));w also bias
                                                %1 %2)
                                    (sum %1 %2))
                                 output-acc
                                 output-param-delta))
                   (if (nil? hidden-acc)
                     lstm-param-delta
                     (merge-with #(if (map? %1); if sparses
                                    (merge-with (fn [accw dw]
                                                  (merge-with (fn [acc d]
                                                                (sum acc d))
                                                              accw dw))
                                                %1 %2)
                                    (sum %1 %2))
                                 hidden-acc lstm-param-delta))))
          :else
          {:output-delta output-acc
           :hidden-delta hidden-acc}))))
  )

(defn bptt
  [model encoder-x-seq decoder-x-seq decoder-output-items-seq & [option]]
  (let [gemv (if-let [it (:gemv option)] it default/gemv)
        {:keys [encoder decoder]} model
        encoder-activation (encoder-forward encoder encoder-x-seq option)
        decoder-activation (decoder-forward decoder decoder-x-seq (:activation (last encoder-activation)) decoder-output-items-seq)
        _(pprint encoder-activation)
        _(pprint decoder-activation)
        ;;         decoder-delta (bptt decoder

        ;;         encode-connection-delta
        ]))

(defn init-encoder-decoder-model
  [{:keys [input-type input-items input-size output-type output-items
           encoder-hidden-size decoder-hidden-size embedding embedding-size] :as param}]
  (let [encoder (lstm/init-model (-> param
                                     (dissoc :output-items)
                                     (assoc :hidden-size encoder-hidden-size)))
        decoder (lstm/init-model (-> param
                                     (assoc :encoder-size encoder-hidden-size :hidden-size decoder-hidden-size)))
        {:keys [output hidden]} decoder
        d-output (reduce (fn [acc [word param]]
                           (assoc acc word (assoc param :encoder-w (random-array encoder-hidden-size) :previous-input-w (random-array embedding-size))))
                         {}
                         output)
        d-hidden (assoc hidden
                   :block-we       (random-array (* decoder-hidden-size encoder-hidden-size))
                   :input-gate-we  (random-array (* decoder-hidden-size encoder-hidden-size))
                   :forget-gate-we (random-array (* decoder-hidden-size encoder-hidden-size))
                   :output-gate-we (random-array (* decoder-hidden-size encoder-hidden-size)))]
    {:encoder encoder :decoder (assoc decoder :hidden d-hidden :output d-output)}))

