(ns prism.unit
  (:require [clojure.pprint :refer [pprint]]
            [clojure.core.matrix.operators :as o]
            [clojure.core.matrix.stats :as s]))

(defn- softmax-states
  [matrix-kit input-list all-output-connection]
  (let [{:keys [dot make-vector]} matrix-kit]
    (loop [coll (vec all-output-connection)
           item-acc [],
           state-acc []]
      (if-let [f (first coll)]
        (let [[item v] f
              {:keys [w bias]} v
              state (+ (dot w input-list) (first bias))
              state (cond (> state 25) 25 (< state -25) -25 :else state)]
          (recur (rest coll)
                 (conj item-acc item)
                 (conj state-acc state)))
        {:items item-acc :states (make-vector state-acc)}))))

(defn multi-class-prob
  [matrix-kit input-list all-output-connection]
  (let [{:keys [sum scal alter-vec exp]} matrix-kit
        {:keys [items states]} (softmax-states matrix-kit input-list all-output-connection)
        activations (alter-vec states exp)
        s (sum activations)
        a (scal (/ 1 s) activations)]
    (->> (mapv vector items a)
         (reduce (fn [acc [item a]]
                   (assoc acc item a))
                 {}))))

(defn activation
  [state activate-fn-key matrix-kit]
  (let [{:keys [alter-vec sigmoid tanh]} matrix-kit]
    (cond
      (= activate-fn-key :linear)
      state
      :else
      (let [f (condp = activate-fn-key :sigmoid sigmoid :tanh tanh)]
        (alter-vec state f)))))

(defn derivative [state activate-fn-key matrix-kit]
  (let [{:keys [alter-vec sigmoid-derivative tanh-derivative linear-derivative-vector]} matrix-kit]
    (cond
      (= activate-fn-key :linear)
      (linear-derivative-vector state)
      :else
      (let [f (condp = activate-fn-key
                :sigmoid sigmoid-derivative
                :tanh    tanh-derivative)]
        (alter-vec state f)))))

(defn multi-classification-error
  [activation expectation]
  (if (= :skip expectation)
    {}
    (->> (dissoc activation expectation)
         keys
         (reduce (fn [acc item]
                   (assoc acc item  (- (get activation item))))
                 {expectation (- 1 (get activation expectation))}))))


(defn binary-classification-error
  [activation expectation]
  (let [{:keys [pos neg]} expectation
        neg (remove (fn [n] (some #(= % n) pos)) neg)
        ps (map (fn [p] [p (- 1 (get activation p))]) pos)
        ns (map (fn [n] [n (- (get activation n))]) neg)]
    (reduce (fn [acc [k v]] (assoc acc k v)) {} (concat ps ns))))

(defn prediction-error
  [activation expectation]
  (if (= :skip expectation)
    {}
    (->> expectation
         (reduce (fn [acc [item expect-value]]
                   (assoc acc item (- expect-value (get activation item))))
                 {}))))

(defn error
  [fn-key activation expectation]
  (condp = fn-key
    :multi-class-classification
    (multi-classification-error activation expectation)
    :binary-classification
    (binary-classification-error activation expectation)
    :prediction
    (prediction-error activation expectation)))


(defn merge-param
  [plus & maps]
  (if (nil? (first maps))
    (second maps); when acc is nil
    (apply
      (fn m [& maps]
        (if (every? map? maps)
          (apply merge-with m maps)
          (do
            (apply plus maps))))
      maps)))


(defn batch-normalization
  [matrix-kit hidden-size alpha-batch scale shift]
  (let [{:keys [plus minus times alter-vec mean make-vector make-matrix]} matrix-kit
        batch-size (count alpha-batch)
        alpha-batch-matrix (make-matrix hidden-size batch-size (apply concat (seq alpha-batch)))
        mean-by-dim (mean alpha-batch-matrix)
        epsilon 0.001
        sig+ep    (plus (s/variance alpha-batch-matrix) (make-vector (repeat hidden-size epsilon)))
        sd-by-dim (alter-vec sig+ep (fn ^double [^double x] (Math/sqrt x)))
        sd-div    (alter-vec sd-by-dim (fn ^double [^double x] (/ 1 x)))]
    {:preactivation-batch (->> alpha-batch
                               (mapv (fn [alpha]
                                       (let [alpha-myu (minus alpha mean-by-dim)
                                             alpha-hat (times sd-div alpha-myu)]
                                         {:state (plus (times alpha-hat scale) shift)
                                          :alpha-hat alpha-hat}))))
     :alpha-batch alpha-batch
     :x-mean mean-by-dim
     :sig+ep sig+ep
     :x-sd sd-by-dim
     :x-sd-div sd-div}))

(defn batch-normalization-with-population
  [matrix-kit hidden-size pop-mean pop-variance alpha scale shift]
  (let [{:keys [plus minus times alter-vec make-vector]} matrix-kit
        epsilon 0.001]
    (plus (times (minus alpha pop-mean)
                 (alter-vec (plus pop-variance (make-vector (repeat hidden-size epsilon)))
                            (fn ^double [^double x] (/ 1 (Math/sqrt x))))
                 scale)
          shift)))

(defn batch-normalization-delta
  [matrix-kit hidden-delta-batch scale bn-forward]
  (let [{:keys [sum plus minus times scal mean transpose alter-vec]} matrix-kit
        {:keys [batch-size alpha-batch x-mean sig+ep x-sd x-sd-div alpha-hat preactivation-batch]} bn-forward
        alpha-hat-delta-batch (->> hidden-delta-batch (mapv (fn [hidden-delta] (times hidden-delta scale))))
        x-myu-batch (minus alpha-batch x-mean)
        v-delta (times (->> (mapv (fn [alpha-hat-delta x-myu]
                                    (times alpha-hat-delta x-myu))
                                  alpha-hat-delta-batch
                                  x-myu-batch)
                            (apply plus))
                       (scal (/ -1 2)
                             (alter-vec sig+ep (fn ^double [^double x] (Math/pow x (/ -3 2))))))
        _v (scal -1 x-sd-div)
        mean-delta (plus (->> alpha-hat-delta-batch
                              (mapv (fn [alpha-hat-delta] (times alpha-hat-delta _v)))
                              (apply plus))
                         (times v-delta
                                (->> x-myu-batch
                                     (mapv (fn [x-myu] (scal -2 x-myu)))
                                     (apply plus)
                                     (scal (/ 1 batch-size)))))]
    (mapv (fn [hidden-delta alpha-hat-delta x-myu batch-item]
            (let [{:keys [alpha-hat]} batch-item
                  scale-delta (times hidden-delta alpha-hat)
                  bn-delta (plus (times alpha-hat-delta x-sd-div)
                                 (times v-delta (scal (/ 2 batch-size) x-myu))
                                 (scal (/ 1 batch-size) mean-delta))]
              {:scale-delta scale-delta
               :shift-delta hidden-delta
               :bn-delta bn-delta}))
          hidden-delta-batch
          alpha-hat-delta-batch
          x-myu-batch
          preactivation-batch)))



(defn layer-normalization
  [matrix-kit alpha gain bias]
  (let [{:keys [plus minus times scal mean sd]} matrix-kit
        sigma (sd alpha)
        normalized-preactivation (scal (/ 1 sigma) (minus alpha (mean alpha)))
        alpha-bar (times normalized-preactivation gain)]
    {:state (plus alpha-bar bias)
     :alpha-bar alpha-bar
     :normalized-preactivation normalized-preactivation
     :sigma sigma}))

(defn layer-normalization-delta
  [matrix-kit gain hidden-delta sigma normalized-preactivation alpha-bar]
  (let [{:keys [minus times scal mean]} matrix-kit
        gain-delta (times hidden-delta normalized-preactivation)
        d1 (times hidden-delta gain)
        d1-mean (mean d1)
        tmp-left (minus d1 d1-mean)
        d2 (times hidden-delta alpha-bar)
        d2-mean (mean d2)
        tmp-right  (scal d2-mean normalized-preactivation)]
    {:gain-delta gain-delta
     :ln-delta (scal (/ 1 sigma) (minus tmp-left tmp-right))}))

