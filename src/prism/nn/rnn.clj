(ns prism.nn.rnn
  (:require
    [prism.nn.rnn.standard :as s]
    [prism.nn.rnn.lstm :as lstm]
    [prism.nn.rnn.gru :as gru]))

(defn context
  [model x-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard (-> (s/context model x-seq)    last :hidden :activation :hidden)
      :lstm     (-> (lstm/context model x-seq) last :hidden :activation)
      :gru      (-> (gru/context model x-seq)  last :hidden :activation :gru))))



(defn forward
  [model x-seq output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard (s/forward model x-seq output-items-seq)
      :lstm     (lstm/forward model x-seq output-items-seq)
      :gru      (gru/forward model x-seq output-items-seq))))

(defn bptt
  [model activation output-items-seq]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard (s/bptt model activation output-items-seq)
      :lstm     (lstm/bptt model activation output-items-seq)
      :gru      (gru/bptt model activation output-items-seq))))

(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [rnn-type]} model]
    (condp = rnn-type
      :standard (s/update-model! model param-delta-list learning-rate)
      :lstm     (lstm/update-model! model param-delta-list learning-rate)
      :gru      (gru/update-model!  model param-delta-list learning-rate))))

(defn init-model
  [{:keys [rnn-type] :as params}]
  (condp = rnn-type
    :standard (s/init-model params)
    :lstm     (lstm/init-model params)
    :gru      (gru/init-model params)
    (throw (Exception. "rnn-type was not specified"))))



