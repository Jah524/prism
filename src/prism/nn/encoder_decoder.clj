(ns prism.nn.encoder-decoder
  (require
    [prism.nn.encoder-decoder.lstm :as lstm]
    [prism.nn.encoder-decoder.gru  :as gru]))

(defn encoder-forward
  [encoder encoder-x-seq]
  (let [{:keys [rnn-type]} encoder]
    (condp = rnn-type
      :lstm (lstm/encoder-forward encoder encoder-x-seq)
      :gru  (gru/encoder-forward encoder encoder-x-seq))))


(defn forward
  [model encoder-x-seq decoder-x-seq output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :lstm (lstm/encoder-decoder-forward model encoder-x-seq decoder-x-seq output-items-seq)
      :gru  (gru/encoder-decoder-forward model encoder-x-seq decoder-x-seq output-items-seq))))

(defn bptt
  [model activation output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :lstm (lstm/encoder-decoder-bptt model activation output-items-seq)
      :gru  (gru/encoder-decoder-bptt model activation output-items-seq))))

(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :lstm (lstm/update-encoder-decoder! model param-delta-list learning-rate)
      :gru  (gru/update-encoder-decoder! model param-delta-list learning-rate))))

(defn init-model
  [{:keys [rnn-type] :as params}]
  (condp = rnn-type
    :lstm  (lstm/init-encoder-decoder-model params)
    :gru   (gru/init-encoder-decoder-model params)
    (throw (Exception. "rnn-type was not specified"))))

