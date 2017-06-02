(ns prism.nn.rnn
  (require [prism.nn.rnn.lstm :as lstm]))

(defn hidden-activation
  [model x-input recurrent-input-list previous-cell-state]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard :fixme
      :lstm (lstm/lstm-activation model x-input recurrent-input-list previous-cell-state)
      :gru :fixme)))

(defn forward
  [model x-seq output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard :fixme
      :lstm (lstm/forward model x-seq output-items-seq)
      :gru :fixme)))

(defn bptt
  [model activation output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard :fixme
      :lstm (lstm/bptt model activation output-items-seq)
      :gru :fixme)))

(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard :fixme
      :lstm (lstm/update-model! model param-delta-list learning-rate)
      :gru :fixme)))

(defn init-model
  [{:keys [rnn-type] :as params}]
  (condp = rnn-type
    :standard :fixme
    :lstm (lstm/init-model params)
    :gru :fixme
    (throw (Exception. "rnn-type was not specified"))))



