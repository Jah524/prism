(ns prism.unit)

(defn softmax [matrix-kit v]
  (let [{:keys [sum minus scal alter-vec make-vector exp]} matrix-kit
;;         m (apply max v)
;;         normalized-v (minus v (make-vector (repeat (count v) m)))
        converted-v (alter-vec v exp)
        s (sum converted-v)]
    (scal (/ 1 s) converted-v)))

(defn activation
  [state activate-fn-key matrix-kit]
  (let [{:keys [alter-vec sigmoid tanh]} matrix-kit]
    (cond
;;       (= activate-fn-key :softmax)
;;       (softmax state)
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
