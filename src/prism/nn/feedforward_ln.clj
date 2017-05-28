(ns prism.nn.feedforward-ln
  (:require
    [clojure.pprint :refer [pprint]]
    [matrix.default :as default]
    [prism.unit :as u]
    [prism.nn.feedforward :as ff]))


(defn network-output
  [model x-input sparse-outputs]
  (let [{:keys [hidden hidden-size matrix-kit]} model
        {:keys [w bias gain]} hidden
        activation-function (:activation hidden)
        {:keys [plus gemv make-vector]} matrix-kit
        alpha (if (or (set? x-input) (map? x-input))
                (ff/hidden-state-by-sparse model x-input (make-vector (repeat hidden-size 0)))
                (gemv w x-input))
        {:keys [state alpha-bar normalized-preactivation sigma]} (u/layer-normalization matrix-kit alpha gain bias)
        hidden-activation (u/activation state activation-function matrix-kit)
        output-activation (ff/output-activation model hidden-activation sparse-outputs)]
    {:activation {:input x-input :hidden hidden-activation :output output-activation}
     :state      {:hidden state :sigma sigma :normalized-preactivation normalized-preactivation :alpha-bar alpha-bar}}))


(defn back-propagation
  [model model-forward training-y]
  (let [{:keys [output hidden hidden-size output-type matrix-kit]} model
        {:keys [gain]} hidden
        {:keys [activation state]} model-forward
        {:keys [sigma normalized-preactivation alpha-bar]} state
        training-x (:input activation)
        {:keys [scal plus times matrix-kit-type native-dv]} matrix-kit
        output-delta (u/error output-type (:output activation) training-y)
        output-param-delta (ff/output-param-delta model output-delta hidden-size (:hidden activation))
        propagated-delta (->> output-delta
                              (map (fn [[item delta]]
                                     (let [w (:w (get output item))]
                                       (scal delta w))))
                              (apply plus))
        hidden-delta (times (u/derivative (:hidden state) (:activation hidden) matrix-kit)
                            propagated-delta)
        {:keys [ln-delta gain-delta]} (u/layer-normalization-delta matrix-kit gain hidden-delta sigma normalized-preactivation alpha-bar)
        hidden-param-delta (if (or (set? training-x) (map? training-x))
                             (ff/param-delta:sparse model ln-delta training-x hidden-size)
                             (ff/param-delta model ln-delta training-x))]
    {:param-loss {:output-delta output-param-delta
                  :hidden-delta (assoc hidden-param-delta
                                  :bias-delta hidden-delta ;over ride
                                  :gain-delta gain-delta)}
     :loss output-delta}))

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
    (let [{:keys [sparses w bias gain]} hidden
          {:keys [sparses-delta w-delta bias-delta gain-delta]} hidden-delta]
      (->> sparses-delta
           (map (fn [[k v]]
                  (let [word-w (get sparses k)]
                    ;; update hidden w
                    (rewrite-vector! learning-rate word-w v))))
           dorun)
      ;; update hidden w
      (when w-delta (rewrite-matrix! learning-rate w w-delta))
      ;; update gain
      (rewrite-vector! learning-rate gain gain-delta)
      ;; update hidden bias
      (rewrite-vector! learning-rate bias bias-delta)))
  model)


(defn init-model
  [{:keys [input-items input-size hidden-size output-type output-items activation matrix-kit]
    :or {matrix-kit default/default-matrix-kit}
    :as init-param}]
  (let [{:keys [make-vector]} matrix-kit
        model (ff/init-model init-param)
        {:keys [hidden hidden-size]} model]
    (assoc model
      :hidden (assoc hidden :gain (make-vector (repeat hidden-size 0.00001))))))

