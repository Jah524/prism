(ns prism.nn.feedforward-bn
  (:require
    [clojure.pprint :refer [pprint]]
    [matrix.default :as default]
    [clojure.core.matrix.stats :as s]
    [prism.unit :as u]
    [prism.nn.feedforward :as ff]))


(defn network-output-batch
  [model x-input-batch sparse-outputs-batch]
  (let [{:keys [hidden hidden-size matrix-kit]} model
        {:keys [w bias scale]} hidden
        activation-function (:activation hidden)
        {:keys [plus gemv make-vector]} matrix-kit
        alpha-batch (->> x-input-batch
                         (mapv (fn [x-input]
                                 (if (or (set? x-input) (map? x-input))
                                   (ff/hidden-state-by-sparse model x-input (make-vector (repeat hidden-size 0)))
                                   (gemv w x-input)))))
        {:keys [preactivation-batch] :as bn-forward} (u/batch-normalization matrix-kit hidden-size alpha-batch scale bias)
        hidden-activation-batch (->> preactivation-batch (mapv (fn [preactivation] (u/activation (:state preactivation) activation-function matrix-kit))))
        output-activation-batch (mapv (fn [hidden-activation sparse-outputs]
                                        (ff/output-activation model hidden-activation sparse-outputs))
                                      hidden-activation-batch
                                      sparse-outputs-batch)]
    (assoc (dissoc bn-forward :preactivation)
      :activation {:input-batch x-input-batch :hidden-batch hidden-activation-batch :output-batch output-activation-batch}
      :state      {:hidden-batch (->> preactivation-batch (mapv :state))}
      :batch-size (count x-input-batch))))

(defn population
  [model x-input-full-batch]
  (let [{:keys [hidden hidden-size matrix-kit]} model
        {:keys [w scale]} hidden
        {:keys [plus gemv make-vector]} matrix-kit
        alpha-full-batch (->> x-input-full-batch
                              (mapv (fn [x-input]
                                      (if (or (set? x-input) (map? x-input))
                                        (ff/hidden-state-by-sparse model x-input (make-vector (repeat hidden-size 0)))
                                        (gemv w x-input)))))]
    {:pop-mean     (s/mean alpha-full-batch)
     :pop-variance (s/variance alpha-full-batch)}))


(defn network-output
  [model pop-mean pop-variance x-input sparse-outputs]
  (let [{:keys [hidden hidden-size matrix-kit]} model
        {:keys [gemv make-vector]} matrix-kit
        {:keys [w bias scale]} hidden
        alpha (if (or (set? x-input) (map? x-input))
                (ff/hidden-state-by-sparse model x-input (make-vector (repeat hidden-size 0)))
                (gemv w x-input))
        activation-function (:activation hidden)
        state (u/batch-normalization-with-population matrix-kit hidden-size pop-mean pop-variance alpha scale bias)
        hidden-activation (u/activation state activation-function matrix-kit)
        output-activation (ff/output-activation model hidden-activation sparse-outputs)]
    {:activation {:input x-input :hidden hidden-activation :output output-activation}}))


(defn back-propagation
  [model model-forward training-y-batch]
  (let [{:keys [output hidden hidden-size output-type matrix-kit]} model
        {:keys [scal plus times matrix-kit-type]} matrix-kit
        {:keys [scale]} hidden
        {:keys [activation state]} model-forward
        {:keys [hidden-batch]} state
        training-x-batch (:input-batch activation)
        output-delta-batch (mapv (fn [output training-y] (u/error output-type output training-y))
                                 (:output-batch activation)
                                 training-y-batch)
        output-param-delta (mapv (fn [output-delta hidden-activation] (ff/output-param-delta model output-delta hidden-size hidden-activation))
                                 output-delta-batch
                                 (:hidden-batch activation))
        propagated-delta-batch (->> output-delta-batch
                                    (mapv (fn [output-delta]
                                            (->> output-delta
                                                 (map (fn [[item delta]]
                                                        (let [w (:w (get output item))]
                                                          (scal delta w))))
                                                 (apply plus)))))
        hidden-delta-batch (mapv (fn [hidden-state propagated-delta]
                                   (times (u/derivative hidden-state (:activation hidden) matrix-kit)
                                          propagated-delta))
                                 hidden-batch
                                 propagated-delta-batch)
        bn-delta-batch (u/batch-normalization-delta matrix-kit hidden-delta-batch scale model-forward)
        hidden-param-delta-batch (mapv (fn [bn-delta training-x]
                                         (if (or (set? training-x) (map? training-x))
                                           (ff/param-delta:sparse model bn-delta training-x hidden-size)
                                           (ff/param-delta model bn-delta training-x)))
                                       (mapv :bn-delta bn-delta-batch)
                                       training-x-batch)]
    {:param-loss {:output-delta (apply u/merge-param plus  output-param-delta)
                  :hidden-delta (assoc (apply u/merge-param plus hidden-param-delta-batch)
                                  :scale-delta (apply plus (mapv :scale-delta  bn-delta-batch))
                                  :shift-delta (apply plus (mapv :shift-delta  bn-delta-batch)))}
     :loss-seq output-delta-batch}))


(defn update-model! [model param-delta learning-rate]
  (let [{:keys [output hidden matrix-kit]} model
        {:keys [output-delta hidden-delta]} param-delta
        {:keys [type rewrite-vector! rewrite-matrix!]} matrix-kit]
    ;; update output
    (->> output-delta
         (map (fn [[item {:keys [w-delta bias-delta]}]]
                (let [{:keys [w bias]} (get output item)]
                  ;update output w
                  (rewrite-vector! learning-rate w w-delta)
                  ;update output bias
                  (rewrite-vector! learning-rate bias bias-delta))))
         dorun)
    ;; update hidden
    (let [{:keys [sparses w bias scale]} hidden
          {:keys [sparses-delta w-delta shift-delta scale-delta]} hidden-delta]
      (->> sparses-delta
           (map (fn [[k v]]
                  (let [word-w (get sparses k)]
                    ;; update hidden w
                    (rewrite-vector! learning-rate word-w v))))
           dorun)
      ;; update hidden w
      (when w-delta (rewrite-matrix! learning-rate w w-delta))
      ;; update gain
      (rewrite-vector! learning-rate scale scale-delta)
      ;; update hidden bias
      (rewrite-vector! learning-rate bias shift-delta)))
  model)

(defn init-model
  [{:keys [input-items input-size hidden-size output-type output-items activation matrix-kit]
    :or {matrix-kit default/default-matrix-kit}
    :as init-param}]
  (let [{:keys [make-vector]} matrix-kit
        model (ff/init-model init-param)
        {:keys [hidden hidden-size]} model]
    (assoc model
      :hidden (assoc hidden :scale (make-vector (repeatedly hidden-size rand))))))

