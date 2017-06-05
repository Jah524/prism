(ns prism.nn.rnn.lstm
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.core.matrix :refer [add add! sub sub! scale emap esum emul emul! mmul outer-product transpose array dot]]
    [prism.nn.feedforward :as ff]
    [prism.unit :refer [sigmoid tanh clip! init-orthogonal-matrix init-vector init-matrix rewrite! activation derivative error merge-param!]]
    [prism.util :as util]))


(defn partial-state-sparse
  "lstm states of each part caused by input"
  [x-input sparses]
  (->> x-input
       (mapv (fn [item]
               (cond (set? x-input)
                     (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses item)]
                       [block-w input-gate-w forget-gate-w output-gate-w])
                     (map? x-input)
                     (let [[sparse-k v] item
                           {:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses sparse-k)]
                       [(scale block-w v) (scale input-gate-w v) (scale forget-gate-w v) (scale output-gate-w v)]))))
       (apply mapv add!)))

(defn lstm-activation
  [model x-input recurrent-input-list previous-cell-state]
  (let [{:keys [hidden hidden-size]} model
        {:keys [block-wr block-bias input-gate-wr input-gate-bias input-gate-peephole
                forget-gate-wr forget-gate-bias forget-gate-peephole
                output-gate-wr output-gate-bias output-gate-peephole peephole
                sparses]} hidden
        [block' input-gate' forget-gate' output-gate'] (if (or (set? x-input) (map? x-input))
                                                         (partial-state-sparse x-input sparses)
                                                         (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} hidden
                                                               lstm-mat [block-w input-gate-w forget-gate-w output-gate-w]]
                                                           (mapv #(mmul % x-input) lstm-mat)))
        lstm-mat-r  [block-wr input-gate-wr forget-gate-wr output-gate-wr]
        [block-r' input-gate-r' forget-gate-r' output-gate-r'] (mapv #(mmul % recurrent-input-list) lstm-mat-r)
        block       (add block' block-r' block-bias)
        input-gate  (add input-gate' input-gate-r' input-gate-bias    (emul input-gate-peephole  previous-cell-state))
        forget-gate (add forget-gate' forget-gate-r' forget-gate-bias (emul forget-gate-peephole previous-cell-state))
        output-gate (add output-gate' output-gate-r' output-gate-bias (emul output-gate-peephole previous-cell-state))
        cell-state  (add (emul (emap tanh block) (emap sigmoid input-gate))
                         (emul (emap sigmoid forget-gate) previous-cell-state))
        lstm  (emul (emap sigmoid output-gate) (emap tanh cell-state))]
    {:activation lstm
     :state {:lstm lstm :block block :input-gate input-gate :forget-gate forget-gate :output-gate output-gate :cell-state cell-state}}))


(defn lstm-model-output
  [model x-input sparse-outputs previous-hidden-output previous-cell-state]
  (let [{:keys [activation state] :as lstm} (lstm-activation model x-input previous-hidden-output previous-cell-state)
        output (if (= :skip sparse-outputs) :skipped (ff/output-activation model activation sparse-outputs))]
    {:activation {:input x-input :hidden activation :output output}
     :state  {:input x-input :hidden state}}))

(defn forward
  [model x-seq output-items-seq]
  (let [{:keys [hidden hidden-size]} model]
    (loop [x-seq x-seq,
           output-items-seq output-items-seq,
           previous-hidden-output (array :vectorz (repeat hidden-size 0)),
           previous-cell-state    (array :vectorz (repeat hidden-size 0)),
           acc []]
      (if-let [x-list (first x-seq)]
        (let [model-output (lstm-model-output model x-list (first output-items-seq) previous-hidden-output previous-cell-state)
              {:keys [activation state]} model-output]
          (recur (rest x-seq)
                 (rest output-items-seq)
                 (:hidden activation)
                 (:cell-state (:hidden state))
                 (cons model-output acc)))
        (vec (reverse acc))))))

(defn context [model x-seq]
  (let [{:keys [hidden hidden-size]} model]
    (loop [x-seq x-seq,
           previous-activation (array :vectorz (repeat hidden-size 0)),
           previous-cell-state (array :vectorz (repeat hidden-size 0)),
           acc []]
      (if-let [x-input (first x-seq)]
        (let [model-output (lstm-activation model x-input previous-activation previous-cell-state)
              {:keys [activation state]} model-output]
          (recur (rest x-seq)
                 activation
                 (:cell-state state)
                 (cons {:input x-input :hidden model-output} acc)))
        (vec (reverse acc))))))


;;;;    Back Propagation Through Time    ;;;;

(defn lstm-part-delta
  "propagation through a lstm unit"
  [hidden-size propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
   peephole-w-input-gate peephole-w-forget-gate peephole-w-output-gate]
  (let [_dog (derivative (:output-gate lstm-state) :sigmoid)
        _cell-state (:cell-state lstm-state)
        output-gate-delta (emul _dog (emap tanh _cell-state) propagated-delta)
        _og (:output-gate lstm-state)
        _dcs (derivative (:cell-state lstm-state) :tanh)
        _fg:t+1 (:forget-gate lstm-state:t+1)
        _csd:t+1 (:cell-state-delta self-delta:t+1)
        _igd:t+1 (:input-gate-delta self-delta:t+1)
        _fgd:t+1 (:forget-gate-delta self-delta:t+1)
        cell-state-delta (add (emul (emap sigmoid _og) _dcs propagated-delta)
                              (emul (emap sigmoid _fg:t+1) _csd:t+1)
                              (emul peephole-w-input-gate _igd:t+1)
                              (emul peephole-w-forget-gate _fgd:t+1)
                              (emul peephole-w-output-gate output-gate-delta))
        _ig (:input-gate lstm-state)
        _db (derivative (:block lstm-state) :tanh)
        block-delta (emul (emap sigmoid _ig) _db cell-state-delta)
        _dfg (derivative (:forget-gate lstm-state) :sigmoid)
        forget-gate-delta (emul _dfg cell-state:t-1 cell-state-delta)
        _dig (derivative (:input-gate lstm-state) :sigmoid)
        _b (:block lstm-state)
        input-gate-delta (emul _dig (emap tanh _b) cell-state-delta)]
    {:output-gate-delta output-gate-delta :cell-state-delta cell-state-delta :block-delta block-delta
     :forget-gate-delta forget-gate-delta :input-gate-delta input-gate-delta}))

(defn param-delta-sparse
  [x-input block-delta input-gate-delta forget-gate-delta output-gate-delta]
  (reduce (fn [acc sparse]
            (cond (set? x-input)
                  (assoc acc sparse {:block-w-delta block-delta
                                     :input-gate-w-delta  input-gate-delta
                                     :forget-gate-w-delta forget-gate-delta
                                     :output-gate-w-delta output-gate-delta})
                  (map? x-input)
                  (let [[sparse-k v] sparse]
                    (assoc acc sparse-k {:block-w-delta       (scale block-delta v)
                                         :input-gate-w-delta  (scale input-gate-delta v)
                                         :forget-gate-w-delta (scale forget-gate-delta v)
                                         :output-gate-w-delta (scale output-gate-delta v)}))))
          {}
          x-input))

(defn lstm-param-delta
  [model lstm-part-delta x-input self-activation:t-1 self-state:t-1]
  (let [{:keys [hidden hidden-size]} model
        {:keys [sparses]} hidden
        {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
        block-wr-delta       (outer-product block-delta self-activation:t-1)
        input-gate-wr-delta  (outer-product input-gate-delta self-activation:t-1)
        forget-gate-wr-delta (outer-product forget-gate-delta self-activation:t-1)
        output-gate-wr-delta (outer-product output-gate-delta self-activation:t-1)
        peephole-input-gate  (emul input-gate-delta  (:cell-state self-state:t-1))
        peephole-forget-gate (emul forget-gate-delta (:cell-state self-state:t-1))
        peephole-output-gate (emul output-gate-delta (:cell-state self-state:t-1))
        template {:block-wr-delta block-wr-delta :input-gate-wr-delta input-gate-wr-delta
                  :forget-gate-wr-delta forget-gate-wr-delta :output-gate-wr-delta output-gate-wr-delta
                  :block-bias-delta block-delta
                  :input-gate-bias-delta input-gate-delta
                  :forget-gate-bias-delta forget-gate-delta
                  :output-gate-bias-delta output-gate-delta
                  :peephole-input-gate-delta peephole-input-gate :peephole-forget-gate-delta peephole-forget-gate
                  :peephole-output-gate-delta peephole-output-gate}
        param-delta (if (or (set? x-input) (map? x-input))
                      (-> template (assoc :sparses-delta (param-delta-sparse x-input block-delta input-gate-delta forget-gate-delta output-gate-delta)))
                      (let [block-w-delta        (outer-product block-delta x-input)
                            input-gate-w-delta   (outer-product input-gate-delta x-input)
                            forget-gate-w-delta  (outer-product forget-gate-delta x-input)
                            output-gate-w-delta  (outer-product output-gate-delta x-input)]
                        (-> template (assoc
                                       :block-w-delta block-w-delta :input-gate-w-delta input-gate-w-delta
                                       :forget-gate-w-delta forget-gate-w-delta :output-gate-w-delta output-gate-w-delta))))]
    param-delta))

(defn lstm-delta-zeros
  [hidden-size]
  {:block-delta       (array :vectorz (repeat hidden-size 0))
   :input-gate-delta  (array :vectorz (repeat hidden-size 0))
   :forget-gate-delta (array :vectorz (repeat hidden-size 0))
   :output-gate-delta (array :vectorz (repeat hidden-size 0))
   :cell-state-delta  (array :vectorz (repeat hidden-size 0))})

(defn gate-zeros
  [hidden-size]
  {:forget-gate (array :vectorz (repeat hidden-size 0))})

(defn bptt
  [model activation output-items-seq]
  (let [{:keys [output hidden hidden-size output-type]} model
        {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr
                input-gate-peephole forget-gate-peephole output-gate-peephole]} hidden]
    ;looping latest to old
    (loop [output-items-seq (reverse output-items-seq),
           propagated-hidden-to-hidden-delta nil,
           output-seq (reverse activation),
           self-delta:t+1 (lstm-delta-zeros hidden-size),
           lstm-state:t+1 (gate-zeros hidden-size),
           output-loss [],
           output-acc nil,
           hidden-acc nil]
      (cond
        (and (= :skip (first output-items-seq)) (nil? propagated-hidden-to-hidden-delta))
        (recur (rest output-items-seq)
               nil
               (rest output-seq)
               (lstm-delta-zeros hidden-size)
               (gate-zeros hidden-size)
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
              lstm-state (:hidden (:state (first output-seq)))
              cell-state:t-1 (or (:cell-state (:hidden (:state (second output-seq)))) (array :vectorz (repeat hidden-size 0)))
              lstm-part-delta (lstm-part-delta hidden-size summed-propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                               input-gate-peephole forget-gate-peephole output-gate-peephole)
              x-input (:input (:activation (first output-seq)))
              self-activation:t-1  (if (second output-seq)
                                     (:hidden (:activation (second output-seq)))
                                     (array :vectorz (repeat hidden-size 0)));when first output time (last time of bptt
              self-state:t-1       (if (second output-seq)
                                     (:hidden (:state (second output-seq)))
                                     {:cell-state (array :vectorz (repeat hidden-size 0))});when first output time (last time of bptt)
              lstm-param-delta (lstm-param-delta model lstm-part-delta x-input self-activation:t-1 self-state:t-1)
              {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
              propagated-hidden-to-hidden-delta:t-1 (->> (map (fn [w d]
                                                                (mmul (transpose w) d))
                                                              [block-wr    input-gate-wr     forget-gate-wr    output-gate-wr]
                                                              [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                                         (apply add!))]
          (recur (rest output-items-seq)
                 propagated-hidden-to-hidden-delta:t-1
                 (rest output-seq)
                 lstm-part-delta
                 (:hidden (:state (first output-seq)))
                 (cons output-delta output-loss)
                 (merge-param! output-acc output-param-delta)
                 (merge-param! hidden-acc lstm-param-delta)))
        :else
        {:param-loss  {:output-delta output-acc
                       :hidden-delta hidden-acc}
         :loss output-loss}))))


(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [output hidden]} model
        {:keys [output-delta hidden-delta]} param-delta-list
        {:keys [block-w-delta block-wr-delta block-bias-delta input-gate-w-delta input-gate-wr-delta input-gate-bias-delta
                forget-gate-w-delta forget-gate-wr-delta forget-gate-bias-delta output-gate-w-delta output-gate-wr-delta output-gate-bias-delta
                peephole-input-gate-delta peephole-forget-gate-delta peephole-output-gate-delta sparses-delta]} hidden-delta
        {:keys [block-w block-wr block-bias input-gate-w input-gate-wr input-gate-bias
                forget-gate-w forget-gate-wr forget-gate-bias output-gate-w output-gate-wr output-gate-bias
                input-gate-peephole forget-gate-peephole output-gate-peephole
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
         (mapv (fn [[word lstm-w-delta]]
                 (let [{:keys [block-w-delta input-gate-w-delta forget-gate-w-delta output-gate-w-delta]} lstm-w-delta
                       {:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses word)]
                   (rewrite! learning-rate block-w block-w-delta)
                   (rewrite! learning-rate input-gate-w input-gate-w-delta)
                   (rewrite! learning-rate forget-gate-w forget-gate-w-delta)
                   (rewrite! learning-rate output-gate-w output-gate-w-delta))))
         dorun)
    (when block-w-delta       (rewrite! learning-rate block-w block-w-delta))
    (when input-gate-w-delta  (rewrite! learning-rate input-gate-w input-gate-w-delta))
    (when forget-gate-w-delta (rewrite! learning-rate forget-gate-w forget-gate-w-delta))
    (when output-gate-w-delta (rewrite! learning-rate output-gate-w output-gate-w-delta))
    ;update recurrent connection
    (rewrite! learning-rate  block-wr  block-wr-delta)
    (rewrite! learning-rate  input-gate-wr  input-gate-wr-delta)
    (rewrite! learning-rate  forget-gate-wr  forget-gate-wr-delta)
    (rewrite! learning-rate  output-gate-wr  output-gate-wr-delta)
    ;update lstm bias and peephole
    (rewrite! learning-rate block-bias block-bias-delta)
    (rewrite! learning-rate input-gate-bias input-gate-bias-delta)
    (rewrite! learning-rate forget-gate-bias forget-gate-bias-delta)
    (rewrite! learning-rate output-gate-bias output-gate-bias-delta)
    (rewrite! learning-rate input-gate-peephole peephole-input-gate-delta)
    (rewrite! learning-rate forget-gate-peephole peephole-forget-gate-delta)
    (rewrite! learning-rate output-gate-peephole peephole-output-gate-delta)
    model))


(defn init-model
  [{:keys [input-items input-size hidden-size output-type output-items]}]
  {:hidden (let [bwr (init-orthogonal-matrix hidden-size);for recurrent connection
                 bb  (init-vector hidden-size)
                 iwr (init-orthogonal-matrix hidden-size)
                 ib  (init-vector hidden-size)
                 ip  (init-vector hidden-size)
                 fwr (init-orthogonal-matrix hidden-size)
                 fb  (array :vectorz (take hidden-size (repeat (float 1))))
                 fp  (init-vector hidden-size)
                 owr (init-orthogonal-matrix hidden-size)
                 ob  (init-vector hidden-size)
                 op  (init-vector hidden-size)
                 template {:block-wr       bwr   :block-bias           bb
                           :input-gate-wr  iwr   :input-gate-bias      ib
                           :forget-gate-wr fwr   :forget-gate-bias     fb
                           :output-gate-wr owr   :output-gate-bias     ob
                           :input-gate-peephole  ip  :forget-gate-peephole fp
                           :output-gate-peephole op}]
             (assoc template
               :sparses (reduce (fn [acc sparse]
                                  (assoc acc sparse {:block-w       (init-vector hidden-size)
                                                     :input-gate-w  (init-vector hidden-size)
                                                     :forget-gate-w (init-vector hidden-size)
                                                     :output-gate-w (init-vector hidden-size)}))
                                {} input-items)
               :block-w       (when input-size (init-matrix input-size hidden-size))
               :input-gate-w  (when input-size (init-matrix input-size hidden-size))
               :forget-gate-w (when input-size (init-matrix input-size hidden-size))
               :output-gate-w (when input-size (init-matrix input-size hidden-size))))
   :output (reduce (fn [acc sparse]
                     (assoc acc sparse {:w (init-vector hidden-size) :bias (init-vector 1)}))
                   {}
                   output-items)
   :input-size input-size
   :hidden-size hidden-size
   :output-type output-type
   :rnn-type :lstm})

