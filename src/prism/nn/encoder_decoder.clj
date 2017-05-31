(ns prism.nn.encoder-decoder
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.core.matrix :refer [emap esum emul mmul outer-product transpose array dot exp]]
    [clojure.core.matrix.operators :as o]
    [prism.unit :refer [sigmoid tanh clip! init-orthogonal-matrix init-vector init-matrix rewrite! error merge-param]]
    [prism.util :as util]
    [prism.nn.lstm :as lstm]))


(defn encoder-forward [encoder x-seq]
  (let [{:keys [hidden hidden-size]} encoder]
    (loop [x-seq x-seq,
           previous-activation (array :vectorz (repeat hidden-size 0)),
           previous-cell-state (array :vectorz (repeat hidden-size 0)),
           acc []]
      (if-let [x-input (first x-seq)]
        (let [model-output (lstm/lstm-activation encoder x-input previous-activation previous-cell-state)
              {:keys [activation state]} model-output]
          (recur (rest x-seq)
                 activation
                 (:cell-state state)
                 (cons {:input x-input :hidden model-output} acc)))
        (vec (reverse acc))))))


(defn decoder-lstm-activation [decoder x-input recurrent-input-list encoder-input previous-cell-state]
  (let [{:keys [hidden hidden-size]} decoder
        {:keys [block-wr block-bias input-gate-wr input-gate-bias input-gate-peephole
                forget-gate-wr forget-gate-bias forget-gate-peephole
                output-gate-wr output-gate-bias output-gate-peephole peephole
                block-we input-gate-we forget-gate-we output-gate-we ;; encoder connection
                sparses]} hidden
        {:keys [block-w input-gate-w forget-gate-w output-gate-w]} hidden
        lstm-mat [block-w input-gate-w forget-gate-w output-gate-w]
        [block' input-gate' forget-gate' output-gate'] (if (or (set? x-input) (map? x-input))
                                                         (lstm/partial-state-sparse decoder x-input sparses)
                                                         (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} hidden
                                                               lstm-mat [block-w input-gate-w forget-gate-w output-gate-w]]
                                                           (mapv #(mmul % x-input) lstm-mat)))
        ;; recurrent-connection
        lstm-mat-r  [block-wr input-gate-wr forget-gate-wr output-gate-wr]
        [block-r' input-gate-r' forget-gate-r' output-gate-r'] (mapv #(mmul % recurrent-input-list) lstm-mat-r)
        ;; encoder connection
        lstm-mat-e  [block-we input-gate-we forget-gate-we output-gate-we]
        [block-e' input-gate-e' forget-gate-e' output-gate-e'] (mapv #(mmul % encoder-input) lstm-mat-e)
        ;; state of each gates
        block       (o/+ block'       block-r'       block-e'       block-bias)
        input-gate  (o/+ input-gate'  input-gate-r'  input-gate-e'  input-gate-bias  (o/* input-gate-peephole  previous-cell-state))
        forget-gate (o/+ forget-gate' forget-gate-r' forget-gate-e' forget-gate-bias (o/* forget-gate-peephole previous-cell-state))
        output-gate (o/+ output-gate' output-gate-r' forget-gate-e' output-gate-bias (o/* output-gate-peephole previous-cell-state))
        cell-state  (o/+ (o/* (emap tanh block) (emap sigmoid input-gate))
                         (o/* (emap sigmoid forget-gate) previous-cell-state))
        lstm (o/* (emap sigmoid output-gate) (emap tanh cell-state))]
    {:activation lstm
     :state {:lstm lstm :block block :input-gate input-gate :forget-gate forget-gate :output-gate output-gate :cell-state cell-state}}))

(defn- decoder-softmax-states
  [decoder-hidden-list encoder-input previous-input all-output-connection]
  (loop [coll (vec all-output-connection)
         item-acc [],
         state-acc []]
    (if-let [f (first coll)]
      (let [[item v] f
            {:keys [w bias encoder-w previous-input-w previous-input-sparses]} v
            state (+ (dot w decoder-hidden-list)
                     (first bias)
                     (dot encoder-w encoder-input)
                     (if (or (set? previous-input) (map? previous-input))
                       (->> previous-input
                            (mapv (fn [item]
                                    (cond (set? previous-input)
                                          (esum (:w (get previous-input-sparses item)))
                                          (map? previous-input)
                                          (let [[sparse-k v] item]
                                            (esum (o/* v (:w (get previous-input-sparses sparse-k))))))))
                            (apply +))
                       (dot previous-input-w previous-input)))
            state (cond (> state 25) 25 (< state -25) -25 :else state)]
        (recur (rest coll)
               (conj item-acc item)
               (conj state-acc state)))
      {:items item-acc :states (array :vectorz state-acc)})))

(defn decoder-multi-class-prob
  [decoder-hidden-list encoder-input previous-input all-output-connection]
  (let [{:keys [items states]} (decoder-softmax-states decoder-hidden-list encoder-input previous-input all-output-connection)
        activations (exp states)
        s (esum activations)
        a (o/* (/ 1 s) activations)]
    (->> (mapv vector items a)
         (reduce (fn [acc [item a]]
                   (assoc acc item a))
                 {}))))

(defn decoder-output-activation
  [decoder decoder-hidden-list encoder-input previous-input sparse-outputs]
  (let [{:keys [output-type output]} decoder]
    (if (= output-type :multi-class-classification)
      (decoder-multi-class-prob decoder-hidden-list encoder-input previous-input output)
      (let [activation-function (condp = output-type :binary-classification sigmoid :prediction identity)]
        (reduce (fn [acc s]
                  (let [{:keys [w bias encoder-w previous-input-w previous-input-sparses]} (get output s)]
                    (assoc acc s (activation-function (+ (dot w decoder-hidden-list)
                                                         (first bias)
                                                         (dot encoder-w encoder-input)
                                                         (if (or (set? previous-input) (map? previous-input))
                                                           (->> previous-input
                                                                (mapv (fn [item]
                                                                        (cond (set? previous-input)
                                                                              (esum (:w (get previous-input-sparses item)))
                                                                              (map? previous-input)
                                                                              (let [[sparse-k v] item]
                                                                                (esum (o/* v (:w (get previous-input-sparses sparse-k))))))))
                                                                (apply +))
                                                           (dot previous-input-w previous-input)))))))
                {}
                (vec sparse-outputs))))))


(defn decoder-activation-time-fixed
  [decoder x-input sparse-outputs previous-hidden-output encoder-input previous-input previous-cell-state]
  (let [{:keys [activation state]} (decoder-lstm-activation decoder x-input previous-hidden-output encoder-input previous-cell-state)
        output (if (= :skip sparse-outputs) :skipped (decoder-output-activation decoder activation encoder-input previous-input sparse-outputs))]
    {:activation {:input x-input :hidden activation :output output}
     :state  {:input x-input :hidden state}}))

(defn decoder-forward
  [decoder x-seq encoder-input output-items-seq]
  (let [{:keys [hidden hidden-size input-size]} decoder]
    (loop [x-seq x-seq,
           output-items-seq output-items-seq,
           previous-hidden-output (array :vectorz (repeat hidden-size 0)),
           previous-input         (array :vectorz (repeat input-size 0)),
           previous-cell-state    (array :vectorz (repeat hidden-size 0)),
           acc []]
      (if-let [x-list (first x-seq)]
        (let [decoder-output (decoder-activation-time-fixed decoder x-list (first output-items-seq)
                                                            previous-hidden-output encoder-input previous-input previous-cell-state)
              {:keys [activation state]} decoder-output]
          (recur (rest x-seq)
                 (rest output-items-seq)
                 (:hidden activation)
                 x-list
                 (:cell-state (:hidden state))
                 (cons decoder-output acc)))
        (vec (reverse acc))))))

(defn encoder-decoder-forward
  [encoder-decoder-model encoder-x-seq decoder-x-seq decoder-output-items-seq]
  (let [{:keys [encoder decoder]} encoder-decoder-model
        encoder-activation (encoder-forward encoder encoder-x-seq)
        decoder-activation (decoder-forward decoder decoder-x-seq (:activation (:hidden (last encoder-activation))) decoder-output-items-seq)]
    {:encoder encoder-activation :decoder decoder-activation}))


;;   BPTT   ;;


(defn decoder-output-param-delta
  [item-delta-pairs decoder-hidden-size hidden-activation encoder-size encoder-input input-size previous-input]
  (->> item-delta-pairs
       (reduce (fn [acc [item delta]]
                 (assoc acc item {:w-delta    (o/* delta hidden-activation)
                                  :bias-delta (array :vectorz [delta])
                                  :encoder-w-delta (o/* delta encoder-input)
                                  :previous-input-w-delta (if (or (set? previous-input) (map? previous-input))
                                                            {:sparses-delta (->> previous-input
                                                                                 (reduce (fn [acc sparse]
                                                                                           (cond (set? previous-input)
                                                                                                 (assoc acc sparse {:w-delta (array :vectorz [delta])})
                                                                                                 (map? previous-input)
                                                                                                 (let [[sparse-k v] sparse]
                                                                                                   (assoc acc sparse-k {:w-delta (array :vectorz [(o/* v delta)])}))))
                                                                                         {}))}
                                                            {:w (o/* delta previous-input)})}))
               {})))

(defn decoder-lstm-param-delta
  [decoder lstm-part-delta x-input self-activation:t-1 encoder-input self-state:t-1]
  (let [{:keys [hidden]} decoder
        {:keys [sparses]} hidden
        {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
        template {;; reccurent connection
                   :block-wr-delta       (outer-product block-delta self-activation:t-1)
                   :input-gate-wr-delta  (outer-product input-gate-delta self-activation:t-1)
                   :forget-gate-wr-delta (outer-product forget-gate-delta self-activation:t-1)
                   :output-gate-wr-delta (outer-product output-gate-delta self-activation:t-1)
                   ;; encoder connection
                   :block-we-delta       (outer-product block-delta encoder-input)
                   :input-gate-we-delta  (outer-product input-gate-delta encoder-input)
                   :forget-gate-we-delta (outer-product forget-gate-delta encoder-input)
                   :output-gate-we-delta (outer-product output-gate-delta encoder-input)
                   ;; bias and peephole
                   :block-bias-delta       block-delta
                   :input-gate-bias-delta  input-gate-delta
                   :forget-gate-bias-delta forget-gate-delta
                   :output-gate-bias-delta output-gate-delta
                   :peephole-input-gate-delta  (o/* input-gate-delta  (:cell-state self-state:t-1))
                   :peephole-forget-gate-delta (o/* forget-gate-delta (:cell-state self-state:t-1))
                   :peephole-output-gate-delta (o/* output-gate-delta (:cell-state self-state:t-1))}]
    (if (or (set? x-input) (map? x-input))
      (assoc template :sparses-delta (lstm/param-delta-sparse decoder x-input block-delta input-gate-delta forget-gate-delta output-gate-delta))
      (assoc template
        :block-w-delta        (outer-product block-delta x-input)
        :input-gate-w-delta   (outer-product input-gate-delta x-input)
        :forget-gate-w-delta  (outer-product forget-gate-delta x-input)
        :output-gate-w-delta  (outer-product output-gate-delta x-input)))))


(defn encoder-bptt
  [encoder encoder-activation propagated-delta-from-decoder]
  (let [{:keys [hidden hidden-size]} encoder
        {:keys [output hidden]} encoder
        {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr
                input-gate-peephole forget-gate-peephole output-gate-peephole]} hidden]
    ;looping latest to old
    (loop [propagated-hidden-to-hidden-delta propagated-delta-from-decoder,
           output-seq (reverse encoder-activation),
           self-delta:t+1 (lstm/lstm-delta-zeros hidden-size),
           lstm-state:t+1 (lstm/gate-zeros hidden-size),
           hidden-acc nil]
      (if (first output-seq)
        (let [lstm-state (:state (:hidden (first output-seq)))
              cell-state:t-1 (or (:cell-state (:state (:hidden (second output-seq)))) (array :vectorz (repeat hidden-size 0)))
              lstm-part-delta (lstm/lstm-part-delta hidden-size propagated-hidden-to-hidden-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                                    input-gate-peephole forget-gate-peephole output-gate-peephole)
              x-input (:input (first output-seq))
              self-activation:t-1 (or (:activation (:hidden (second output-seq)))
                                      (array :vectorz (repeat hidden-size 0)));when first output time (last time of bptt
              self-state:t-1      (or (:state (:hidden (second output-seq)))
                                      {:cell-state (array :vectorz (repeat hidden-size 0))});when first output time (last time of bptt)
              lstm-param-delta (lstm/lstm-param-delta encoder lstm-part-delta x-input self-activation:t-1 self-state:t-1)
              {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
              propagated-hidden-to-hidden-delta:t-1 (->> (map (fn [w d]
                                                                (mmul (transpose w) d))
                                                              [block-wr    input-gate-wr     forget-gate-wr    output-gate-wr]
                                                              [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                                         (apply o/+))]
          (recur propagated-hidden-to-hidden-delta:t-1
                 (rest output-seq)
                 lstm-part-delta
                 (:state (:hidden (first output-seq)))
                 (merge-param hidden-acc lstm-param-delta)))
        {:hidden-delta hidden-acc}))))

(defn decoder-bptt
  [decoder decoder-activation encoder-input output-items-seq]
  (let [{:keys [output-type output hidden encoder-size input-size hidden-size]} decoder
        {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr
                block-we input-gate-we forget-gate-we output-gate-we
                input-gate-peephole forget-gate-peephole output-gate-peephole]} hidden]
    ;looping latest to old
    (loop [output-items-seq (reverse output-items-seq),
           propagated-hidden-to-hidden-delta nil,
           output-seq (reverse decoder-activation),
           self-delta:t+1 (lstm/lstm-delta-zeros hidden-size),
           lstm-state:t+1 (lstm/gate-zeros hidden-size),
           output-loss [],
           output-acc nil,
           hidden-acc nil,
           encoder-delta (array :vectorz (repeat encoder-size 0))]
      (cond
        (and (= :skip (first output-items-seq)) (nil? propagated-hidden-to-hidden-delta))
        (recur (rest output-items-seq)
               nil
               (rest output-seq)
               (lstm/lstm-delta-zeros hidden-size)
               (lstm/gate-zeros hidden-size)
               output-loss
               nil
               nil
               encoder-delta)
        (first output-seq)
        (let [output-delta (error output-type (:output (:activation (first output-seq))) (first output-items-seq))
              previous-decoder-input (if-let [it (:input (:activation (second output-seq)))] it (array :vectorz (repeat input-size 0)))
              output-param-delta (decoder-output-param-delta output-delta
                                                             hidden-size
                                                             (:hidden (:activation (first output-seq)))
                                                             encoder-size
                                                             encoder-input
                                                             input-size
                                                             previous-decoder-input)
              propagated-output-to-hidden-delta (when-not (= :skip (first output-items-seq))
                                                  (->> output-delta
                                                       (map (fn [[item delta]]
                                                              (let [w (:w (get output item))]
                                                                (o/* delta w))))
                                                       (apply o/+)))
              ;merging delta: hidden-to-hidden + above-to-hidden
              summed-propagated-delta (cond (and (not= :skip (first output-items-seq)) propagated-hidden-to-hidden-delta)
                                            (o/+ propagated-hidden-to-hidden-delta propagated-output-to-hidden-delta)
                                            (= :skip (first output-items-seq))
                                            propagated-hidden-to-hidden-delta
                                            (nil? propagated-hidden-to-hidden-delta)
                                            propagated-output-to-hidden-delta)
              ;hidden delta
              lstm-state (:hidden (:state (first output-seq)))
              cell-state:t-1 (or (:cell-state (:hidden (:state (second output-seq)))) (array :vectorz (repeat hidden-size 0)))
              lstm-part-delta (lstm/lstm-part-delta hidden-size summed-propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                                    input-gate-peephole forget-gate-peephole output-gate-peephole)
              x-input (:input (:activation (first output-seq)))
              self-activation:t-1 (or (:hidden (:activation (second output-seq)))
                                      (array :vectorz (repeat hidden-size 0)));when first output time (last time of bptt
              self-state:t-1      (or (:hidden (:state      (second output-seq)))
                                      {:cell-state (array :vectorz (repeat hidden-size 0))});when first output time (last time of bptt)
              lstm-param-delta (decoder-lstm-param-delta decoder lstm-part-delta x-input self-activation:t-1 encoder-input self-state:t-1)
              {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
              propagated-hidden-to-hidden-delta:t-1 (->> (map (fn [w d]
                                                                (mmul (transpose w) d))
                                                              [block-wr    input-gate-wr     forget-gate-wr    output-gate-wr]
                                                              [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                                         (apply o/+))
              propagation-to-encoder (->> (map (fn [w d]
                                                 (mmul (transpose w) d))
                                               [block-we    input-gate-we    forget-gate-we    output-gate-we]
                                               [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                          (apply o/+))]
          (recur (rest output-items-seq)
                 propagated-hidden-to-hidden-delta:t-1
                 (rest output-seq)
                 lstm-part-delta
                 (:hidden (:state (first output-seq)))
                 (cons output-delta output-loss)
                 (merge-param output-acc output-param-delta)
                 (merge-param hidden-acc lstm-param-delta)
                 (o/+ encoder-delta propagation-to-encoder)))
        :else
        {:param-loss {:output-delta output-acc
                      :hidden-delta hidden-acc
                      :encoder-delta encoder-delta}
         :loss output-loss}))))


(defn encoder-decoder-bptt
  [encoder-decoder-model encoder-decoder-forward decoder-output-items-seq]
  (let [{:keys [encoder decoder]} encoder-decoder-model
        {encoder-activation :encoder decoder-activation :decoder} encoder-decoder-forward
        {loss :loss decoder-param-delta :param-loss} (decoder-bptt decoder decoder-activation  (:activation (:hidden (last encoder-activation))) decoder-output-items-seq)
        encoder-param-delta (encoder-bptt encoder encoder-activation (:encoder-delta decoder-param-delta))]
    {:loss loss
     :param-loss {:encoder-param-delta encoder-param-delta :decoder-param-delta decoder-param-delta}}))


(defn update-decoder!
  [decoder param-delta-list learning-rate]
  (let [{:keys [output hidden input-size encoder-size]} decoder
        {:keys [output-delta hidden-delta]} param-delta-list
        {:keys [block-w-delta block-wr-delta block-bias-delta input-gate-w-delta input-gate-wr-delta input-gate-bias-delta
                forget-gate-w-delta forget-gate-wr-delta forget-gate-bias-delta output-gate-w-delta output-gate-wr-delta output-gate-bias-delta
                block-we-delta input-gate-we-delta forget-gate-we-delta output-gate-we-delta
                peephole-input-gate-delta peephole-forget-gate-delta peephole-output-gate-delta sparses-delta]} hidden-delta
        {:keys [block-w block-wr block-bias input-gate-w input-gate-wr input-gate-bias
                forget-gate-w forget-gate-wr forget-gate-bias output-gate-w output-gate-wr output-gate-bias
                block-we input-gate-we forget-gate-we output-gate-we
                input-gate-peephole forget-gate-peephole output-gate-peephole
                sparse? sparses]} hidden]
    ;update output connection
    (->> output-delta
         (map (fn [[item {:keys [w-delta bias-delta encoder-w-delta previous-input-w-delta previous-input-w-delta]}]]
                (let [{:keys [w bias encoder-w previous-input-w previous-input-sparses]} (get output item)]
                  ; update output params
                  (rewrite! learning-rate bias bias-delta)
                  (rewrite! learning-rate w w-delta)
                  ;; update encoder connection
                  (rewrite! learning-rate encoder-w encoder-w-delta)
                  ;; update decoder previous input
                  ; sparse
                  (->> (:sparses-delta previous-input-w-delta)
                       (map (fn [[item {:keys [w-delta]}]]
                              (let [{:keys [w]} (get previous-input-sparses item)]
                                (rewrite! learning-rate w w-delta))))
                       dorun)
                  ; dense
                  (when (:w previous-input-w-delta) (rewrite! learning-rate previous-input-w (:w previous-input-w-delta))))))
         dorun)
    ;;; update hidden layer
    ;; update input connections
    ; sparse
    (->> sparses-delta
         (map (fn [[word lstm-w-delta]]
                (let [{:keys [block-w-delta input-gate-w-delta forget-gate-w-delta output-gate-w-delta]} lstm-w-delta
                      {:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses word)]
                  (rewrite! learning-rate block-w block-w-delta)
                  (rewrite! learning-rate input-gate-w input-gate-w-delta)
                  (rewrite! learning-rate forget-gate-w forget-gate-w-delta)
                  (rewrite! learning-rate output-gate-w output-gate-w-delta))))
         dorun)
    ; dense
    (when block-w-delta       (rewrite! learning-rate block-w block-w-delta))
    (when input-gate-w-delta  (rewrite! learning-rate input-gate-w input-gate-w-delta))
    (when forget-gate-w-delta (rewrite! learning-rate forget-gate-w forget-gate-w-delta))
    (when output-gate-w-delta (rewrite! learning-rate output-gate-w output-gate-w-delta))
    ;; update recurrent connections
    (rewrite! learning-rate block-wr block-wr-delta)
    (rewrite! learning-rate input-gate-wr input-gate-wr-delta)
    (rewrite! learning-rate forget-gate-wr forget-gate-wr-delta)
    (rewrite! learning-rate output-gate-wr output-gate-wr-delta)
    ; update encoder connections
    (rewrite! learning-rate block-we block-we-delta)
    (rewrite! learning-rate input-gate-we input-gate-we-delta)
    (rewrite! learning-rate forget-gate-we forget-gate-we-delta)
    (rewrite! learning-rate output-gate-we output-gate-we-delta)
    ;; update lstm bias and peephole
    (rewrite! learning-rate block-bias block-bias-delta)
    (rewrite! learning-rate input-gate-bias input-gate-bias-delta)
    (rewrite! learning-rate forget-gate-bias forget-gate-bias-delta)
    (rewrite! learning-rate output-gate-bias output-gate-bias-delta)
    (rewrite! learning-rate input-gate-peephole peephole-input-gate-delta)
    (rewrite! learning-rate forget-gate-peephole peephole-forget-gate-delta)
    (rewrite! learning-rate output-gate-peephole peephole-output-gate-delta)
    decoder))


(defn update-encoder-decoder!
  [encoder-decoder-model encoder-decoder-param-delta learning-rate]
  (let[{:keys [encoder decoder]} encoder-decoder-model
       {:keys [encoder-param-delta decoder-param-delta]} encoder-decoder-param-delta]
    (lstm/update-model! encoder encoder-param-delta learning-rate)
    (update-decoder!    decoder decoder-param-delta learning-rate)
    encoder-decoder-model))


(defn init-decoder
  [{:keys [input-size input-items output-type output-items encoder-hidden-size decoder-hidden-size]
    :as param}]
  (let [decoder (lstm/init-model (assoc param
                                   :input-items input-items
                                   :encoder-size encoder-hidden-size
                                   :hidden-size decoder-hidden-size))
        {:keys [output hidden]} decoder
        d-output (reduce (fn [acc [word param]]
                           (assoc acc word (assoc param
                                             :encoder-w (init-vector encoder-hidden-size)
                                             :previous-input-w (init-vector input-size)
                                             :previous-input-sparses (->> input-items
                                                                          (reduce (fn [acc item]
                                                                                    (assoc acc item {:w (init-vector 1)}))
                                                                                  {})))))
                         {}
                         output)
        d-hidden (assoc hidden ;encoder connection
                   :block-we       (init-matrix encoder-hidden-size decoder-hidden-size)
                   :input-gate-we  (init-matrix encoder-hidden-size decoder-hidden-size)
                   :forget-gate-we (init-matrix encoder-hidden-size decoder-hidden-size)
                   :output-gate-we (init-matrix encoder-hidden-size decoder-hidden-size))]
    (assoc decoder
      :hidden d-hidden
      :output d-output
      :input-size input-size
      :hidden-size decoder-hidden-size
      :encoder-size encoder-hidden-size)))

(defn init-encoder-decoder-model
  [{:keys [input-size input-items output-type output-items encoder-hidden-size decoder-hidden-size]
    :as param}]
  (let [encoder (lstm/init-model (-> param
                                     (dissoc :output-items)
                                     (assoc
                                       :input-items input-items
                                       :hidden-size encoder-hidden-size
                                       :input-size input-size)))]
    {:encoder encoder
     :decoder (init-decoder param)}))
