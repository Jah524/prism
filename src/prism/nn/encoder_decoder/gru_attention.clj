(ns prism.nn.encoder-decoder.gru-attention
  (:require
    [clojure.core.matrix :refer [add add! sub sub! emap esum emul emul! mmul div outer-product transpose array matrix dot exp] :as m]
    [clojure.core.matrix.operators :as o]
    ;;     [prism.unit :refer []]
    [prism.util :as util]
    [prism.unit :as unit]
    [prism.nn.rnn.gru :as rnn]
    [prism.nn.encoder-decoder.gru :as ed]))


(defn alignment
  [encoder-context]
  (let [hidden-activation-matrix (->> encoder-context (map #(-> % :hidden :activation :gru)) matrix)
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
        {:keys [wr update-gate-wr reset-gate-wr]} hidden
        alignment-delta-seq (emul (:w encoder-alignment) propagated-delta-from-decoder)]
    ;looping latest to old
    (loop [propagated-hidden-to-hidden-delta propagated-delta-from-decoder,
           alignment-delta-seq (reverse alignment-delta-seq)
           output-seq (reverse encoder-activation),
           hidden-acc nil]
      (if (first output-seq)
        (let [gru-activation (-> output-seq first  :hidden :activation)
              gru-state (-> output-seq first :hidden :state)
              hidden:t-1 (if (second output-seq)
                           (-> output-seq second :hidden :activation :gru)
                           (array :vectorz (repeat hidden-size 0)))
              alignment-delta (first alignment-delta-seq)
              gru-delta (rnn/gru-delta encoder (add propagated-hidden-to-hidden-delta alignment-delta) gru-activation gru-state hidden:t-1 )
              x-input (:input (first output-seq))
              gru-param-delta (rnn/gru-param-delta encoder gru-delta x-input hidden:t-1)]
          (recur (:hidden:t-1-delta gru-delta)
                 (rest alignment-delta-seq)
                 (rest output-seq)
                 (unit/merge-param! hidden-acc gru-param-delta)))
        {:hidden-delta hidden-acc}))))


(defn encoder-decoder-bptt
  [encoder-decoder-model encoder-decoder-forward decoder-output-items-seq]
  (let [{:keys [encoder decoder]} encoder-decoder-model
        {encoder-alignment :encoder-alignment encoder-activation :encoder decoder-activation :decoder} encoder-decoder-forward
        {loss :loss decoder-param-delta :param-loss} (ed/decoder-bptt decoder decoder-activation (:activation  encoder-alignment) decoder-output-items-seq)
        encoder-param-delta (encoder-bptt encoder encoder-activation encoder-alignment (:encoder-delta decoder-param-delta))]
    {:loss loss
     :param-loss {:encoder-param-delta encoder-param-delta :decoder-param-delta decoder-param-delta}}))


(defn init-encoder-decoder-model
  [params]
  (assoc (ed/init-encoder-decoder-model params)
    :rnn-type :gru-attention))
