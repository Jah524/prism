(ns sai-ai.nn.sparse-output-lstm
  (:require
    [clojure.string :as str]
    [clj-time.local :as l]
    [clj-time.core  :as t]
    [clojure.data.json :as json]
    [clojure.core.async :refer [go]]
    [clojure.java.io :refer [reader writer]]
    [matrix.default :refer [sum minus times dot outer transpose gemv]]
    [sai-ai.util :refer [progress-format make-wl save-model load-model]]
    [sai-ai.unit :refer [derivative tanh sigmoid softmax model-rand]]
    ))

(defn output-activation [model input-list positives negatives & [lstm-option]]
  (let [model-layer (:output model)
        {:keys [w bias]} model-layer]
    (reduce (fn [acc word]
              (assoc acc word (sigmoid (+ (reduce + (times (get w word) input-list)) (aget ^floats (get bias word) 0)))))
            {}
            (concat positives negatives))))

(defn lstm-activation [model x-input recurrent-input-list previous-cell-state & [lstm-option]]
  (let [gemv (if-let [it (:gemv lstm-option)] it gemv)
        lstm-layer (:utt-embedding model)
        {:keys [block-wr block-bias input-gate-wr input-gate-bias input-gate-peephole
                forget-gate-wr forget-gate-bias forget-gate-peephole
                output-gate-wr output-gate-bias output-gate-peephole peephole unit-num sparse?
                sparses]} lstm-layer
        [block' input-gate' forget-gate' output-gate']
        (if sparse?
          (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses x-input)]
            [block-w input-gate-w forget-gate-w output-gate-w])
          (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} lstm-layer
                lstm-mat [block-w input-gate-w forget-gate-w output-gate-w]]
            ;;             (clojure.pprint/pprint x-input)
            (mapv #(gemv % x-input) lstm-mat)))
        lstm-mat-r  [block-wr input-gate-wr forget-gate-wr output-gate-wr]
        [block-r' input-gate-r' forget-gate-r' output-gate-r'] (mapv #(gemv % recurrent-input-list) lstm-mat-r)
        ;;         _(clojure.pprint/pprint block')
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

(defn lstm-model-output
  [model x-input positives negatives previous-hidden-output previous-cell-state & [lstm-option]]
  (let [{:keys [activation state]} (lstm-activation model x-input previous-hidden-output previous-cell-state lstm-option)
        output (output-activation model activation positives negatives lstm-option)]
    {:activation [x-input activation output]
     :state  [x-input state nil]}))


(defn sequential-output [model x-seq positives negatives & [lstm-option]]
  (let [hidden-size (:hidden-size model)]
    (loop [x-seq x-seq,
           previous-hidden-output (float-array hidden-size)
           previous-cell-state    (float-array hidden-size)
           acc []]
      (if-let [x-list (first x-seq)]
        (let [model-output (lstm-model-output model x-list positives negatives previous-hidden-output previous-cell-state lstm-option)
              {:keys [activation state]} model-output]
          (recur (rest x-seq)
                 (second activation)
                 (:cell-state (second state))
                 (cons model-output acc)))
        (vec (reverse acc))))))


;;;;    Back Propagation Through Time    ;;;;


(defn negative-sampling
  [hidden-size model-output-seq positives negatives & [option]]
  (let [negatives (remove (fn [n] (some #(= % n) positives)) negatives)
        output (last (:activation (last model-output-seq)))
        ps (map (fn [p] [p (float (- 1 (get output p)))]) positives)
        ns (map (fn [n] [n (float (- (get output n)))]) negatives)]
    (vec (concat ps ns))))


(defn lstm-part-delta
  "propagation through a unit"
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
  (let [{:keys [utt-embedding hidden-size]} model
        {:keys [sparse? sparses]} utt-embedding
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
                      (-> template (assoc :sparses-delta (hash-map x-input {:block-w-delta block-delta
                                                                            :input-gate-w-delta input-gate-delta
                                                                            :forget-gate-w-delta forget-gate-delta
                                                                            :output-gate-w-delta output-gate-delta})))
                      (let [block-w-delta        (outer block-delta x-input)
                            input-gate-w-delta   (outer input-gate-delta x-input)
                            forget-gate-w-delta  (outer forget-gate-delta x-input)
                            output-gate-w-delta  (outer output-gate-delta x-input)]
                        (-> template (assoc
                                       :block-w-delta block-w-delta :input-gate-w-delta input-gate-w-delta
                                       :forget-gate-w-delta forget-gate-w-delta :output-gate-w-delta output-gate-w-delta))))]
    param-delta))

(defn output-param-delta
  [word-delta-pairs hidden-size hidden-activation]
  (->> word-delta-pairs
       (mapv (fn [[word delta]]
               {:word word
                :w-delta    (times hidden-activation (float-array (repeat hidden-size delta)))
                :bias-delta delta}))))

(defn bptt
  [model x-seq positives negatives & [option]]
  (let [{:keys [hidden-size output utt-embedding]} model
        {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr
                input-gate-peephole forget-gate-peephole output-gate-peephole]} utt-embedding
        model-output-seq (sequential-output model x-seq positives negatives option)
        output-w (:w output)
        output-delta (negative-sampling hidden-size model-output-seq positives negatives)
        output-param-delta (output-param-delta output-delta hidden-size (second (:activation (last model-output-seq))))
        propagated-delta (->> output-delta
                              (map (fn [[word delta]]
                                     (let [w (get output-w word)
                                           v (float-array (repeat hidden-size delta))]
                                       (times w v))))
                              (apply sum))]
    (loop [propagated-delta propagated-delta,
           oseq (reverse model-output-seq)
           x-seq (reverse x-seq)
           self-delta:t+1 {:block-delta       (float-array hidden-size)
                           :input-gate-delta  (float-array hidden-size)
                           :forget-gate-delta (float-array hidden-size)
                           :output-gate-delta (float-array hidden-size)
                           :cell-state-delta  (float-array hidden-size)}
           lstm-state:t+1 {:forget-gate (float-array hidden-size)}
           acc nil]
      (if-let [output-layer (first oseq)]
        (let [lstm-state (second (:state (first oseq)))
              cell-state:t-1 (or (:cell-state (second (:state (second oseq)))) (float-array hidden-size))
              lstm-part-delta (lstm-part-delta hidden-size propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                               input-gate-peephole forget-gate-peephole output-gate-peephole)
              x-input (first x-seq)
              self-activation:t-1 (or (second (:activation (second oseq))) (float-array hidden-size))
              self-state:t-1      (or (second (:state      (second oseq))) {:cell-state (float-array hidden-size)})
              lstm-param-delta (lstm-param-delta model lstm-part-delta x-input self-activation:t-1 self-state:t-1)
              {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
              propagated-delta:t-1 (->> (map (fn [w d]
                                               (gemv (transpose hidden-size w) d))
                                             [block-wr    input-gate-wr     forget-gate-wr    output-gate-wr]
                                             [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                        (apply sum))]
          (recur propagated-delta:t-1
                 (rest oseq)
                 (rest x-seq)
                 lstm-part-delta
                 (second (:state output-layer))
                 (if (nil? acc)
                   lstm-param-delta
                   (merge-with #(if (map? %1); if sparses
                                  (merge-with (fn [accw dw]
                                                (merge-with (fn [acc d]
                                                              (sum acc d))
                                                            accw dw))
                                              %1 %2)
                                  (sum %1 %2))
                               acc lstm-param-delta))))
        {:output-delta output-param-delta
         :utt-embedding-delta acc}))))


(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [output utt-embedding hidden-size]} model
        {:keys [output-delta utt-embedding-delta]} param-delta-list
        _ (->> output-delta
               (map (fn [{:keys [word w-delta bias-delta]}]
                      (let [w    (get (:w output) word)
                            bias (get (:bias output) word)]
                        (aset ^floats bias 0 (float (+ (aget ^floats bias 0) (* learning-rate bias-delta))))
                        (dotimes [x hidden-size]
                          (aset ^floats w x (float (+ (aget ^floats w x) (* learning-rate (aget ^floats w-delta x)))))))))
               doall)
        {:keys [block-w-delta block-wr-delta block-bias-delta input-gate-w-delta input-gate-wr-delta input-gate-bias-delta
                forget-gate-w-delta forget-gate-wr-delta forget-gate-bias-delta output-gate-w-delta output-gate-wr-delta output-gate-bias-delta
                peephole-input-gate-delta peephole-forget-gate-delta peephole-output-gate-delta sparses-delta]} utt-embedding-delta
        {:keys [block-w block-wr block-bias input-gate-w input-gate-wr input-gate-bias
                forget-gate-w forget-gate-wr forget-gate-bias output-gate-w output-gate-wr output-gate-bias
                input-gate-peephole forget-gate-peephole output-gate-peephole
                unit-num sparse? sparses]} utt-embedding
        ;update input connection
        _ (if sparse?
            (->> sparses-delta
                 vec
                 (mapv (fn [[word lstm-w-delta]]
                         (let [{:keys [block-w-delta input-gate-w-delta forget-gate-w-delta output-gate-w-delta]} lstm-w-delta
                               {:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses word)]
                           ;;                            (clojure.pprint/pprint block-w)
                           (dotimes [x hidden-size]
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
        _ (dotimes [x (* hidden-size hidden-size)]
            (aset ^floats block-wr x (float (+ (aget ^floats block-wr x) (* learning-rate (aget ^floats block-wr-delta x)))))
            (aset ^floats input-gate-wr x (float (+ (aget ^floats input-gate-wr x) (* learning-rate (aget ^floats input-gate-wr-delta x)))))
            (aset ^floats forget-gate-wr x (float (+ (aget ^floats forget-gate-wr x) (* learning-rate (aget ^floats forget-gate-wr-delta x)))))
            (aset ^floats output-gate-wr x (float (+ (aget ^floats output-gate-wr x) (* learning-rate (aget ^floats output-gate-wr-delta x))))))
        ;update lstm biase and peephole
        _ (dotimes [x hidden-size]
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
                        (float (+ (aget ^floats output-gate-peephole x) (* learning-rate (aget ^floats peephole-output-gate-delta x)))))))]
    :done))

(defn random-array [n]
  (let [it (float-array n)]
    (dotimes [x n] (aset ^floats it x (model-rand)))
    it))

(defn init-model
  [wl em em-size input-type hidden-size]
  (let [wl (assoc wl "<unk>" 0);(if (string? wl) (clojure.edn/read-string (slurp wl)) wl)
        em (if (string? em) (load-model em) em)
        wl-keys (keys wl)]
    {:utt-embedding (let [bwr (random-array (* hidden-size hidden-size));for recurrent connection
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
                                              {} (keys wl))]
                          (-> template (assoc :sparses sparses :sparse? (= input-type :sparse))))
                        (let [bw  (random-array (* em-size hidden-size))
                              iw  (random-array (* em-size hidden-size))
                              fw  (random-array (* em-size hidden-size))
                              ow  (random-array (* em-size hidden-size))]
                          (-> template (assoc :block-w bw :input-gate-w iw  :forget-gate-w fw :output-gate-w ow)))))
     :output {:w    (reduce #(assoc %1 %2 (random-array hidden-size)) {} wl-keys)
              :bias (reduce #(assoc %1 %2 (float-array [(model-rand)])) {} wl-keys)}
     :input-type input-type
     :em-size em-size
     :hidden-size hidden-size
     :wl wl
     :em em}))

(defn train!
  [model x-seq positives negatives learning-rate & [option]]
  (let [delta-list (bptt model x-seq positives negatives option)]
    (update-model! model delta-list learning-rate)))


