(ns prism.unit
  (:require [clojure.pprint :refer [pprint]]))

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
