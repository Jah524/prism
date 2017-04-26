(ns prism.unit)


(defn softmax [y-list]
  (let [n (count y-list)
        ret (float-array n)
        m (float (apply max y-list))
        sum-y (areduce ^floats y-list i acc (float 0) (+ acc (float (Math/exp (- (aget ^floats y-list i) m)))))
        _ (dotimes [x n] (aset ^floats ret x (float (/ (Math/exp (- (aget ^floats y-list x) m)) sum-y))))]
    ret))

(defn activation
  [state activate-fn-key matrix-kit]
  (let [{:keys [alter-vec sigmoid tanh]} matrix-kit]
    (cond
      (= activate-fn-key :softmax)
      (softmax state)
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


(defn binary-classification-error
  [activation positives negatives]
  (let [negatives (remove (fn [n] (some #(= % n) positives)) negatives)
        ps (map (fn [p] [p (float (- 1 (get activation p)))]) positives)
        ns (map (fn [n] [n (float (- (get activation n)))]) negatives)]
    (reduce (fn [acc [k v]] (assoc acc k v)) {} (concat ps ns))))

(defn prediction-error
  [activation expectation]
  (if (= :skip expectation)
    {}
    (->> expectation
         (reduce (fn [acc [item expect-value]]
                   (assoc acc item (float (- expect-value (get activation item)))))
                 {}))))
