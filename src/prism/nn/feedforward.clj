(ns prism.nn.feedforward
  (:require
    [clojure.pprint :refer [pprint]]
    [matrix.default :as default]
    [prism.unit :refer [activation derivative binary-classification-error prediction-error]]
    [prism.util :as util]))


(defn hidden-state-by-sparse
  [model sparse-inputs bias]
  (let [{:keys [hidden matrix-kit]} model
        {:keys [w unit-num]} hidden
        {:keys [plus scal]} matrix-kit]
    (reduce (fn [acc sparse]
              (cond (set? sparse-inputs)
                    (plus acc (get w sparse))
                    (map? sparse-inputs)
                    (let [[k v] sparse]
                      (plus acc (scal v (get w k))))))
            bias
            sparse-inputs)))


(defn output-activation
  [model input-list sparse-outputs]
  (let [{:keys [output-type output matrix-kit sigmoid]} model
        {:keys [dot sigmoid]} matrix-kit
        activation-function (condp = output-type :binary-classification sigmoid :prediction identity)]
    (if (= output-type :multi-class-classification)
      :FIXME
      (reduce (fn [acc s]
                (let [{:keys [w bias]} (get output s)]
                  (assoc acc s (activation-function (+ (dot w input-list)
                                                       (first bias))))))
              {}
              sparse-outputs))))


(defn network-output
  [model x-input sparse-outputs]
  (let [{:keys [hidden input-type matrix-kit]} model
        {:keys [w bias unit-num]} hidden
        activation-function (:activation hidden)
        {:keys [plus gemv matrix-kit-type native-dv]} matrix-kit
        state (if (= :sparse input-type)
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
    {:w-delta (reduce (fn [acc sparse]
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
  (let [{:keys [output hidden input-type output-type matrix-kit]} model
        {:keys [unit-num]} hidden
        {:keys [activation state]} model-forward
        training-x (:input activation)
        {:keys [pos neg]} training-y   ;used when binary claassification
        {:keys [scal plus times matrix-kit-type native-dv]} matrix-kit
        output-delta (condp = (:output-type model)
                       :binary-classification
                       (binary-classification-error (:output activation) pos neg)
                       :prediction
                       (prediction-error (:output activation) training-y))
        output-param-delta (output-param-delta model output-delta unit-num (:hidden activation))
        propagated-delta (->> output-delta
                              (map (fn [[item delta]]
                                     (let [w (:w (get output item))]
                                       (scal delta w))))
                              (apply plus))
        hidden-delta (times (derivative (:hidden state) (:activation hidden) matrix-kit)
                            propagated-delta)
        hidden-param-delta (if (= :sparse input-type)
                             (param-delta:sparse model hidden-delta training-x unit-num)
                             (param-delta model hidden-delta training-x))]
    {:param-loss {:output-delta output-param-delta
                  :hidden-delta hidden-param-delta}
     :loss output-delta}))


(defn update-model! [model param-delta learning-rate]
  (let [{:keys [output hidden input-type matrix-kit]} model
        {:keys [output-delta hidden-delta]} param-delta
        {:keys [unit-num]} hidden
        {:keys [type rewrite-vector! rewrite-matrix!]} matrix-kit]
    ;; update output
    (->> output-delta
         (map (fn [[item {:keys [w-delta bias-delta]}]]
                (let [{:keys [w bias]} (get output item)]
                  ;update output w
                  (rewrite-vector! learning-rate w w-delta)
                  ;update output bias
                  (rewrite-vector! learning-rate bias bias-delta))))
         doall)
    ;; update hidden
    (let [{:keys [w bias]} hidden
          {:keys [w-delta bias-delta]} hidden-delta]
      (if (= :sparse (:input-type model))
        (->> w-delta
             (map (fn [[k v]]
                    (let [word-w (get w k)]
                      ;; update hidden w
                      (rewrite-vector! learning-rate word-w v))))
             dorun)
        ;; update hidden w
        (rewrite-matrix! learning-rate w w-delta))
      ;; update hidden bias
      (rewrite-vector! learning-rate bias bias-delta)))
  model)


(defn init-model
  [{:keys [input-type input-items input-size hidden-size output-type output-items activation matrix-kit]
    :or {matrix-kit default/default-matrix-kit}}]
  (let [sparse-input? (= input-type :sparse)
        {:keys [type init-vector init-matrix]} matrix-kit]
    (println (str "initializing model as " (if (= type :native) "native-array" "float-array") " ..."))
    {:matrix-kit matrix-kit
     :weight-type type
     :hidden (-> (if (= input-type :sparse)
                   (let [sparses (reduce (fn [acc sparse]
                                           (assoc acc sparse (init-vector hidden-size)))
                                         {}
                                         input-items)]
                     {:w sparses})
                   {:w (init-matrix input-size hidden-size)})
                 (assoc
                   :bias (init-vector hidden-size)
                   :unit-num hidden-size
                   :activation activation))
     :output (reduce (fn [acc sparse]
                       (assoc acc sparse {:w    (init-vector hidden-size)
                                          :bias (init-vector 1)}))
                     {}
                     output-items)
     :input-type input-type
     :output-type output-type
     :unit-nums [(if sparse-input? (count input-items) input-size) hidden-size (count output-items)]}))

(defn convert-model
  [model new-matrix-kit]
  (let [{:keys [hidden output input-type unit-nums]} model
        [input-num hidden-num] unit-nums
        {:keys [make-vector make-matrix] :as matrix-kit} (or new-matrix-kit default/default-matrix-kit)]
    (assoc model
      :matrix-kit matrix-kit
      :hidden (let [{:keys [w bias]} hidden]
                (-> hidden
                    (assoc
                      :w (if (= input-type :sparse)
                           (reduce (fn [acc [word em]] (assoc acc word (make-vector (seq em)))) {} w)
                           (make-matrix input-num hidden-num (apply concat (seq w))))
                      :bias (make-vector (seq bias)))))
      :output (reduce (fn [acc [item {:keys [w bias]}]]
                        (assoc acc item {:w (make-vector (seq w)) :bias (make-vector (seq bias))}))
                      {}
                      output))))

(defn load-model
  [target-path matrix-kit]
  (convert-model (util/load-model target-path) matrix-kit))
