(ns sai-ai.nn.feedforward
  (:require [sai-ai.unit :refer [activation derivative model-rand]]
            [matrix.default :as default]))

(defn hidden-state-by-sparse
  [w sparse-inputs hidden-size & [option]]
  (let [gemv (if-let [it (:gemv option)] it default/gemv)
        all-input-size (/ (count w) hidden-size)
        sparse-size (count sparse-inputs)
        ks (keys sparse-inputs)]
    (reduce (fn [acc [k v]]
               (default/sum acc
                            (default/times (get w k) (float-array (take hidden-size (repeat v))))))
            (float-array hidden-size)
            (vec sparse-inputs))))

;; (vec (hidden-state-by-sparse {"natural" (float-array [2 3]) "language" (float-array [10 20])}
;;                              {"natural" -0.5 "language" 1} 2))


;{"natural" -1 "language" 1}
;(vec (hidden-state-by-sparse (float-array (range 6)) {1 1.0} 2))

(defn network-output [model x-list & [option]]
  (let [gemv (if-let [it (:gemv option)] it default/gemv)]
;;         x-list (float-array x-list)]
    (loop [layers (:layers model), input-list x-list, acc [{:activation x-list}]]
      (if-let [layer (first layers)]
        (let [{activate-fn :activate-fn w :w bias :bias unit-num :unit-num layer-type :layer-type}  layer
              state-list (default/sum (cond (and (= acc [{:activation x-list}]);input-layer
                                                 (= :sparse (:input-type model)))
                                            (hidden-state-by-sparse w input-list unit-num option)
                                            :else
                                            (gemv w input-list))
                                      bias)
              activation-list (activation state-list activate-fn)]
          (recur (rest layers) activation-list (cons {:activation activation-list :state state-list} acc)))
        (vec (reverse acc))))))


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


(defn back-propagation [model training-x training-y & [option]]
  (let [gemv (if-let [it (:gemv option)] it default/gemv)
        training-y (float-array training-y)]
    (loop [layers' (reverse (:layers model))
           output-layers' (reverse (network-output model training-x option))
           propagated-delta (default/minus training-y (:activation (first output-layers')))
           acc []]
      (if-let [layer (first layers')]
        (let [{:keys [w unit-num layer-type]} layer
              delta (if (= layer-type :output)
                      propagated-delta
                      ;(mapv #(float (* %1 %2))
                      (default/times
                            propagated-delta
                            (derivative (:state (first output-layers')) (:activate-fn (first layers')))))
              input-size (/ (count w) unit-num)
              input-list (:activation (second output-layers'))
              param-delta (if (and (nil? (second layers'));equals input-layer?
                                   (= :sparse (:input-type model)))
                            (param-delta:sparse delta training-x unit-num)
                            (param-delta delta input-list))]
          (recur (rest layers')
                 (rest output-layers')
                 (cond (nil? (second layers'))
                       nil
                       :else
                       (gemv (default/transpose unit-num w) delta))
                 (cons param-delta acc)))
        acc))))


(defn update-model [model delta-list learning-rate]
  (loop [layers (:layers model)
         delta-list delta-list]
    (if-let [layer (first layers)]
      (let [delta-layer (first delta-list)
            {:keys [w bias unit-num layer-type]} layer
            {:keys [w-delta bias-delta]} delta-layer
            _ (if (and (= layer-type :hidden)
                       (= :sparse (:input-type model)))
                (->> (vec w-delta)
                     (mapv (fn [[k v]]
                             (let [word-w (get w k)]
                               (dotimes [x unit-num]
                                 (aset ^floats word-w x (float (+ (aget ^floats word-w x) (* learning-rate (aget ^floats v x))))))))))
                (dotimes [x (count w)]
                  (let [d (aget ^floats w-delta x)]
                    (when-not (zero? d) (aset ^floats w x (float (+ (aget ^floats w x) (* learning-rate d))))))))
            _ (dotimes [x unit-num]
                (let [d (aget ^floats bias-delta x)]
                  (when-not (zero? d) (aset ^floats bias x (float (+ (aget ^floats bias x) (* learning-rate d)))))))]
        (recur (rest layers)
               (rest delta-list)))
      model)))



(defn init-model [model-design & [wl]]
  (let [{:keys [input-type output-type]} model-design]
    (loop [layers-info (:layers model-design),
           acc []]
      (if-let [layer-info (second layers-info)]
        (let [{next-n :unit-num a :activate-fn next-layer-type :layer-type} layer-info
              {layer-type :layer-type n :unit-num} (first layers-info)
              w (cond (and (= next-layer-type :output) (= output-type :sparse))
                      (reduce #(assoc %1 %2 (float-array (take n (repeatedly model-rand)))) {} (keys wl))
                      (and (= layer-type :input) (= input-type :sparse))
                      (reduce #(assoc %1 %2 (float-array (take next-n (repeatedly model-rand)))) {} (keys wl))
                      :else
                      (let [it (float-array (* next-n n))]
                        (dotimes [x (* next-n n)] (aset ^floats it x (model-rand)))
                        it))
              bias ;(if (and (= next-layer-type :output) (= output-type :sparse))
                    ; (reduce #(assoc %1 %2 (float-array [(model-rand)])) {} (keys wl))
              (let [it (float-array next-n)]
                (dotimes [x next-n] (aset ^floats it x (model-rand)))
                it)]
          (recur (rest layers-info)
                 (cons {:activate-fn a
                        :layer-type layer-type
                        :unit-num next-n
                        :w w
                        :bias bias}
                       acc)))
        (assoc model-design :layers (reverse acc))))))


