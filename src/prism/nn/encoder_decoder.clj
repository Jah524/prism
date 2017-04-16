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

(defn decoder-output-time-fixed
  [model x-input sparse-outputs previous-hidden-output encoder-input previous-cell-state & [lstm-option]]
  (let [{:keys [activation state]} (decoder-lstm-activation model x-input previous-hidden-output encoder-input previous-cell-state lstm-option)
        output (if (= :skip sparse-outputs) :skipped (lstm/output-activation model activation sparse-outputs lstm-option))]
    {:activation {:input x-input :hidden activation :output output}
     :state  {:input x-input :hidden state}}))

(defn decoder-forward [model x-seq encode-input output-items-seq & [lstm-option]]
  (let [hidden-size (:unit-num (:hidden model))]
    (loop [x-seq x-seq,
           output-items-seq output-items-seq,
           previous-hidden-output (float-array hidden-size),
           previous-cell-state    (float-array hidden-size),
           acc []]
      (if-let [x-list (first x-seq)]
        (let [model-output (decoder-output-time-fixed model x-list (first output-items-seq) previous-hidden-output encode-input previous-cell-state lstm-option)
              {:keys [activation state]} model-output]
          (recur (rest x-seq)
                 (rest output-items-seq)
                 (:hidden activation)
                 (:cell-state (:hidden state))
                 (cons model-output acc)))
        (vec (reverse acc))))))



(defn init-encoder-decoder-model
  [{:keys [input-type input-items input-size output-type output-items
           encoder-hidden-size decoder-hidden-size] :as param}]
  (let [encoder (lstm/init-model (-> param
                                     (dissoc :output-items)
                                     (assoc :hidden-size encoder-hidden-size)))
        decoder (lstm/init-model (-> param
                                     (assoc :encoder-size encoder-hidden-size :hidden-size decoder-hidden-size)))
        d-hidden (assoc (:hidden decoder)
                   :block-we       (random-array (* decoder-hidden-size encoder-hidden-size))
                   :input-gate-we  (random-array (* decoder-hidden-size encoder-hidden-size))
                   :forget-gate-we (random-array (* decoder-hidden-size encoder-hidden-size))
                   :output-gate-we (random-array (* decoder-hidden-size encoder-hidden-size)))]
    {:encoder encoder :decoder (assoc decoder :hidden d-hidden)}))

