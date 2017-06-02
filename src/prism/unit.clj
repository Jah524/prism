(ns prism.unit
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.core.matrix :refer [set-current-implementation array matrix esum dot shape add! emul emul! mmul
                                 matrix? vec? emap emap! outer-product transpose ecount
                                 matrix array esum dot exp emap sqrt pow]]
    [clojure.core.matrix.random :refer [sample-normal]]
    [clojure.core.matrix.linear :refer [svd]]
    [clojure.core.matrix.operators :as o]
    [clojure.core.matrix.stats :as s]))

(set-current-implementation :vectorz)

(defn sigmoid [x] (/ 1 (+ 1 (Math/exp (- x)))))
(defn sigmoid-derivative [x] (let [s (sigmoid x)] (float (* s (- 1 s)))))

(defn tanh [x] (Math/tanh  x))
(defn tanh-derivative [x] (let [it (Math/tanh x)] (float (- 1 (* it it)))))

(defn linear-derivative-vector [v] (array :vectorz (take (ecount v) (repeat 1))))

(defn clip!
  "v: vector, t: threshould(positive value)"
  [t v]
  (let [tmin (- t)]
    (emap! #(cond (> % t) t (< % tmin) tmin :else %) v)))

(defn rewrite!
  [alpha v! v2]
  (add! v! (emap! #(* alpha %) v2)))


(defn random-array [^Integer n]
  (emap #(* 0.08 %) (sample-normal n)))


(defn init-vector
  [n]
  (array :vectorz (random-array n)))

(defn init-matrix
  [input-num hidden-num]
  (->> (random-array (* input-num hidden-num)) (partition input-num) matrix))

(defn init-orthogonal-matrix
  [n]
  (let [{:keys [U]} (svd (->> (random-array (* n n)) (partition n) matrix))]
    U))


(defn- softmax-states
  [input-list all-output-connection]
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
      {:items item-acc :states (array :vectorz state-acc)})))

(defn multi-class-prob
  [input-list all-output-connection]
  (let [{:keys [items states]} (softmax-states input-list all-output-connection)
        activations (exp states)
        s (esum activations)
        a (o/* (/ 1 s) activations)]
    (->> (mapv vector items a)
         (reduce (fn [acc [item a]]
                   (assoc acc item a))
                 {}))))

(defn activation
  [state activate-fn-key]
  (cond
    (= activate-fn-key :linear)
    state
    :else
    (let [f (condp = activate-fn-key :sigmoid sigmoid :tanh tanh)]
      (emap f state))))

(defn derivative [state activate-fn-key]
  (cond
    (= activate-fn-key :linear)
    (linear-derivative-vector state)
    :else
    (let [f (condp = activate-fn-key
              :sigmoid sigmoid-derivative
              :tanh    tanh-derivative)]
      (emap f state))))

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
  [& maps]
  (if (nil? (first maps))
    (second maps); when acc is nil
    (apply
      (fn m [& maps]
        (if (every? map? maps)
          (apply merge-with m maps)
          (do
            (apply add! maps))))
      maps)))


(defn batch-normalization
  [hidden-size alpha-batch scale shift]
  (let [batch-size (count alpha-batch)
        alpha-batch-matrix (->> (apply concat (seq alpha-batch)) (partition hidden-size) matrix)
        mean-by-dim (s/mean alpha-batch-matrix)
        epsilon   0.001
        sig+ep    (o/+ (s/variance alpha-batch-matrix) (array :vectorz (repeat hidden-size epsilon)))
        sd-by-dim (sqrt sig+ep)
        sd-div    (o// 1 sd-by-dim)]
    {:preactivation-batch (->> alpha-batch
                               (mapv (fn [alpha]
                                       (let [alpha-myu (o/- alpha mean-by-dim)
                                             alpha-hat (o/* sd-div alpha-myu)]
                                         {:state (o/+ (o/* alpha-hat scale) shift)
                                          :alpha-hat alpha-hat}))))
     :alpha-batch alpha-batch
     :x-mean mean-by-dim
     :sig+ep sig+ep
     :x-sd sd-by-dim
     :x-sd-div sd-div}))

(defn batch-normalization-with-population
  [hidden-size pop-mean pop-variance alpha scale shift]
  (let [epsilon 0.001]
    (o/+ (o/* (o/- alpha pop-mean)
              (emap (fn [x] (/ 1 (Math/sqrt x))) (o/+ pop-variance (array :vectorz (repeat hidden-size epsilon))))
              scale)
         shift)))

(defn batch-normalization-delta
  [hidden-delta-batch scale bn-forward]
  (let [{:keys [batch-size alpha-batch x-mean sig+ep x-sd x-sd-div alpha-hat preactivation-batch]} bn-forward
        alpha-hat-delta-batch (->> hidden-delta-batch (mapv (fn [hidden-delta] (o/* hidden-delta scale))))
        x-myu-batch (o/- alpha-batch x-mean)
        v-delta (o/* (->> (mapv (fn [alpha-hat-delta x-myu]
                                  (o/* alpha-hat-delta x-myu))
                                alpha-hat-delta-batch
                                x-myu-batch)
                          (apply o/+))
                     (o/* (/ -1 2)
                          (pow sig+ep (/ -3 2))))
        _v (o/* -1 x-sd-div)
        mean-delta (o/+ (->> alpha-hat-delta-batch
                             (mapv (fn [alpha-hat-delta] (o/* alpha-hat-delta _v)))
                             (apply o/+))
                        (o/* v-delta
                             (->> x-myu-batch
                                  (mapv (fn [x-myu] (o/* -2 x-myu)))
                                  (apply o/+)
                                  (o/* (/ 1 batch-size)))))]
    (mapv (fn [hidden-delta alpha-hat-delta x-myu batch-item]
            (let [{:keys [alpha-hat]} batch-item
                  scale-delta (o/* hidden-delta alpha-hat)
                  bn-delta (o/+ (o/* alpha-hat-delta x-sd-div)
                                (o/* v-delta (o/* (/ 2 batch-size) x-myu))
                                (o/* (/ 1 batch-size) mean-delta))]
              {:scale-delta scale-delta
               :shift-delta hidden-delta
               :bn-delta bn-delta}))
          hidden-delta-batch
          alpha-hat-delta-batch
          x-myu-batch
          preactivation-batch)))



(defn layer-normalization
  [alpha gain bias]
  (let [sigma (s/sd alpha)
        normalized-preactivation (o/* (/ 1 sigma) (o/- alpha (s/mean alpha)))
        alpha-bar (o/* normalized-preactivation gain)]
    {:state (o/+ alpha-bar bias)
     :alpha-bar alpha-bar
     :normalized-preactivation normalized-preactivation
     :sigma sigma}))

(defn layer-normalization-delta
  [gain hidden-delta sigma normalized-preactivation alpha-bar]
  (let [gain-delta (o/* hidden-delta normalized-preactivation)
        d1 (o/* hidden-delta gain)
        d1-mean (s/mean d1)
        tmp-left (o/- d1 d1-mean)
        d2 (o/* hidden-delta alpha-bar)
        d2-mean (s/mean d2)
        tmp-right  (o/* d2-mean normalized-preactivation)]
    {:gain-delta gain-delta
     :ln-delta (o/* (/ 1 sigma) (o/- tmp-left tmp-right))}))

