(ns prism.nn.encoder-decoder.lstm-attention
  (:require
    [clojure.core.matrix :refer [add add! sub sub! emap esum emul emul! mmul div outer-product transpose array matrix dot exp] :as m]
    [clojure.core.matrix.operators :as o]
    ;;     [prism.unit :refer []]
    [prism.util :as util]
    [prism.unit :as unit]
    [prism.nn.rnn.lstm :as rnn]
    [prism.nn.encoder-decoder.lstm :as ed]))


(defn alignment
  [encoder-context]
  (let [hidden-activation-matrix (->> encoder-context (map #(-> % :hidden :activation )) matrix)
        u (m/exp hidden-activation-matrix)
        sumu (apply add u)
        w (div u sumu)
        context (emul hidden-activation-matrix w)]
    {:activation (apply add context)
     :w w}))

(defn encoder-decoder-forward
  [encoder-decoder-model encoder-x-seq decoder-x-seq decoder-output-items-seq]
  (let [{:keys [encoder decoder]} encoder-decoder-model
        encoder-context (rnn/context encoder encoder-x-seq)
        encoder-alignment (alignment encoder-context)
        decoder-activation (ed/decoder-forward decoder decoder-x-seq (:activation encoder-alignment) decoder-output-items-seq)]
    {:encoder encoder-context
     :encoder-alignment encoder-alignment
     :decoder decoder-activation}))


(defn encoder-bptt
  [encoder encoder-activation encoder-alignment propagated-delta-from-decoder]
  (let [{:keys [hidden hidden-size output]} encoder
        {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr
                input-gate-peephole forget-gate-peephole output-gate-peephole]} hidden
        alignment-delta-seq (emul (:w encoder-alignment) propagated-delta-from-decoder)]
    ;looping latest to old
    (loop [propagated-hidden-to-hidden-delta propagated-delta-from-decoder,
           alignment-delta-seq (reverse alignment-delta-seq)
           self-delta:t+1 (rnn/lstm-delta-zeros hidden-size),
           lstm-state:t+1 (rnn/gate-zeros hidden-size),
           output-seq (reverse encoder-activation),
           hidden-acc nil]
      (if (first output-seq)
        (let [lstm-activation (-> output-seq first  :hidden :activation)
              lstm-state (-> output-seq first :hidden :state)
              cell-state:t-1 (if (second output-seq)
                               (:cell-state (:state (:hidden (second output-seq))))
                               (array :vectorz (repeat hidden-size 0)))
              alignment-delta (first alignment-delta-seq)
              lstm-delta (rnn/lstm-part-delta hidden-size propagated-hidden-to-hidden-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                              input-gate-peephole forget-gate-peephole output-gate-peephole)
              x-input (:input (first output-seq))
              hidden:t-1 (if (second output-seq)
                           (-> output-seq second :hidden :activation)
                           (array :vectorz (repeat hidden-size 0)))
              self-state:t-1 (if (second output-seq)
                               (:state (:hidden (second output-seq)))
                               {:cell-state (array :vectorz (repeat hidden-size 0))})
              lstm-param-delta (rnn/lstm-param-delta encoder lstm-delta x-input hidden:t-1 self-state:t-1)
              {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-delta
              propagated-h2h-delta:t-1 (->> (map (fn [w d]
                                                   (mmul (transpose w) d))
                                                 [block-wr    input-gate-wr     forget-gate-wr    output-gate-wr]
                                                 [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                            (apply add!))]
          (recur propagated-h2h-delta:t-1
                 (rest alignment-delta-seq)
                 lstm-delta
                 (:state (:hidden (first output-seq)))
                 (rest output-seq)
                 (unit/merge-param! hidden-acc lstm-param-delta)))
        {:hidden-delta hidden-acc}))))


(defn encoder-decoder-bptt
  [encoder-decoder-model encoder-decoder-forward decoder-output-items-seq]
  (let [{:keys [encoder decoder]} encoder-decoder-model
        {encoder-alignment :encoder-alignment encoder-activation :encoder decoder-activation :decoder} encoder-decoder-forward
        {loss :loss decoder-param-delta :param-loss} (ed/decoder-bptt decoder decoder-activation (:activation  encoder-alignment) decoder-output-items-seq)
        encoder-param-delta (encoder-bptt encoder encoder-activation encoder-alignment (:encoder-delta decoder-param-delta))]
    {:loss loss
     :param-loss {:encoder-param-delta encoder-param-delta :decoder-param-delta decoder-param-delta}}))

