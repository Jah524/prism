(ns sai-ai.nn.feedforward
  (:require
    [clojure.pprint :refer [pprint]]
    [sai-ai.unit :refer [activation derivative model-rand random-array]]
    [matrix.default :refer [transpose sum times outer minus] :as default]
    [sai-ai.unit :refer [sigmoid tanh activation derivative ]]))


(defn hidden-state-by-sparse
  [model sparse-inputs & [option]]
  (let [gemv (if-let [it (:gemv option)] it default/gemv)
        {:keys [hidden]} model
        {:keys [w unit-num]} hidden]
    (reduce (fn [acc [k v]]
              (default/sum acc (default/times (get w k) (float-array (take unit-num (repeat v))))))
            (float-array unit-num)
            sparse-inputs)))


(defn output-activation
  [model input-list sparse-outputs & [option]]
  (let [{:keys [output-type output]} model
        {:keys [w bias activation]} output
        activation-function (condp = output-type :binary-classification sigmoid :prediction identity)]
    (if (= output-type :multi-class-classification)
      :FIXME
      (reduce (fn [acc s]
                (assoc acc s (activation-function (+ (reduce + (times (get w s) input-list)) (aget ^floats (get bias s) 0)))))
              {}
              sparse-outputs))))


(defn network-output [model x-input sparse-outputs & [option]]
  (let [gemv (if-let [it (:gemv option)] it default/gemv)
        {:keys [hidden input-type]} model
        {:keys [w bias unit-num]} hidden
        activation-function (:activation hidden)
        state (sum bias (if (= :sparse input-type)
                          (hidden-state-by-sparse model x-input option)
                          (gemv w x-input)))
        hidden-activation (activation state activation-function)
        output-activation (output-activation model hidden-activation sparse-outputs option)]
    {:activation {:input x-input :hidden hidden-activation :output output-activation}
     :state      {:hidden state}}))


;; Back Propagation ;;

(defn output-param-delta
  [item-delta-pairs hidden-size hidden-activation]
  (->> item-delta-pairs
       (reduce (fn [acc [item delta]]
                 (assoc acc item {:w-delta    (times hidden-activation (float-array (repeat hidden-size delta)))
                                  :bias-delta (float-array [delta])}))
               {})))

(defn param-delta
  "for standard-unit at output layer"
  [delta-list bottom-layer-output]
  {:w-delta    (default/outer delta-list bottom-layer-output)
   :bias-delta delta-list})

(defn param-delta:sparse [delta sparse-inputs hidden-size]
  {:w-delta (reduce (fn [acc [k v]] (assoc acc k (default/times delta (float-array (take hidden-size (repeat v))))))
                    {}
                    (vec sparse-inputs))
   :bias-delta delta})

(defn binary-classification-error
  [hidden-size activation positives negatives & [option]]
  (let [negatives (remove (fn [n] (some #(= % n) positives)) negatives)
        ps (map (fn [p] [p (float (- 1 (get activation p)))]) positives)
        ns (map (fn [n] [n (float (- (get activation n)))]) negatives)]
    (vec (concat ps ns))))

(defn prediction-error
  [hidden-size activation predictions & option]
  (when-not (= :skip predictions)
    (->> predictions
         (mapv (fn [[item expect-value]]
                 [item (float (- expect-value (get activation item)))])))))

(defn back-propagation [model training-x training-y & [option]]
  (let [gemv (if-let [it (:gemv option)] it default/gemv)
        {:keys [output hidden input-type output-type]} model
        {:keys [unit-num]} hidden
        {:keys [activation state]} (network-output model training-x (keys training-y) option)
        {:keys [pos neg]} training-y   ;used when binary claassification
        output-w (:w output)
        output-delta (condp = (:output-type model)
                       :binary-classification
                       (binary-classification-error unit-num (:output activation) pos neg)
                       :prediction
                       (prediction-error unit-num  (:output activation) training-y))
        output-param-delta (output-param-delta training-y unit-num (:hidden activation))
        propagated-delta (->> output-delta
                              (map (fn [[item delta]]
                                     (let [w (get output-w item)
                                           v (float-array (repeat unit-num delta))]
                                       (times w v))))
                              (apply sum))
        hidden-delta (times (derivative (:hidden state) (:activation hidden))
                            propagated-delta)
        hidden-param-delta (if (= :sparse input-type)
                             (param-delta:sparse hidden-delta training-x unit-num)
                             (param-delta hidden-delta training-x))]
    {:output-delta output-param-delta
     :hidden-delta hidden-param-delta}))


(defn update-model! [model param-delta learning-rate]
  (let [{:keys [output hidden input-type]} model
        {:keys [output-delta hidden-delta]} param-delta
        {:keys [unit-num]} hidden]
    ;; update output
    (let [{:keys [w bias]} output]
      (->> output-delta
           (map (fn [[item {:keys [w-delta bias-delta]}]]
                  (let [item-w    (get w item)
                        item-bias (get bias item)]
                    ;update output w
                    (dotimes [x unit-num]
                      (aset ^floats item-w x (float (+ (aget ^floats item-w x) (* learning-rate (aget ^floats w-delta x))))))
                    ;update output bias
                    (aset ^floats item-bias 0 (float (+ (aget ^floats item-bias 0) (* learning-rate (aget ^floats bias-delta 0))))))))
           doall))
    ;; update hidden
    (let [{:keys [w bias]} hidden
          {:keys [w-delta bias-delta]} hidden-delta]
      (if (= :sparse (:input-type model))
        (->> w-delta
             (map (fn [[k v]]
                    (let [word-w (get w k)]
                      ;; update hidden w
                      (dotimes [x unit-num]
                        (aset ^floats word-w x (float (+ (aget ^floats word-w x) (* learning-rate (aget ^floats v x)))))))))
             doall)
        ;; update hidden w
        (dotimes [x (count w)]
          (aset ^floats w x (float (+ (aget ^floats w x) (* learning-rate (aget ^floats w-delta x)))))))
      ;; update hidden bias
      (dotimes [x unit-num]
        (aset ^floats bias x (float (+ (aget ^floats bias x) (* learning-rate (aget ^floats bias-delta x))))))))
  model)



(defn init-model
  [{:keys [input-type input-items input-size hidden-size output-type output-items activation]}]
  (let [sparse-input? (= input-type :sparse)]
    {:hidden (-> (if (= input-type :sparse)
                   (let [sparses (reduce (fn [acc sparse]
                                           (assoc acc sparse {:w    (random-array hidden-size)
                                                              :bias (random-array hidden-size)}))
                                         {}
                                         input-items)]
                     {:w sparses})
                   {:w (random-array (* input-size hidden-size))})
                 (assoc
                   :bias (random-array hidden-size)
                   :unit-num hidden-size
                   :activation activation))
     :output {:w    (reduce #(assoc %1 %2 (random-array hidden-size))   {} output-items)
              :bias (reduce #(assoc %1 %2 (float-array [(model-rand)])) {} output-items)}
     :input-type input-type
     :output-type output-type
     :unit-nums [(if sparse-input? (count input-items) input-size) hidden-size (count output-items)]}))

(defn train!
  [model x-input positives negatives learning-rate & [option]]
  (let [param-delta (back-propagation model x-input positives negatives option)]
    (update-model! model param-delta learning-rate)))
