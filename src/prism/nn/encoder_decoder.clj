(ns prism.nn.encoder-decoder
  (require
    [prism.nn.rnn.lstm :as lstmr]
    [prism.nn.rnn.gru :as grur]
    [prism.nn.encoder-decoder.lstm :as lstm]
    [prism.nn.encoder-decoder.gru  :as gru]
    [prism.nn.encoder-decoder.gru-attention :as gruatt]
    [prism.nn.encoder-decoder.lstm-attention :as lstmatt]))

(defn encoder-forward
  [encoder encoder-x-seq]
  (let [{:keys [rnn-type]} encoder]
    (condp = rnn-type
      :lstm (lstmr/context encoder encoder-x-seq)
      :gru  (grur/context encoder encoder-x-seq)
      :gru-attention  (:activation (gruatt/alignment  (grur/context encoder encoder-x-seq)))
      :lstm-attention (:activation (lstmatt/alignment (lstmr/context encoder encoder-x-seq))))))


(defn forward
  [model encoder-x-seq decoder-x-seq output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :lstm (lstm/encoder-decoder-forward model encoder-x-seq decoder-x-seq output-items-seq)
      :gru  (gru/encoder-decoder-forward model encoder-x-seq decoder-x-seq output-items-seq)
      :gru-attention   (gruatt/encoder-decoder-forward model encoder-x-seq decoder-x-seq output-items-seq)
      :lstm-attention (lstmatt/encoder-decoder-forward model encoder-x-seq decoder-x-seq output-items-seq))))

(defn bptt
  [model activation output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :lstm (lstm/encoder-decoder-bptt model activation output-items-seq)
      :gru  (gru/encoder-decoder-bptt model activation output-items-seq)
      :gru-attention (gruatt/encoder-decoder-bptt model activation output-items-seq)
      :lstm-attention (lstmatt/encoder-decoder-bptt model activation output-items-seq))))

(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :lstm    (lstm/update-encoder-decoder! model param-delta-list learning-rate)
      :gru     (gru/update-encoder-decoder! model param-delta-list learning-rate)
      :gru-attention   (gru/update-encoder-decoder!  model param-delta-list learning-rate)
      :lstm-attention  (lstm/update-encoder-decoder! model param-delta-list learning-rate))))

(defn init-model
  [{:keys [rnn-type] :as params}]
  (condp = rnn-type
    :lstm    (lstm/init-encoder-decoder-model params)
    :gru     (gru/init-encoder-decoder-model params)
    :gru-attention   (assoc (gru/init-encoder-decoder-model params)  :rnn-type :gru-attention)
    :lstm-attention  (assoc (lstm/init-encoder-decoder-model params) :rnn-type :lstm-attention)
    (throw (Exception. "rnn-type was not specified"))))

