(ns prism.nn.feedforward
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.core.matrix :refer [add add! scale emap esum emul emul! mmul outer-product array dot]]
    [prism.unit :refer [sigmoid init-vector init-matrix rewrite! activation multi-class-prob derivative error]]
    [prism.util :as util]))


(defn hidden-state-by-sparse
  [model x-input bias]
  (let [{:keys [hidden]} model
        {:keys [sparses]} hidden]
    (->> x-input
         (reduce (fn [acc item]
                   (cond (set? x-input)
                         (let [w (get sparses item)]
                           (add acc w))
                         (map? x-input)
                         (let [[k v] item
                               w (get sparses k)]
                           (add acc (scale w v)))))
                 bias))))


(defn output-activation
  [model input-list sparse-outputs]
  (let [{:keys [output-type output]} model]
    (if (= output-type :multi-class-classification)
      (multi-class-prob input-list output)
      (let [activation-function (condp = output-type :binary-classification sigmoid :prediction identity)]
        (->> sparse-outputs
             (reduce (fn [acc s]
                       (let [{:keys [w bias]} (get output s)]
                         (assoc acc s (activation-function (+ (dot w input-list)
                                                              (first bias))))))
                     {}))))))


(defn forward
  [model x-input sparse-outputs]
  (let [{:keys [hidden]} model
        {:keys [w bias]} hidden
        activation-function (:activation hidden)
        state (if (or (set? x-input) (map? x-input))
                (hidden-state-by-sparse model x-input bias)
                (add (mmul w x-input) bias))
        hidden-activation (activation state activation-function)
        output-activation (output-activation model hidden-activation sparse-outputs)]
    {:activation {:input x-input :hidden hidden-activation :output output-activation}
     :state      {:hidden state}}))


;; Back Propagation ;;


(defn output-param-delta
  [item-delta-pairs hidden-size hidden-activation]
  (->> item-delta-pairs
       (reduce (fn [acc [item delta]]
                 (assoc acc item {:w-delta    (emul delta hidden-activation)
                                  :bias-delta (array :vectorz [delta])}))
               {})))

(defn param-delta
  [delta-list bottom-layer-output]
  {:w-delta    (outer-product delta-list bottom-layer-output)
   :bias-delta delta-list})

(defn param-delta:sparse
  [model delta sparse-inputs hidden-size]
  {:sparses-delta (reduce (fn [acc sparse]
                            (cond (set? sparse-inputs)
                                  (assoc acc sparse delta)
                                  (map? sparse-inputs)
                                  (let [[k v] sparse]
                                    (assoc acc k (scale delta v)))))
                          {}
                          sparse-inputs)
   :bias-delta delta})

(defn back-propagation
  "
  training-y have to be given like {\"prediction1\" 12 \"prediction2\" 321} when prediction model.
  In classification model, you put possitive and negative pairs like {:pos #{\"item1\", \"item2\"} :neg #{\"item3\"}}
  "
  [model model-forward training-y]
  (let [{:keys [output hidden hidden-size output-type]} model
        {:keys [activation state]} model-forward
        training-x (:input activation)
        output-delta (error output-type (:output activation) training-y)
        output-param-delta (output-param-delta output-delta hidden-size (:hidden activation))
        propagated-delta (->> output-delta
                              (map (fn [[item delta]]
                                     (let [w (:w (get output item))]
                                       (emul delta w))))
                              (apply add!))
        hidden-delta (emul (derivative (:hidden state) (:activation hidden))
                           propagated-delta)
        hidden-param-delta (if (or (set? training-x) (map? training-x))
                             (param-delta:sparse model hidden-delta training-x hidden-size)
                             (param-delta hidden-delta training-x))]
    {:param-loss {:output-delta output-param-delta
                  :hidden-delta hidden-param-delta}
     :loss output-delta}))


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
    (let [{:keys [sparses w bias]} hidden
          {:keys [sparses-delta w-delta bias-delta]} hidden-delta]
      (->> sparses-delta
           (map (fn [[k v]]
                  (let [word-w (get sparses k)]
                    ;; update hidden w
                    (rewrite! learning-rate word-w v))))
           dorun)
      ;; update hidden w
      (when w-delta (rewrite! learning-rate w w-delta))
      ;; update hidden bias
      (rewrite! learning-rate bias bias-delta)))
  model)


(defn init-model
  [{:keys [input-items input-size hidden-size output-type output-items activation]}]
  {:hidden {:sparses (reduce (fn [acc sparse]
                               (assoc acc sparse (init-vector hidden-size)))
                             {}
                             input-items)
            :w (when input-size (init-matrix input-size hidden-size))
            :bias (init-vector hidden-size)
            :activation activation}
   :output (reduce (fn [acc sparse]
                     (assoc acc sparse {:w    (init-vector hidden-size)
                                        :bias (init-vector 1)}))
                   {}
                   output-items)
   :input-size  input-size
   :hidden-size hidden-size
   :output-type output-type})

