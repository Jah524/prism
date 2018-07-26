(ns prism.nn.rnn.gru
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.core.matrix :refer [add add! emap esum sub sub! scale scale! emul emul! mmul outer-product transpose array dot] :as m]
    [prism.nn.feedforward :as ff]
    [prism.unit :refer [sigmoid tanh clip! init-orthogonal-matrix init-vector init-matrix rewrite! activation derivative error merge-param!]]
    [prism.util :as util]))


(defn partial-state-sparse
  [x-input sparses]
  (->> x-input
       (mapv (fn [item]
               (cond (set? x-input)
                     (let [{:keys [w update-gate-w reset-gate-w]} (get sparses item)]
                       [w update-gate-w reset-gate-w])
                     (map? x-input)
                     (let [[sparse-k v] item
                           {:keys [w update-gate-w reset-gate-w]} (get sparses sparse-k)]
                       [(scale w v) (scale update-gate-w v) (scale reset-gate-w v)]))))
       (apply mapv add!)))

(defn gru-activation
  [model x-input hidden:t-1]
  (let [{:keys [hidden hidden-size]} model
        {:keys [w wr bias
                update-gate-w update-gate-wr update-gate-bias
                reset-gate-w reset-gate-wr reset-gate-bias
                sparses]} hidden
        [unit' update-gate' reset-gate'] (if (or (set? x-input) (map? x-input))
                                           (partial-state-sparse x-input sparses)
                                           (let [{:keys [w update-gate-w reset-gate-w]} hidden
                                                 gru-mat [w update-gate-w reset-gate-w]]
                                             (mapv #(mmul % x-input) gru-mat)))
        update-gate-state (add update-gate' (mmul update-gate-wr hidden:t-1) update-gate-bias)
        reset-gate-state  (add reset-gate'  (mmul reset-gate-wr hidden:t-1)  reset-gate-bias)
        update-gate (m/logistic update-gate-state)
        reset-gate  (m/logistic reset-gate-state)
        h-state (add! (mmul wr (emul reset-gate hidden:t-1))
                      unit'
                      bias)
        h (m/tanh h-state)
        gru (add! (emul h update-gate)
                  (emul! (sub! (array :vectorz (repeat hidden-size 1)) update-gate)
                         hidden:t-1))]
    {:activation  {:gru gru :update-gate update-gate :reset-gate reset-gate :h h}
     :state       {:update-gate update-gate-state :reset-gate reset-gate-state :h-state h-state}}))

(defn gru-fixed-time
  [model x-input sparse-outputs hidden:t-1]
  (let [{:keys [activation state]} (gru-activation model x-input hidden:t-1)
        output (if (= :skip sparse-outputs) :skipped (ff/output-activation model (:gru activation) sparse-outputs))]
    {:activation  {:input x-input :hidden activation :output output}
     :state       {:input x-input :hidden state}}))

(defn forward
  [model x-seq output-items-seq]
  (let [{:keys [hidden hidden-size]} model]
    (loop [x-seq x-seq,
           output-items-seq output-items-seq,
           hidden:t-1 (array :vectorz (repeat hidden-size 0)),
           acc []]
      (if-let [x-list (first x-seq)]
        (let [{:keys [activation state] :as model-output} (gru-fixed-time model x-list (first output-items-seq) hidden:t-1)]
          (recur (rest x-seq)
                 (rest output-items-seq)
                 (:gru (:hidden activation))
                 (cons model-output acc)))
        (vec (reverse acc))))))

(defn context [model x-seq]
  (let [{:keys [hidden hidden-size]} model]
    (loop [x-seq x-seq,
           hidden:t-1 (array :vectorz (repeat hidden-size 0)),
           acc []]
      (if-let [x-input (first x-seq)]
        (let [{:keys [activation state] :as model-output} (gru-activation model x-input hidden:t-1)]
          (recur (rest x-seq)
                 (:gru activation)
                 (cons {:input x-input :hidden model-output} acc)))
        (vec (reverse acc))))))

(defn gru-delta
  "propagation through a gru unit"
  [model propagated-delta gru-activation gru-state hidden:t-1]
  (let [{:keys [hidden hidden-size]} model
        {:keys [wr update-gate-wr reset-gate-wr]} hidden
        {:keys [update-gate reset-gate h]} gru-activation
        {update-gate-state :update-gate reset-gate-state :reset-gate h-state :h-state} gru-state
        ones (array :vectorz (repeat hidden-size 1))
        update-gate-delta1 (sub ones (emul hidden:t-1 propagated-delta))
        update-gate-delta2 (emul h propagated-delta)
        update-gate-delta (emul (add update-gate-delta1 update-gate-delta2)
                                (derivative update-gate-state :sigmoid))
        h-delta (emul (derivative h-state :tanh)
                      update-gate
                      propagated-delta)
        tmp-delta (mmul (transpose wr) h-delta)
        reset-gate-delta (emul tmp-delta
                               hidden:t-1
                               (derivative reset-gate-state :sigmoid))
        hidden:t-1-delta (add! (emul (sub ones update-gate)
                                     propagated-delta)
                               (mmul (transpose update-gate-wr) update-gate-delta)
                               (mmul (transpose reset-gate-wr) reset-gate-delta)
                               (emul tmp-delta reset-gate))]
    {:update-gate-delta update-gate-delta
     :reset-gate-delta reset-gate-delta
     :unit-delta h-delta
     :hidden:t-1-delta hidden:t-1-delta}))

(defn param-delta-sparse
  [x-input unit-delta update-gate-delta reset-gate-delta]
  (reduce (fn [acc sparse]
            (cond (set? x-input)
                  (assoc acc sparse {:w-delta             unit-delta
                                     :update-gate-w-delta update-gate-delta
                                     :reset-gate-w-delta  reset-gate-delta})
                  (map? x-input)
                  (let [[sparse-k v] sparse]
                    (assoc acc sparse-k {:w-delta             (scale unit-delta v)
                                         :update-gate-w-delta (scale update-gate-delta v)
                                         :reset-gate-w-delta  (scale reset-gate-delta v)}))))
          {}
          x-input))

(defn gru-param-delta
  [model gru-delta x-input hidden:t-1]
  (let [{:keys [hidden hidden-size]} model
        {:keys [sparses]} hidden
        {:keys [unit-delta update-gate-delta reset-gate-delta]} gru-delta
        template {:wr-delta (outer-product unit-delta hidden:t-1)
                  :update-gate-wr-delta (outer-product update-gate-delta hidden:t-1)
                  :reset-gate-wr-delta (outer-product reset-gate-delta hidden:t-1)
                  :bias-delta unit-delta
                  :update-gate-bias-delta update-gate-delta
                  :reset-gate-bias-delta reset-gate-delta}]
    (if (or (set? x-input) (map? x-input))
      (-> template (assoc :sparses-delta (param-delta-sparse x-input unit-delta update-gate-delta reset-gate-delta)))
      (assoc template
        :w-delta             (outer-product unit-delta x-input)
        :update-gate-w-delta (outer-product update-gate-delta x-input)
        :reset-gate-w-delta  (outer-product reset-gate-delta x-input)))))


(defn bptt
  [model activation output-items-seq]
  (let [{:keys [output hidden hidden-size output-type]} model
        {:keys [wr update-gate-wr reset-gate-wr]} hidden]
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
              output-param-delta (ff/output-param-delta output-delta hidden-size (:gru (:hidden (:activation (first output-seq)))))
              propagated-output-to-hidden-delta (when-not (= :skip (first output-items-seq))
                                                  (->> output-delta
                                                       (map (fn [[item delta]]
                                                              (let [w (:w (get output item))]
                                                                (emul delta w))))
                                                       (apply add!)
                                                       (clip! 1)))
              ;merging delta: hidden-to-hidden + above-to-hidden
              summed-propagated-delta (cond (and (not= :skip (first output-items-seq)) propagated-hidden-to-hidden-delta)
                                            (add! propagated-hidden-to-hidden-delta propagated-output-to-hidden-delta)
                                            (= :skip (first output-items-seq))
                                            propagated-hidden-to-hidden-delta
                                            (nil? propagated-hidden-to-hidden-delta)
                                            propagated-output-to-hidden-delta)
              ;hidden delta
              gru-state (-> output-seq first :state :hidden)
              gru-activation (-> output-seq first :activation :hidden)
              hidden:t-1 (if (second output-seq)
                           (-> output-seq second :activation :hidden :gru)
                           (array :vectorz (repeat hidden-size 0)))
              gru-delta (gru-delta model summed-propagated-delta gru-activation gru-state hidden:t-1)
              x-input (:input (:activation (first output-seq)))
              gru-param-delta (gru-param-delta model gru-delta x-input hidden:t-1)]
          (recur (rest output-items-seq)
                 (:hidden:t-1-delta gru-delta)
                 (rest output-seq)
                 (cons output-delta output-loss)
                 (merge-param! output-acc output-param-delta)
                 (merge-param! hidden-acc gru-param-delta)))
        :else
        {:param-loss  {:output-delta output-acc
                       :hidden-delta hidden-acc}
         :loss output-loss}))))


(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [output hidden]} model
        {:keys [output-delta hidden-delta]} param-delta-list
        {:keys [w-delta wr-delta bias-delta
                update-gate-w-delta update-gate-wr-delta update-gate-bias-delta
                reset-gate-w-delta reset-gate-wr-delta reset-gate-bias-delta
                sparses-delta]} hidden-delta
        {:keys [w wr bias
                update-gate-w update-gate-wr update-gate-bias
                reset-gate-w reset-gate-wr reset-gate-bias
                sparses]} hidden]
    ;update output connection
    (->> output-delta
         (map (fn [[item {:keys [w-delta bias-delta]}]]
                (let [{:keys [w bias]} (get output item)]
                  (rewrite! learning-rate bias bias-delta)
                  (rewrite! learning-rate w w-delta))))
         dorun)
    ;update input connection
    (->> sparses-delta
         vec
         (mapv (fn [[word gru-w-delta]]
                 (let [{:keys [w-delta update-gate-w-delta reset-gate-w-delta]} gru-w-delta
                       {:keys [w update-gate-w reset-gate-w]} (get sparses word)]
                   (rewrite! learning-rate w w-delta)
                   (rewrite! learning-rate update-gate-w update-gate-w-delta)
                   (rewrite! learning-rate reset-gate-w  reset-gate-w-delta))))
         dorun)
    (when w-delta       (rewrite! learning-rate w w-delta))
    (when update-gate-w-delta (rewrite! learning-rate update-gate-w update-gate-w-delta))
    (when reset-gate-w-delta  (rewrite! learning-rate reset-gate-w  reset-gate-w-delta))
    ;update recurrent connection
    (rewrite! learning-rate  wr  wr-delta)
    (rewrite! learning-rate  update-gate-wr  update-gate-wr-delta)
    (rewrite! learning-rate  reset-gate-wr   reset-gate-wr-delta)
    ;update lstm bias and peephole
    (rewrite! learning-rate bias bias-delta)
    (rewrite! learning-rate update-gate-bias update-gate-bias-delta)
    (rewrite! learning-rate reset-gate-bias reset-gate-bias-delta)
    model))


(defn init-model
  [{:keys [input-items input-size hidden-size output-type output-items]}]
  {:hidden {:w                (when input-size (init-matrix input-size hidden-size))
            :wr               (init-orthogonal-matrix hidden-size)
            :bias             (init-vector hidden-size)
            :update-gate-w    (when input-size (init-matrix input-size hidden-size))
            :update-gate-wr   (init-orthogonal-matrix hidden-size)
            :update-gate-bias (init-vector hidden-size)
            :reset-gate-w     (when input-size (init-matrix input-size hidden-size))
            :reset-gate-wr    (init-orthogonal-matrix hidden-size)
            :reset-gate-bias  (init-vector hidden-size)
            :sparses          (reduce (fn [acc sparse]
                                        (assoc acc sparse {:w             (init-vector hidden-size)
                                                           :update-gate-w (init-vector hidden-size)
                                                           :reset-gate-w  (init-vector hidden-size)}))
                                      {} input-items)}
   :output (reduce (fn [acc sparse]
                     (assoc acc sparse {:w (init-vector hidden-size) :bias (init-vector 1)}))
                   {}
                   output-items)
   :input-size input-size
   :hidden-size hidden-size
   :output-type output-type
   :rnn-type :gru})
