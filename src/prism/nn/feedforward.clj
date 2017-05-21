(ns prism.nn.feedforward
  (:require
    [clojure.pprint :refer [pprint]]
    [matrix.default :as default]
    [prism.unit :refer [activation multi-class-prob derivative error]]
    [prism.util :as util]))


(defn hidden-state-by-sparse
  [model x-input bias]
  (let [{:keys [hidden matrix-kit]} model
        {:keys [sparses]} hidden
        {:keys [plus scal]} matrix-kit]
    (->> x-input
         (reduce (fn [acc item]
                   (cond (set? x-input)
                         (let [w (get sparses item)]
                           (plus acc w))
                         (map? x-input)
                         (let [[k v] item
                               w (get sparses k)]
                           (plus acc (scal v w)))))
                 bias))))


(defn output-activation
  [model input-list sparse-outputs]
  (let [{:keys [output-type output matrix-kit sigmoid]} model
        {:keys [dot sigmoid]} matrix-kit]
    (if (= output-type :multi-class-classification)
      (multi-class-prob matrix-kit input-list output)
      (let [activation-function (condp = output-type :binary-classification sigmoid :prediction identity)]
        (->> sparse-outputs
             (reduce (fn [acc s]
                       (let [{:keys [w bias]} (get output s)]
                         (assoc acc s (activation-function (+ (dot w input-list)
                                                              (first bias))))))
                     {}))))))


(defn network-output
  [model x-input sparse-outputs]
  (let [{:keys [hidden matrix-kit]} model
        {:keys [w bias]} hidden
        activation-function (:activation hidden)
        {:keys [plus gemv matrix-kit-type native-dv]} matrix-kit
        state (if (or (set? x-input) (map? x-input))
                (hidden-state-by-sparse model x-input bias)
                (plus (gemv w x-input) bias))
        hidden-activation (activation state activation-function matrix-kit)
        output-activation (output-activation model hidden-activation sparse-outputs)]
    {:activation {:input x-input :hidden hidden-activation :output output-activation}
     :state      {:hidden state}}))


;; Back Propagation ;;


(defn output-param-delta
  [model item-delta-pairs hidden-size hidden-activation]
  (let [{:keys [scal make-vector]} (:matrix-kit model)]
    (->> item-delta-pairs
         (reduce (fn [acc [item delta]]
                   (assoc acc item {:w-delta    (scal delta hidden-activation)
                                    :bias-delta (make-vector [delta])}))
                 {}))))

(defn param-delta
  [model delta-list bottom-layer-output]
  (let [{:keys [outer]} (:matrix-kit model)]
    {:w-delta    (outer delta-list bottom-layer-output)
     :bias-delta delta-list}))

(defn param-delta:sparse
  [model delta sparse-inputs hidden-size]
  (let [{:keys [scal]} (:matrix-kit model)]
    {:sparses-delta (reduce (fn [acc sparse]
                              (cond (set? sparse-inputs)
                                    (assoc acc sparse delta)
                                    (map? sparse-inputs)
                                    (let [[k v] sparse]
                                      (assoc acc k (scal v delta)))))
                            {}
                            sparse-inputs)
     :bias-delta delta}))

(defn back-propagation
  "
  training-y have to be given like {\"prediction1\" 12 \"prediction2\" 321} when prediction model.
  In classification model, you put possitive and negative pairs like {:pos #{\"item1\", \"item2\"} :neg #{\"item3\"}}
  "
  [model model-forward training-y]
  (let [{:keys [output hidden hidden-size output-type matrix-kit]} model
        {:keys [activation state]} model-forward
        training-x (:input activation)
        {:keys [scal plus times matrix-kit-type native-dv]} matrix-kit
        output-delta (error output-type (:output activation) training-y)
        output-param-delta (output-param-delta model output-delta hidden-size (:hidden activation))
        propagated-delta (->> output-delta
                              (map (fn [[item delta]]
                                     (let [w (:w (get output item))]
                                       (scal delta w))))
                              (apply plus))
        hidden-delta (times (derivative (:hidden state) (:activation hidden) matrix-kit)
                            propagated-delta)
        hidden-param-delta (if (or (set? training-x) (map? training-x))
                             (param-delta:sparse model hidden-delta training-x hidden-size)
                             (param-delta model hidden-delta training-x))]
    {:param-loss {:output-delta output-param-delta
                  :hidden-delta hidden-param-delta}
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
    (let [{:keys [sparses w bias]} hidden
          {:keys [sparses-delta w-delta bias-delta]} hidden-delta]
      (->> sparses-delta
           (map (fn [[k v]]
                  (let [word-w (get sparses k)]
                    ;; update hidden w
                    (rewrite-vector! learning-rate word-w v))))
           dorun)
      ;; update hidden w
      (when w-delta (rewrite-matrix! learning-rate w w-delta))
      ;; update hidden bias
      (rewrite-vector! learning-rate bias bias-delta)))
  model)


(defn init-model
  [{:keys [input-items input-size hidden-size output-type output-items activation matrix-kit]
    :or {matrix-kit default/default-matrix-kit}}]
  (let [{:keys [type init-vector init-matrix]} matrix-kit]
    (println (str "initializing model as " (if (= type :native) "native-array" "vectorz") " ..."))
    {:matrix-kit matrix-kit
     :hidden {:sparses (reduce (fn [acc sparse]
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
     :output-type output-type}))

(defn convert-model
  [model new-matrix-kit]
  (let [{:keys [hidden output input-size hidden-size]} model
        {:keys [make-vector make-matrix] :as matrix-kit} (or new-matrix-kit default/default-matrix-kit)]
    (assoc model
      :matrix-kit matrix-kit
      :hidden (let [{:keys [w bias sparse]} hidden]
                (-> hidden
                    (assoc
                      :sparse (reduce (fn [acc [word em]]
                                        (assoc acc word (make-vector (seq em)))) {} w)
                      :w (make-matrix input-size hidden-size (apply concat (seq w)))
                      :bias (make-vector (seq bias)))))
      :output (reduce (fn [acc [item {:keys [w bias]}]]
                        (assoc acc item {:w (make-vector (seq w)) :bias (make-vector (seq bias))}))
                      {}
                      output))))

(defn load-model
  [target-path matrix-kit]
  (convert-model (util/load-model target-path) matrix-kit))
