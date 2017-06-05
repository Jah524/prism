(ns prism.nn.rnn.standard
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.core.matrix :refer [add add! sub sub! emap esum scale emul emul! mmul outer-product transpose array dot]]
    [prism.nn.feedforward :as ff]
    [prism.unit :refer [sigmoid tanh clip! init-orthogonal-matrix init-vector init-matrix rewrite! activation derivative error merge-param!]]
    [prism.util :as util]))


(defn forward-fixed-time
  [model x-input hidden:t-1 sparse-outputs]
  (let [{:keys [hidden]} model
        {:keys [w wr bias]} hidden
        activation-function (:activation hidden)
        state (add (if (or (set? x-input) (map? x-input))
                     (ff/hidden-state-by-sparse model x-input bias)
                     (add (mmul w x-input) bias))
                   (mmul wr hidden:t-1))
        hidden-activation (activation state activation-function)
        output-activation (if (= :skip sparse-outputs) :skipped (ff/output-activation model hidden-activation sparse-outputs))]
    {:activation {:input x-input :hidden hidden-activation :output output-activation}
     :state      {:hidden state}}))


(defn forward
  [model x-seq output-items-seq]
  (let [{:keys [hidden hidden-size]} model]
    (loop [x-seq x-seq,
           output-items-seq output-items-seq,
           previous-hidden-output (array :vectorz (repeat hidden-size 0)),
           acc []]
      (if-let [x-list (first x-seq)]
        (let [{:keys [activation state] :as model-output} (forward-fixed-time model x-list previous-hidden-output (first output-items-seq))]
          (recur (rest x-seq)
                 (rest output-items-seq)
                 (:hidden activation)
                 (cons model-output acc)))
        (vec (reverse acc))))))

(defn context [model x-seq]
  (let [{:keys [hidden hidden-size]} model]
    (loop [x-seq x-seq,
           hidden:t-1 (array :vectorz (repeat hidden-size 0)),
           acc []]
      (if-let [x-input (first x-seq)]
        (let [{:keys [activation state] :as model-output} (forward-fixed-time model x-input hidden:t-1 :skip)]
          (recur (rest x-seq)
                 (:hidden activation)
                 (cons {:input x-input :hidden model-output} acc)))
        (vec (reverse acc))))))

(defn bptt
  [model activation output-items-seq]
  (let [{:keys [output hidden hidden-size output-type]} model
        {:keys [w wr]} hidden]
    ;looping latest to old
    (loop [output-items-seq (reverse output-items-seq),
           propagated-hidden-to-hidden-delta nil,
           output-seq (reverse activation),
           output-loss [],
           output-acc nil,
           hidden-acc nil]
      (cond
        (and (= :skip (first output-items-seq)) (nil? propagated-hidden-to-hidden-delta))
        (recur (rest output-items-seq)
               nil
               (rest output-seq)
               output-loss
               nil
               nil)
        (first output-seq)
        (let [output-delta (error output-type (:output (:activation (first output-seq))) (first output-items-seq))
              output-param-delta (ff/output-param-delta output-delta hidden-size (:hidden (:activation (first output-seq))))
              propagated-output-to-hidden-delta (when-not (= :skip (first output-items-seq))
                                                  (->> output-delta
                                                       (map (fn [[item delta]]
                                                              (let [w (:w (get output item))]
                                                                (emul delta w))))
                                                       (apply add!)
                                                       (clip! 100)))
              ;merging delta: hidden-to-hidden + above-to-hidden
              summed-propagated-delta (cond (and (not= :skip (first output-items-seq)) propagated-hidden-to-hidden-delta)
                                            (add! propagated-hidden-to-hidden-delta propagated-output-to-hidden-delta)
                                            (= :skip (first output-items-seq))
                                            propagated-hidden-to-hidden-delta
                                            (nil? propagated-hidden-to-hidden-delta)
                                            propagated-output-to-hidden-delta)
              ;hidden delta
              hidden-state (:hidden (:state (first output-seq)))
              hidden-delta (emul (derivative hidden-state (:activation hidden))
                                 summed-propagated-delta)
              x-input (:input (:activation (first output-seq)))
              hidden:t-1  (if (second output-seq)
                            (-> (second output-seq) :activation :hidden)
                            (array :vectorz (repeat hidden-size 0)))
              h2h-param-delta {:wr-delta (outer-product hidden-delta hidden:t-1)}
              i2h-param-delta (if (or (set? x-input) (map? x-input))
                                (ff/param-delta:sparse model hidden-delta x-input hidden-size)
                                (ff/param-delta hidden-delta x-input))
              hidden-param-delta (merge h2h-param-delta i2h-param-delta)
              propagated-h2h-delta:t-1 (mmul (transpose wr) hidden-delta)]
          (recur (rest output-items-seq)
                 propagated-h2h-delta:t-1
                 (rest output-seq)
                 (cons output-delta output-loss)
                 (merge-param! output-acc output-param-delta)
                 (merge-param! hidden-acc hidden-param-delta)))
        :else
        {:param-loss  {:output-delta output-acc
                       :hidden-delta hidden-acc}
         :loss output-loss}))))


(defn update-model! [model param-delta learning-rate]
  (let [{:keys [output hidden]} model
        {:keys [output-delta hidden-delta]} param-delta]
    ;; update output
    (->> output-delta
         (map (fn [[item {:keys [w-delta bias-delta]}]]
                (let [{:keys [w bias]} (get output item)]
                  ;update output w
                  (rewrite! learning-rate w w-delta)
                  ;update output bias
                  (rewrite! learning-rate bias bias-delta))))
         dorun)
    ;; update hidden
    (let [{:keys [sparses w bias wr]} hidden
          {:keys [sparses-delta w-delta bias-delta wr-delta]} hidden-delta]
      (->> sparses-delta
           (map (fn [[k v]]
                  (let [word-w (get sparses k)]
                    ;; update hidden w
                    (rewrite! learning-rate word-w v))))
           dorun)
      ;; update input to hidden hidden connection
      (when w-delta (rewrite! learning-rate w w-delta))
      ;; update hidden to hidden connection
      (rewrite! learning-rate wr wr-delta)
      ;; update hidden bias
      (rewrite! learning-rate bias bias-delta)))
  model)


(defn init-model
  [{:keys [input-items input-size hidden-size output-type output-items activation]}]
  {:hidden {:w (when input-size (init-matrix input-size hidden-size))
            :bias (init-vector hidden-size)
            :wr (init-orthogonal-matrix hidden-size)
            :sparses (reduce (fn [acc sparse]
                               (assoc acc sparse (init-vector hidden-size)))
                             {} input-items)
            :activation activation}
   :output (reduce (fn [acc sparse]
                     (assoc acc sparse {:w (init-vector hidden-size) :bias (init-vector 1)}))
                   {}
                   output-items)
   :input-size input-size
   :hidden-size hidden-size
   :output-type output-type
   :rnn-type :standard})
