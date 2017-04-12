(ns prism.unit)

(defn sigmoid [x]
  (float (/ 1 (+ 1 (Math/exp (- (float x)))))))

(defn tanh [x]
  (float (Math/tanh x)))

(defn softmax [y-list]
  (let [n (count y-list)
        ret (float-array n)
        m (float (apply max y-list))
        sum-y (areduce ^floats y-list i acc (float 0) (+ acc (float (Math/exp (- (aget ^floats y-list i) m)))))
        _ (dotimes [x n] (aset ^floats ret x (float (/ (Math/exp (- (aget ^floats y-list x) m)) sum-y))))]
    ret))

(defn activation [state activate-fn-key]
  (if (= activate-fn-key :softmax)
    (softmax state)
    (let [n (count state)
          ret (float-array n)
          func (condp = activate-fn-key
                 :sigmoid sigmoid
                 :linear  identity
                 :tanh    tanh)]
      (dotimes [x n] (aset ^floats ret x (func (aget ^floats state x))))
      ret)))

(defn derivative [state activate-fn-key]
  (let [n (count state)
        ret (float-array n)
        func (condp = activate-fn-key
               :sigmoid #(float (* (sigmoid %) (- 1 (sigmoid %))))
               :tanh    #(float (- 1 (* (tanh %) (tanh %))))
               :linear  (fn [_] (float 1)))
        _ (dotimes [x n] (aset ^floats ret x (func (aget ^floats state x))))]
    ret))

(defn model-rand []
  (float (/ (- (rand 16) 8) 1000)))

(defn random-array [n]
  (let [it (float-array n)]
    (dotimes [x n] (aset ^floats it x (model-rand)))
    it))

(defn binary-classification-error
  [activation positives negatives]
  (let [negatives (remove (fn [n] (some #(= % n) positives)) negatives)
        ps (map (fn [p] [p (float (- 1 (get activation p)))]) positives)
        ns (map (fn [n] [n (float (- (get activation n)))]) negatives)]
    (vec (concat ps ns))))

(defn prediction-error
  [activation expectation]
  (when-not (= :skip expectation)
    (->> expectation
         (mapv (fn [[item expect-value]]
                 [item (float (- expect-value (get activation item)))])))))
