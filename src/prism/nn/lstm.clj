(ns prism.nn.lstm
  (:require
    [clojure.pprint :refer [pprint]]
    [matrix.default :as default]
    [prism.nn.feedforward :as ff]
    [prism.unit :refer [activation derivative error]]
    [prism.util :as util]))


(defn partial-state-sparse
  "lstm states of each part caused by input"
  [model x-input sparses unit-num]
  (let [{:keys [scal plus]} (:matrix-kit model)]
    (->> x-input
         (mapv (fn [item]
                 (cond (set? x-input)
                       (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses item)]
                         [block-w input-gate-w forget-gate-w output-gate-w])
                       (map? x-input)
                       (let [[sparse-k v] item
                             {:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses sparse-k)]
                         [(scal v block-w) (scal v input-gate-w) (scal v forget-gate-w) (scal v output-gate-w)]))))
         (apply mapv plus))))

(defn lstm-activation
  [model x-input recurrent-input-list previous-cell-state]
  (let [{:keys [hidden matrix-kit]} model
        lstm-layer hidden
        {:keys [gemv plus times sigmoid tanh alter-vec]} matrix-kit
        {:keys [block-wr block-bias input-gate-wr input-gate-bias input-gate-peephole
                forget-gate-wr forget-gate-bias forget-gate-peephole
                output-gate-wr output-gate-bias output-gate-peephole peephole unit-num
                sparses]} lstm-layer
        [block' input-gate' forget-gate' output-gate'] (if (or (set? x-input) (map? x-input))
                                                         (partial-state-sparse model x-input sparses unit-num)
                                                         (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} lstm-layer
                                                               lstm-mat [block-w input-gate-w forget-gate-w output-gate-w]]
                                                           (mapv #(gemv % x-input) lstm-mat)))
        lstm-mat-r  [block-wr input-gate-wr forget-gate-wr output-gate-wr]
        [block-r' input-gate-r' forget-gate-r' output-gate-r'] (mapv #(gemv % recurrent-input-list) lstm-mat-r)
        block       (plus block' block-r' block-bias)
        input-gate  (plus input-gate' input-gate-r' input-gate-bias    (times input-gate-peephole  previous-cell-state))
        forget-gate (plus forget-gate' forget-gate-r' forget-gate-bias (times forget-gate-peephole previous-cell-state))
        output-gate (plus output-gate' output-gate-r' output-gate-bias (times output-gate-peephole previous-cell-state))
        cell-state  (plus (times (alter-vec block tanh) (alter-vec input-gate sigmoid))
                          (times (alter-vec forget-gate sigmoid) previous-cell-state))
        lstm  (times (alter-vec output-gate sigmoid) (alter-vec cell-state tanh))]
    {:activation lstm
     :state {:lstm lstm :block block :input-gate input-gate :forget-gate forget-gate :output-gate output-gate :cell-state cell-state}}))


(defn lstm-model-output
  [model x-input sparse-outputs previous-hidden-output previous-cell-state]
  (let [{:keys [activation state] :as lstm} (lstm-activation model x-input previous-hidden-output previous-cell-state)
        output (if (= :skip sparse-outputs) :skipped (ff/output-activation model activation sparse-outputs))]
    {:activation {:input x-input :hidden activation :output output}
     :state  {:input x-input :hidden state}}))

(defn sequential-output
  [model x-seq output-items-seq]
  (let [{:keys [hidden matrix-kit]} model
        hidden-size (:unit-num hidden)
        {:keys [make-vector]} matrix-kit]
    (loop [x-seq x-seq,
           output-items-seq output-items-seq,
           previous-hidden-output (make-vector (repeat hidden-size 0)),
           previous-cell-state    (make-vector (repeat hidden-size 0)),
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


;;;;    Back Propagation Through Time    ;;;;

(defn lstm-part-delta
  "propagation through a lstm unit"
  [model hidden-size propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
   peephole-w-input-gate peephole-w-forget-gate peephole-w-output-gate]
  (let [{:keys [matrix-kit]} model
        {:keys [plus times alter-vec sigmoid tanh]} matrix-kit
        _dog (derivative (:output-gate lstm-state) :sigmoid matrix-kit)
        _cell-state (:cell-state lstm-state)
        output-gate-delta (times _dog (alter-vec _cell-state tanh) propagated-delta)
        _og (:output-gate lstm-state)
        _dcs (derivative (:cell-state lstm-state) :tanh matrix-kit)
        _fg:t+1 (:forget-gate lstm-state:t+1)
        _csd:t+1 (:cell-state-delta self-delta:t+1)
        _igd:t+1 (:input-gate-delta self-delta:t+1)
        _fgd:t+1 (:forget-gate-delta self-delta:t+1)
        cell-state-delta (plus (times (alter-vec _og sigmoid) _dcs propagated-delta)
                               (times (alter-vec _fg:t+1 sigmoid) _csd:t+1)
                               (times peephole-w-input-gate _igd:t+1)
                               (times peephole-w-forget-gate _fgd:t+1)
                               (times peephole-w-output-gate output-gate-delta))
        _ig (:input-gate lstm-state)
        _db (derivative (:block lstm-state) :tanh matrix-kit)
        block-delta (times (alter-vec _ig sigmoid) _db cell-state-delta)
        _dfg (derivative (:forget-gate lstm-state) :sigmoid matrix-kit)
        forget-gate-delta (times _dfg cell-state:t-1 cell-state-delta)
        _dig (derivative (:input-gate lstm-state) :sigmoid matrix-kit)
        _b (:block lstm-state)
        input-gate-delta (times _dig (alter-vec _b tanh) cell-state-delta)]
    {:output-gate-delta output-gate-delta :cell-state-delta cell-state-delta :block-delta block-delta
     :forget-gate-delta forget-gate-delta :input-gate-delta input-gate-delta}))

(defn param-delta-sparse
  [model x-input block-delta input-gate-delta forget-gate-delta output-gate-delta unit-num]
  (let [{:keys [scal]} (:matrix-kit model)]
    (reduce (fn [acc sparse]
              (cond (set? x-input)
                    (assoc acc sparse {:block-w-delta block-delta
                                       :input-gate-w-delta  input-gate-delta
                                       :forget-gate-w-delta forget-gate-delta
                                       :output-gate-w-delta output-gate-delta})
                    (map? x-input)
                    (let [[sparse-k v] sparse]
                      (assoc acc sparse-k {:block-w-delta       (scal v block-delta)
                                           :input-gate-w-delta  (scal v input-gate-delta)
                                           :forget-gate-w-delta (scal v forget-gate-delta)
                                           :output-gate-w-delta (scal v output-gate-delta)}))))
            {}
            x-input)))

(defn lstm-param-delta
  [model lstm-part-delta x-input self-activation:t-1 self-state:t-1]
  (let [{:keys [hidden  matrix-kit]} model
        {:keys [outer times]} matrix-kit
        {:keys [sparses unit-num]} hidden
        {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
        block-wr-delta       (outer block-delta self-activation:t-1)
        input-gate-wr-delta  (outer input-gate-delta self-activation:t-1)
        forget-gate-wr-delta (outer forget-gate-delta self-activation:t-1)
        output-gate-wr-delta (outer output-gate-delta self-activation:t-1)
        peephole-input-gate  (times input-gate-delta  (:cell-state self-state:t-1))
        peephole-forget-gate (times forget-gate-delta (:cell-state self-state:t-1))
        peephole-output-gate (times output-gate-delta (:cell-state self-state:t-1))
        template {:block-wr-delta block-wr-delta :input-gate-wr-delta input-gate-wr-delta
                  :forget-gate-wr-delta forget-gate-wr-delta :output-gate-wr-delta output-gate-wr-delta
                  :block-bias-delta block-delta
                  :input-gate-bias-delta input-gate-delta
                  :forget-gate-bias-delta forget-gate-delta
                  :output-gate-bias-delta output-gate-delta
                  :peephole-input-gate-delta peephole-input-gate :peephole-forget-gate-delta peephole-forget-gate
                  :peephole-output-gate-delta peephole-output-gate}
        param-delta (if (or (set? x-input) (map? x-input))
                      (-> template (assoc :sparses-delta (param-delta-sparse model x-input block-delta input-gate-delta forget-gate-delta output-gate-delta unit-num)))
                      (let [block-w-delta        (outer block-delta x-input)
                            input-gate-w-delta   (outer input-gate-delta x-input)
                            forget-gate-w-delta  (outer forget-gate-delta x-input)
                            output-gate-w-delta  (outer output-gate-delta x-input)]
                        (-> template (assoc
                                       :block-w-delta block-w-delta :input-gate-w-delta input-gate-w-delta
                                       :forget-gate-w-delta forget-gate-w-delta :output-gate-w-delta output-gate-w-delta))))]
    param-delta))

(defn lstm-delta-zeros
  [make-vector unit-num]
  {:block-delta       (make-vector (repeat unit-num 0))
   :input-gate-delta  (make-vector (repeat unit-num 0))
   :forget-gate-delta (make-vector (repeat unit-num 0))
   :output-gate-delta (make-vector (repeat unit-num 0))
   :cell-state-delta  (make-vector (repeat unit-num 0))})

(defn gate-zeros
  [make-vector unit-num]
  {:forget-gate (make-vector (repeat unit-num 0))})

(defn merge-param
  [plus merger! acc param-delta]
  (if (nil? acc)
    param-delta
    (merge-with #(cond
                   (map? %1); if each value is sparses
                   (merge-with (fn [accw dw] ;for each items
                                 (if (map? accw)
                                   (merge-with (fn [a b] (plus a b)) accw dw);sprase w of each gates
                                   (plus accw dw)));w also bias
                               %1 %2)
                   :else ;if hidden weight map or bias
                   (merger! %2 %1))
                acc
                param-delta)))

(defn bptt
  [model activation output-items-seq]
  (let [{:keys [output hidden matrix-kit output-type]} model
        {:keys [make-vector scal plus merger! transpose gemv clip!]} matrix-kit
        {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr
                input-gate-peephole forget-gate-peephole output-gate-peephole
                unit-num]} hidden]
    ;looping latest to old
    (loop [output-items-seq (reverse output-items-seq),
           propagated-hidden-to-hidden-delta nil,
           output-seq (reverse activation),
           self-delta:t+1 (lstm-delta-zeros make-vector unit-num),
           lstm-state:t+1 (gate-zeros make-vector unit-num),
           output-loss [],
           output-acc nil,
           hidden-acc nil]
      (cond
        (and (= :skip (first output-items-seq)) (nil? propagated-hidden-to-hidden-delta))
        (recur (rest output-items-seq)
               nil
               (rest output-seq)
               (lstm-delta-zeros unit-num)
               (gate-zeros unit-num)
               output-loss
               nil
               nil)
        (first output-seq)
        (let [output-delta (error output-type (:output (:activation (first output-seq))) (first output-items-seq))
              output-param-delta (ff/output-param-delta model output-delta unit-num (:hidden (:activation (first output-seq))))
              propagated-output-to-hidden-delta (when-not (= :skip (first output-items-seq))
                                                  (->> output-delta
                                                       (map (fn [[item delta]]
                                                              (let [w (:w (get output item))]
                                                                (scal delta w))))
                                                       (apply plus)
                                                       (clip! 100)))
              ;merging delta: hidden-to-hidden + above-to-hidden
              summed-propagated-delta (cond (and (not= :skip (first output-items-seq)) propagated-hidden-to-hidden-delta)
                                            (plus propagated-hidden-to-hidden-delta propagated-output-to-hidden-delta)
                                            (= :skip (first output-items-seq))
                                            propagated-hidden-to-hidden-delta
                                            (nil? propagated-hidden-to-hidden-delta)
                                            propagated-output-to-hidden-delta)
              ;hidden delta
              lstm-state (:hidden (:state (first output-seq)))
              cell-state:t-1 (or (:cell-state (:hidden (:state (second output-seq)))) (make-vector (repeat unit-num 0)))
              lstm-part-delta (lstm-part-delta model unit-num summed-propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                               input-gate-peephole forget-gate-peephole output-gate-peephole)
              x-input (:input (:activation (first output-seq)))
              self-activation:t-1 (or (:hidden (:activation (second output-seq)))
                                      (make-vector (repeat unit-num 0)));when first output time (last time of bptt
              self-state:t-1      (or (:hidden (:state (second output-seq)))
                                      {:cell-state (make-vector (repeat unit-num 0))});when first output time (last time of bptt)
              lstm-param-delta (lstm-param-delta model lstm-part-delta x-input self-activation:t-1 self-state:t-1)
              {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
              propagated-hidden-to-hidden-delta:t-1 (->> (map (fn [w d]
                                                                (gemv (transpose w) d))
                                                              [block-wr    input-gate-wr     forget-gate-wr    output-gate-wr]
                                                              [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                                         (apply plus))]
          (recur (rest output-items-seq)
                 propagated-hidden-to-hidden-delta:t-1
                 (rest output-seq)
                 lstm-part-delta
                 (:hidden (:state (first output-seq)))
                 (cons output-delta output-loss)
                 (merge-param plus merger! output-acc output-param-delta)
                 (merge-param plus merger! hidden-acc lstm-param-delta)))
        :else
        {:param-loss  {:output-delta output-acc
                       :hidden-delta hidden-acc}
         :loss output-loss}))))


(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [output hidden matrix-kit]} model
        {:keys [rewrite-vector! rewrite-matrix!]} matrix-kit
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
                  (rewrite-vector! learning-rate bias bias-delta)
                  (rewrite-vector! learning-rate w w-delta))))
         dorun)
    ;update input connection
    (->> sparses-delta
         vec
         (mapv (fn [[word lstm-w-delta]]
                 (let [{:keys [block-w-delta input-gate-w-delta forget-gate-w-delta output-gate-w-delta]} lstm-w-delta
                       {:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses word)]
                   (rewrite-vector! learning-rate block-w block-w-delta)
                   (rewrite-vector! learning-rate input-gate-w input-gate-w-delta)
                   (rewrite-vector! learning-rate forget-gate-w forget-gate-w-delta)
                   (rewrite-vector! learning-rate output-gate-w output-gate-w-delta))))
         dorun)
    (when block-w-delta       (rewrite-matrix! learning-rate block-w block-w-delta))
    (when input-gate-w-delta  (rewrite-matrix! learning-rate input-gate-w input-gate-w-delta))
    (when forget-gate-w-delta (rewrite-matrix! learning-rate forget-gate-w forget-gate-w-delta))
    (when output-gate-w-delta (rewrite-matrix! learning-rate output-gate-w output-gate-w-delta))
    ;update recurrent connection
    (rewrite-matrix! learning-rate  block-wr  block-wr-delta)
    (rewrite-matrix! learning-rate  input-gate-wr  input-gate-wr-delta)
    (rewrite-matrix! learning-rate  forget-gate-wr  forget-gate-wr-delta)
    (rewrite-matrix! learning-rate  output-gate-wr  output-gate-wr-delta)
    ;update lstm bias and peephole
    (rewrite-vector! learning-rate block-bias block-bias-delta)
    (rewrite-vector! learning-rate input-gate-bias input-gate-bias-delta)
    (rewrite-vector! learning-rate forget-gate-bias forget-gate-bias-delta)
    (rewrite-vector! learning-rate output-gate-bias output-gate-bias-delta)
    (rewrite-vector! learning-rate input-gate-peephole peephole-input-gate-delta)
    (rewrite-vector! learning-rate forget-gate-peephole peephole-forget-gate-delta)
    (rewrite-vector! learning-rate output-gate-peephole peephole-output-gate-delta)
    model))


(defn init-model
  [{:keys [input-items input-size hidden-size output-type output-items matrix-kit]
    :or {matrix-kit default/default-matrix-kit}}]
  (let [{:keys [type make-vector init-vector init-matrix init-orthogonal-matrix]} matrix-kit]
    (println (str "initializing model as " (if (= type :native) "native-array" "vectorz") " ..."))
    {:matrix-kit matrix-kit
     :weight-type type
     :hidden (let [bwr (init-orthogonal-matrix hidden-size);for recurrent connection
                   bb  (init-vector hidden-size)
                   iwr (init-orthogonal-matrix hidden-size)
                   ib  (init-vector hidden-size)
                   ip  (init-vector hidden-size)
                   fwr (init-orthogonal-matrix hidden-size)
                   fb  (make-vector (take hidden-size (repeat (float 1))))
                   fp  (init-vector hidden-size)
                   owr (init-orthogonal-matrix hidden-size)
                   ob  (init-vector hidden-size)
                   op  (init-vector hidden-size)
                   template {:unit-num hidden-size
                             :block-wr       bwr   :block-bias           bb
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
     :output-type output-type}))

(defn convert-model
  [model new-matrix-kit]
  (let [{:keys [hidden output unit-nums]} model
        [input-num hidden-num] unit-nums
        {:keys [make-vector make-matrix] :as matrix-kit} (or new-matrix-kit default/default-matrix-kit)]
    (assoc model
      :matrix-kit matrix-kit
      :hidden (let [{:keys [sparses block-w input-gate-w forget-gate-w output-gate-w
                            unit-num block-wr block-bias input-gate-wr input-gate-bias
                            forget-gate-wr forget-gate-bias output-gate-wr　output-gate-bias
                            input-gate-peephole　forget-gate-peephole　output-gate-peephole]} hidden
                    bwr (make-matrix unit-num unit-num (apply concat (seq block-wr)))
                    bb  (make-vector (seq block-bias))
                    iwr (make-matrix unit-num unit-num (apply concat (seq input-gate-wr)))
                    ib  (make-vector (seq input-gate-bias))
                    ip  (make-vector (seq input-gate-peephole))
                    fwr (make-matrix unit-num unit-num (apply concat (seq forget-gate-wr)))
                    fb  (make-vector (seq forget-gate-bias))
                    fp  (make-vector (seq forget-gate-peephole))
                    owr (make-matrix unit-num unit-num (apply concat (seq output-gate-wr)))
                    ob  (make-vector (seq output-gate-bias))
                    op  (make-vector (seq output-gate-peephole))
                    template (assoc hidden
                               :block-wr       bwr   :block-bias           bb
                               :input-gate-wr  iwr   :input-gate-bias      ib
                               :forget-gate-wr fwr   :forget-gate-bias     fb
                               :output-gate-wr owr   :output-gate-bias     ob
                               :input-gate-peephole  ip  :forget-gate-peephole fp :output-gate-peephole op)]
                (assoc template
                  :sparses (reduce (fn [acc [item {:keys[ block-w input-gate-w forget-gate-w output-gate-w]}]]
                                     (assoc acc item
                                       {:block-w       (make-vector (seq block-w))
                                        :input-gate-w  (make-vector (seq input-gate-w))
                                        :forget-gate-w (make-vector (seq forget-gate-w))
                                        :output-gate-w (make-vector (seq output-gate-w))}))
                                   {}
                                   sparses)
                  :block-w       (make-matrix input-num hidden-num (apply concat (seq block-w)))
                  :input-gate-w  (make-matrix input-num hidden-num (apply concat (seq input-gate-w)))
                  :forget-gate-w (make-matrix input-num hidden-num (apply concat (seq forget-gate-w)))
                  :output-gate-w (make-matrix input-num hidden-num (apply concat (seq output-gate-w)))))
      :output (reduce (fn [acc [item {:keys [w bias]}]]
                        (assoc acc item {:w (make-vector (seq w)) :bias (make-vector (seq bias))}))
                      {}
                      output))))


(defn load-model
  [target-path matrix-kit]
  (convert-model (util/load-model target-path) matrix-kit))
