(ns prism.nn.rnn
  (require
    [prism.nn.rnn.standard :as s]
    [prism.nn.rnn.lstm :as lstm]))

(defn hidden-activation
  [model x-input recurrent-input-list previous-cell-state]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard (s/forward-fixed-time model x-input recurrent-input-list previous-cell-state)
      :lstm     (lstm/lstm-activation model x-input recurrent-input-list previous-cell-state)
      :gru :fixme)))

(defn forward
  [model x-seq output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard (s/forward model x-seq output-items-seq)
      :lstm     (lstm/forward model x-seq output-items-seq)
      :gru :fixme)))

(defn bptt
  [model activation output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard (s/bptt model activation output-items-seq)
      :lstm     (lstm/bptt model activation output-items-seq)
      :gru :fixme)))

(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard (s/update-model! model param-delta-list learning-rate)
      :lstm     (lstm/update-model! model param-delta-list learning-rate)
      :gru :fixme)))

(defn init-model
  [{:keys [rnn-type] :as params}]
  (condp = rnn-type
    :standard (s/init-model params)
    :lstm     (lstm/init-model params)
    :gru :fixme
    (throw (Exception. "rnn-type was not specified"))))



