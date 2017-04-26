(ns matrix.default)

(defn sum
  ([^floats v1]
   v1)
  ([^floats v1 ^floats v2]
   (when-not (= (alength v1) (alength v2)) (throw (Exception. "vectors must be same length")))
   (let [n (alength v1)
         ret (float-array n)
         _ (dotimes [x n] (aset ^floats ret x (float (+ (aget ^floats v1 x) (aget ^floats v2 x)))))]
     ret))
  ([^floats v1 ^floats v2 & more]
   (reduce #(sum %1 %2) (sum v1 v2) more)))

(defn minus
  ([^floats v1 ^floats v2]
   (when-not (= (alength v1) (alength v2)) (throw (Exception. "vectors must be same length")))
   (let [n (alength v1)
         ret (float-array n)
         _ (dotimes [x n] (aset ^floats ret x (float (- (aget ^floats v1 x) (aget ^floats v2 x)))))]
     ret))
  ([^floats v1 ^floats v2 & more]
   (reduce #(minus %1 %2) (minus v1 v2) more)))

(defn scal
  [^double a ^floats v]
  (float-array (map #(* a %) v)))

(defn times
  ([^floats v1 ^floats v2]
   (when-not (= (alength v1) (alength v2)) (throw (Exception. "vectors must be same length")))
   (let [n (alength v1)
         ret (float-array n)
         _ (dotimes [x n] (aset ^floats ret x (float (* (aget ^floats v1 x) (aget ^floats v2 x)))))]
     ret))
  ([^floats v1 ^floats v2 & more]
   (reduce #(times %1 %2) (times v1 v2) more)))

(defn dot
  [^floats v1 ^floats v2]
  (let [s (times v1 v2)]
    (areduce ^floats s i ret (float 0) (+ ret (aget ^floats s i)))))

(defn outer
  [^floats v1 ^floats v2]
  (let [a (alength v1)
        b (alength v2)
        ret (float-array (* a b))]
    (dotimes [x a]
      (dotimes [y b]
        (aset ^floats ret (+ (* x b) y) (float (* (aget ^floats v1 x) (aget ^floats v2 y))))))
    ret))

(defn transpose
  [^Integer row-size ^floats matrix]
  (let [col-size (quot (alength matrix) row-size)
        ret-mat  (float-array (alength matrix))]
    (dotimes [row-index row-size]
      (dotimes [col-index col-size]
        (aset ^floats ret-mat (+ (* row-index col-size) col-index) (aget ^floats matrix (+ (* col-index row-size) row-index)))))
    ret-mat))

(defn gemv
  [^floats matrix ^floats v]
  (let [row-n (alength v)
        col-n (quot (alength matrix) (alength v))
        tmp-v (float-array row-n)
        ret-v (float-array col-n)]
    (dotimes [col-index col-n];for a col
      (dotimes [row-index row-n];for a row
        (aset ^floats tmp-v row-index (float (* (aget ^floats matrix (+ (* col-index row-n) row-index)) (aget ^floats v row-index)))))
      (aset ^floats ret-v col-index (float (areduce tmp-v i ret (float 0) (+ ret (aget ^floats tmp-v i))))))
    ret-v))

(defn rewrite-vector!
  [^double alpha ^floats v! ^floats v2]
  (dotimes [x (alength v!)]
    (aset ^floats v! x (float (+ (aget ^floats v! x) (* alpha (aget ^floats v2 x)))))))

(defn sigmoid
  [^double x]
  (float (/ 1 (+ 1 (Math/exp (-  x))))))

(defn tanh
  [^double x]
  (float (Math/tanh x)))


(defn alter-vec
  "f should take unboxed value and return unboxed value to work faster"
  [^floats v f]
  (let [tmp (aclone v)]
    (dotimes [i (alength tmp)]
      (aset ^floats tmp i (f (aget ^floats v i))))
    tmp))

(defn model-rand [] (float (/ (- (rand 16) 8) 1000)))

(defn random-array [^Integer n]
  (let [it (float-array n)]
    (dotimes [x n] (aset ^floats it x (model-rand)))
    it))

(def default-matrix-kit
  {:type :default
   :sum sum
   :minus minus
   :times times
   :scal scal
   :dot dot
   :outer outer
   :transpose transpose
   :gemv gemv
   :init-vector random-array
   :init-matrix random-array
   :make-vector float-array
   :rewrite-vector! rewrite-vector!
   :sigmoid sigmoid
   :sigmoid-derivative (fn [x] (let [s (sigmoid x)] (float (* s (- 1 s)))))
   :tanh tanh
   :tanh-derivative (fn [x] (let [it (Math/tanh x)] (float (- 1 (* it it)))))
   :alter-vec alter-vec})
