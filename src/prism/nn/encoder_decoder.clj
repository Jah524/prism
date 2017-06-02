(ns prism.nn.encoder-decoder
  (require [prism.nn.encoder-decoder.lstm :as lstm]))

(defn forward
  [model x-seq output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :lstm (lstm/encoder-decoder-forward model x-seq output-items-seq)
      :gru :fixme)))

(defn bptt
  [model activation output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :lstm (lstm/encoder-decoder-bptt model activation output-items-seq)
      :gru :fixme)))

(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :lstm (lstm/update-encoder-decoder! model param-delta-list learning-rate)
      :gru :fixme)))

(defn init-model
  [{:keys [rnn-type] :as params}]
  (condp = rnn-type
    :lstm (lstm/init-encoder-decoder-model params)
    :gru :fixme
    (throw (Exception. "rnn-type was not specified"))))



