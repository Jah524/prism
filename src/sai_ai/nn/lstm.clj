(ns sai-ai.nn.lstm
  (:require
    [clojure.pprint :refer [pprint]]
    [matrix.default :refer [transpose sum times outer minus] :as default]
    [sai-ai.unit :refer [sigmoid tanh activation derivative model-rand]]))


(defn lstm-activation [model x-input recurrent-input-list previous-cell-state & [lstm-option]]
  (let [gemv (if-let [it (:gemv lstm-option)] it default/gemv)
        lstm-layer (:hidden model)
        {:keys [block-wr block-bias input-gate-wr input-gate-bias input-gate-peephole
                forget-gate-wr forget-gate-bias forget-gate-peephole
                output-gate-wr output-gate-bias output-gate-peephole peephole unit-num
                sparses]} lstm-layer
        [block' input-gate' forget-gate' output-gate']
        (if (= (:input-type model) :sparse)
          (->> x-input
               (mapv (fn [[sparse-k v]]
                       (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses sparse-k)
                             v-arr (float-array (take unit-num (repeat v)))]
                         [(times block-w v-arr) (times input-gate-w v-arr) (times forget-gate-w v-arr) (times output-gate-w v-arr)])))
               (apply mapv sum))
          (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} lstm-layer
                lstm-mat [block-w input-gate-w forget-gate-w output-gate-w]]
            (mapv #(gemv % x-input) lstm-mat)))
        lstm-mat-r  [block-wr input-gate-wr forget-gate-wr output-gate-wr]
        [block-r' input-gate-r' forget-gate-r' output-gate-r'] (mapv #(gemv % recurrent-input-list) lstm-mat-r)
        block       (sum block' block-r' block-bias)
        input-gate  (sum input-gate' input-gate-r' input-gate-bias    (times input-gate-peephole  previous-cell-state))
        forget-gate (sum forget-gate' forget-gate-r' forget-gate-bias (times forget-gate-peephole previous-cell-state))
        output-gate (sum output-gate' output-gate-r' output-gate-bias (times output-gate-peephole previous-cell-state))
        cell-state  (float-array unit-num)
        _ (dotimes [x unit-num]
            (aset ^floats cell-state x
                  (float (+ (* (tanh (aget ^floats block x)) (sigmoid (aget ^floats input-gate x)))
                            (* (sigmoid (aget ^floats forget-gate x)) (aget ^floats previous-cell-state x))))))
        lstm (float-array unit-num)
        _ (dotimes [x unit-num]
            (aset ^floats lstm x (float (* (sigmoid (aget ^floats output-gate x)) (tanh (aget ^floats cell-state x))))))]
    {:activation lstm
     :state {:lstm lstm :block block :input-gate input-gate :forget-gate forget-gate :output-gate output-gate :cell-state cell-state}}))

(defn output-activation
  [output-layer input-list sparse-outputs & [lstm-option]]
  (let [{:keys [w bias activation]} output-layer
        activation-function (condp = activation :sigmoid sigmoid :negative-sampling sigmoid :linear identity)]
    (if (= activation :softmax)
      :FIXME
      (reduce (fn [acc s]
                (assoc acc s (activation-function (+ (reduce + (times (get w s) input-list)) (aget ^floats (get bias s) 0)))))
              {}
              (vec sparse-outputs)))))

(defn lstm-model-output
  [model x-input sparse-outputs previous-hidden-output previous-cell-state & [lstm-option]]
  (let [{:keys [activation state]} (lstm-activation model x-input previous-hidden-output previous-cell-state lstm-option)
        output (if (= :skip sparse-outputs) :skipped (output-activation (:output model) activation sparse-outputs lstm-option))]
    {:activation {:input x-input :hidden activation :output output}
     :state  {:input x-input :hidden state}}))

(defn sequential-output [model x-seq output-items-seq & [lstm-option]]
  (let [hidden-size (:unit-num (:hidden model))]
    (loop [x-seq x-seq,
           output-items-seq output-items-seq,
           previous-hidden-output (float-array hidden-size),
           previous-cell-state    (float-array hidden-size),
           acc []]
      (if-let [x-list (first x-seq)]
        (let [model-output (lstm-model-output model x-list (first output-items-seq) previous-hidden-output previous-cell-state lstm-option)
              {:keys [activation state]} model-output]
          (recur (rest x-seq)
                 (rest output-items-seq)
                 (:hidden activation)
                 (:cell-state (:hidden state))
                 (cons model-output acc)))
        (vec (reverse acc))))))


;;;;    Back Propagation Through Time    ;;;;

(defn output-param-delta
  [item-delta-pairs hidden-size hidden-activation]
  (->> item-delta-pairs
       (reduce (fn [acc [item delta]]
                 (assoc acc item {:w-delta    (times hidden-activation (float-array (repeat hidden-size delta)))
                                  :bias-delta (float-array [delta])}))
               {})))

(defn lstm-part-delta
  "propagation through a lstm unit"
  [hidden-size propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
   peephole-w-input-gate peephole-w-forget-gate peephole-w-output-gate]
  (let [output-gate-delta (float-array hidden-size)
        _dog (derivative (:output-gate lstm-state) :sigmoid)
        _cell-state (:cell-state lstm-state)
        _ (dotimes [x hidden-size] (aset ^floats output-gate-delta x
                                         (float (* (aget ^floats _dog x) (tanh (aget ^floats _cell-state x)) (aget ^floats propagated-delta x)))))
        cell-state-delta (float-array hidden-size)
        _og (:output-gate lstm-state)
        _dcs (derivative (:cell-state lstm-state) :tanh)
        _fg:t+1 (:forget-gate lstm-state:t+1)
        _csd:t+1 (:cell-state-delta self-delta:t+1)
        _igd:t+1 (:input-gate-delta self-delta:t+1)
        _fgd:t+1 (:forget-gate-delta self-delta:t+1)
        _ (dotimes [x hidden-size] (aset ^floats cell-state-delta x
                                         (float (+ (* (sigmoid (aget ^floats _og x)) (aget ^floats _dcs x) (aget ^floats propagated-delta x))
                                                   (* (sigmoid (aget ^floats _fg:t+1 x)) (aget ^floats _csd:t+1 x))
                                                   (* (aget ^floats peephole-w-input-gate  x) (aget ^floats _igd:t+1 x))
                                                   (* (aget ^floats peephole-w-forget-gate x) (aget ^floats _fgd:t+1 x))
                                                   (* (aget ^floats peephole-w-output-gate x) (aget ^floats output-gate-delta x))))))
        block-delta (float-array hidden-size)
        _ig (:input-gate lstm-state)
        _db (derivative (:block lstm-state) :tanh)
        _ (dotimes [x hidden-size] (aset ^floats block-delta x
                                         (float (* (sigmoid (aget ^floats _ig x)) (aget ^floats _db x) (aget ^floats cell-state-delta x)))))
        forget-gate-delta (float-array hidden-size)
        _dfg (derivative (:forget-gate lstm-state) :sigmoid)
        _ (dotimes [x hidden-size] (aset ^floats forget-gate-delta x
                                         (float (* (aget ^floats _dfg x) (aget ^floats cell-state:t-1 x) (aget ^floats cell-state-delta x)))))
        input-gate-delta (float-array hidden-size)
        _dig (derivative (:input-gate lstm-state) :sigmoid)
        _b (:block lstm-state)
        _ (dotimes [x hidden-size] (aset ^floats input-gate-delta x
                                         (float (* (aget ^floats _dig x) (tanh (aget ^floats _b x)) (aget ^floats cell-state-delta x)))))]
    {:output-gate-delta output-gate-delta :cell-state-delta cell-state-delta :block-delta block-delta
     :forget-gate-delta forget-gate-delta :input-gate-delta input-gate-delta}))

(defn lstm-param-delta
  [model lstm-part-delta x-input self-activation:t-1 self-state:t-1]
  (let [{:keys [sparses unit-num]} (:hidden model)
        sparse? (= (:input-type model) :sparse)
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
        param-delta (if sparse?
                      (let [sparses-delta (reduce (fn [acc [sparse-k v]]
                                                    (let [v-arr (float-array (take unit-num (repeat v)))]
                                                      (assoc acc sparse-k {:block-w-delta       (times block-delta v-arr)
                                                                           :input-gate-w-delta  (times input-gate-delta v-arr)
                                                                           :forget-gate-w-delta (times forget-gate-delta v-arr)
                                                                           :output-gate-w-delta (times output-gate-delta v-arr)})))
                                                  {}
                                                  x-input)]
                        (-> template (assoc :sparses-delta sparses-delta)))
                      (let [block-w-delta        (outer block-delta x-input)
                            input-gate-w-delta   (outer input-gate-delta x-input)
                            forget-gate-w-delta  (outer forget-gate-delta x-input)
                            output-gate-w-delta  (outer output-gate-delta x-input)]
                        (-> template (assoc
                                       :block-w-delta block-w-delta :input-gate-w-delta input-gate-w-delta
                                       :forget-gate-w-delta forget-gate-w-delta :output-gate-w-delta output-gate-w-delta))))]
    param-delta))

(defn lstm-delta-zeros
  [unit-num]
  {:block-delta       (float-array unit-num)
   :input-gate-delta  (float-array unit-num)
   :forget-gate-delta (float-array unit-num)
   :output-gate-delta (float-array unit-num)
   :cell-state-delta  (float-array unit-num)})

(defn gate-zeros
  [unit-num]
  {:forget-gate (float-array unit-num)})


(defn binary-classification-error
  [hidden-size activation positives negatives & [option]]
  (let [negatives (remove (fn [n] (some #(= % n) positives)) negatives)
        ps (map (fn [p] [p (float (- 1 (get activation p)))]) positives)
        ns (map (fn [n] [n (float (- (get activation n)))]) negatives)]
    (vec (concat ps ns))))

(defn prediction-error
  [hidden-size activation positives & option]
  (->> positives
       (map (fn [[item expect-value]]
              (float (- expect-value (get activation item)))))))

(defn bptt
  [model x-seq output-items-seq & [option]]
  (let [gemv (if-let [it (:gemv option)] it default/gemv)
        {:keys [output hidden]} model
        {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr
                input-gate-peephole forget-gate-peephole output-gate-peephole
                unit-num]} hidden
        model-output-seq (sequential-output model x-seq (->> output-items-seq (map (fn [{:keys [pos neg]}] (concat pos neg)))) option)
        output-w (:w output)
        ]
    ;looping latest to old
    (loop [output-items-seq (reverse output-items-seq)
           propagated-hidden-to-hidden-delta nil,
           output-seq (reverse model-output-seq)
           x-seq (reverse x-seq)
           self-delta:t+1 (lstm-delta-zeros unit-num)
           lstm-state:t+1 (gate-zeros unit-num)
           output-acc nil
           hidden-acc nil]
      (cond
        (and (= :skip (first output-items-seq)) (nil? propagated-hidden-to-hidden-delta))
        (recur (rest output-items-seq)
               nil
               (rest output-seq)
               (rest x-seq)
               (lstm-delta-zeros unit-num)
               (gate-zeros unit-num)
               nil
               nil)
        (first output-seq)
        (let [{:keys [pos neg]} (first output-items-seq)
              output-delta (condp = (:output-type model)
                             :binary-classification
                             (binary-classification-error unit-num (:output (:activation (first output-seq))) pos neg))
              output-param-delta (output-param-delta output-delta unit-num (:hidden (:activation (first output-seq))))
              propagated-output-to-hidden-delta (when-not (= :skip (first output-items-seq))
                                                  (->> output-delta
                                                       (map (fn [[item delta]]
                                                              (let [w (get output-w item)
                                                                    v (float-array (repeat unit-num delta))]
                                                                (times w v))))
                                                       (apply sum)))
              ;merge delta hidden-to-hidden + above-to-hidden
              summed-propagated-delta (cond (and (not= :skip (first output-items-seq)) propagated-hidden-to-hidden-delta)
                                            (sum propagated-hidden-to-hidden-delta propagated-output-to-hidden-delta)
                                            (= :skip (first output-items-seq))
                                            propagated-hidden-to-hidden-delta
                                            (nil? propagated-hidden-to-hidden-delta)
                                            propagated-output-to-hidden-delta)
              ;;hidden delta
              lstm-state (:hidden (:state (first output-seq)))
              cell-state:t-1 (or (:cell-state (second (:state (second output-seq)))) (float-array unit-num))
              lstm-part-delta (lstm-part-delta unit-num summed-propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                               input-gate-peephole forget-gate-peephole output-gate-peephole)
              x-input (first x-seq)
              self-activation:t-1 (or (:hidden (:activation (second output-seq)))
                                      (float-array unit-num));when first output time (last time of bptt
              self-state:t-1      (or (:hidden (:state      (second output-seq)))
                                      {:cell-state (float-array unit-num)});when first output time (last time of bptt)
              lstm-param-delta (lstm-param-delta model lstm-part-delta x-input self-activation:t-1 self-state:t-1)
              {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
              propagated-hidden-to-hidden-delta:t-1 (->> (map (fn [w d]
                                                                (gemv (transpose unit-num w) d))
                                                              [block-wr    input-gate-wr     forget-gate-wr    output-gate-wr]
                                                              [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                                         (apply sum))]
          (recur (rest output-items-seq)
                 propagated-hidden-to-hidden-delta:t-1
                 (rest output-seq)
                 (rest x-seq)
                 lstm-part-delta
                 (:hidden (:state (first output-seq)))
                 (if (nil? output-acc)
                   output-param-delta
                   (merge-with #(if (map? %1); if sparses
                                  (merge-with (fn [accw dw]
                                                (sum accw dw));w also bias
                                              %1 %2)
                                  (sum %1 %2))
                               output-acc
                               output-param-delta))
                 (if (nil? hidden-acc)
                   lstm-param-delta
                   (merge-with #(if (map? %1); if sparses
                                  (merge-with (fn [accw dw]
                                                (merge-with (fn [acc d]
                                                              (sum acc d))
                                                            accw dw))
                                              %1 %2)
                                  (sum %1 %2))
                               hidden-acc lstm-param-delta))))
        :else
        {:output-delta output-acc
         :hidden-delta hidden-acc}))))


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
                unit-num sparse? sparses]} hidden]
    ;update output connection
    (->> output-delta
         (map (fn [[item {:keys [w-delta bias-delta]}]]
                (let [w    (get (:w output) item)
                      bias (get (:bias output) item)]
                  (aset ^floats bias 0 (float (+ (aget ^floats bias 0) (* learning-rate (aget ^floats bias-delta 0)))))
                  (dotimes [x unit-num]
                    (aset ^floats w x (float (+ (aget ^floats w x) (* learning-rate (aget ^floats w-delta x)))))))))
         doall)
    ;update input connection
    (if sparse?
      (->> sparses-delta
           vec
           (mapv (fn [[word lstm-w-delta]]
                   (let [{:keys [block-w-delta input-gate-w-delta forget-gate-w-delta output-gate-w-delta]} lstm-w-delta
                         {:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses word)]
                     (dotimes [x unit-num]
                       (aset ^floats block-w x (float (+ (aget ^floats block-w x) (* learning-rate (aget ^floats block-w-delta x)))))
                       (aset ^floats input-gate-w x (float (+ (aget ^floats input-gate-w x) (* learning-rate (aget ^floats input-gate-w-delta x)))))
                       (aset ^floats forget-gate-w x (float (+ (aget ^floats forget-gate-w x) (* learning-rate (aget ^floats forget-gate-w-delta x)))))
                       (aset ^floats output-gate-w x (float (+ (aget ^floats output-gate-w x) (* learning-rate (aget ^floats output-gate-w-delta x)))))))))
           doall)
      (dotimes [x (count block-w-delta)]
        (aset ^floats block-w x (float (+ (aget ^floats block-w x) (* learning-rate (aget ^floats block-w-delta x)))))
        (aset ^floats input-gate-w x (float (+ (aget ^floats input-gate-w x) (* learning-rate (aget ^floats input-gate-w-delta x)))))
        (aset ^floats forget-gate-w x (float (+ (aget ^floats forget-gate-w x) (* learning-rate (aget ^floats forget-gate-w-delta x)))))
        (aset ^floats output-gate-w x (float (+ (aget ^floats output-gate-w x) (* learning-rate (aget ^floats output-gate-w-delta x)))))))
    ;update recurrent connection
    (dotimes [x (* unit-num unit-num)]
      (aset ^floats block-wr x (float (+ (aget ^floats block-wr x) (* learning-rate (aget ^floats block-wr-delta x)))))
      (aset ^floats input-gate-wr x (float (+ (aget ^floats input-gate-wr x) (* learning-rate (aget ^floats input-gate-wr-delta x)))))
      (aset ^floats forget-gate-wr x (float (+ (aget ^floats forget-gate-wr x) (* learning-rate (aget ^floats forget-gate-wr-delta x)))))
      (aset ^floats output-gate-wr x (float (+ (aget ^floats output-gate-wr x) (* learning-rate (aget ^floats output-gate-wr-delta x))))))
    ;update lstm bias and peephole
    (dotimes [x unit-num]
      ;update bias
      (aset ^floats block-bias x (float (+ (aget ^floats block-bias x) (* learning-rate (aget ^floats block-bias-delta x)))))
      (aset ^floats input-gate-bias x (float (+ (aget ^floats input-gate-bias x) (* learning-rate (aget ^floats input-gate-bias-delta x)))))
      (aset ^floats forget-gate-bias x (float (+ (aget ^floats forget-gate-bias x) (* learning-rate (aget ^floats forget-gate-bias-delta x)))))
      (aset ^floats output-gate-bias x (float (+ (aget ^floats output-gate-bias x) (* learning-rate (aget ^floats output-gate-bias-delta x)))))
      ;and peephole
      (when (aset ^floats input-gate-peephole  x
                  (float (+ (aget ^floats input-gate-peephole x)  (* learning-rate (aget ^floats peephole-input-gate-delta  x))))))
      (when (aset ^floats forget-gate-peephole x
                  (float (+ (aget ^floats forget-gate-peephole x) (* learning-rate (aget ^floats peephole-forget-gate-delta x))))))
      (when (aset ^floats output-gate-peephole x
                  (float (+ (aget ^floats output-gate-peephole x) (* learning-rate (aget ^floats peephole-output-gate-delta x)))))))
    model))


(defn random-array [n]
  (let [it (float-array n)]
    (dotimes [x n] (aset ^floats it x (model-rand)))
    it))

(defn init-model
  [input-items output-items input-type input-size hidden-size]
  (let [sparse-input? (= input-type :sparse)]
    {:hidden (let [bwr (random-array (* hidden-size hidden-size));for recurrent connection
                   bb  (random-array hidden-size)
                   iwr (random-array (* hidden-size hidden-size))
                   ib  (random-array hidden-size)
                   ip  (random-array hidden-size)
                   fwr (random-array (* hidden-size hidden-size))
                   fb  (random-array hidden-size)
                   fp  (random-array hidden-size)
                   owr (random-array (* hidden-size hidden-size))
                   ob  (random-array hidden-size)
                   op  (random-array hidden-size)
                   template {:unit-num hidden-size
                             :block-wr       bwr   :block-bias           bb
                             :input-gate-wr  iwr   :input-gate-bias      ib
                             :forget-gate-wr fwr   :forget-gate-bias     fb
                             :output-gate-wr owr   :output-gate-bias     ob
                             :input-gate-peephole  ip  :forget-gate-peephole fp
                             :output-gate-peephole op}]
               (if (= input-type :sparse)
                 (let [sparses (reduce (fn [acc sparse]
                                         (assoc acc sparse {:block-w       (random-array hidden-size)
                                                            :input-gate-w  (random-array hidden-size)
                                                            :forget-gate-w (random-array hidden-size)
                                                            :output-gate-w (random-array hidden-size)}))
                                       {} input-items)]
                   (-> template (assoc :sparses sparses :sparse? (= input-type :sparse))))
                 (let [bw  (random-array (* input-size hidden-size))
                       iw  (random-array (* input-size hidden-size))
                       fw  (random-array (* input-size hidden-size))
                       ow  (random-array (* input-size hidden-size))]
                   (-> template (assoc :block-w bw :input-gate-w iw  :forget-gate-w fw :output-gate-w ow)))))
     :output {:w    (reduce #(assoc %1 %2 (random-array hidden-size))   {} output-items)
              :bias (reduce #(assoc %1 %2 (float-array [(model-rand)])) {} output-items)}
     :input-type input-type
     :unit-nums [(if sparse-input? (count input-items) input-size) hidden-size (count output-items)]}))

(defn train!
  [model x-seq positives negatives learning-rate & [option]]
  (let [delta-list (bptt model x-seq positives negatives option)]
    (update-model! model delta-list learning-rate)))
