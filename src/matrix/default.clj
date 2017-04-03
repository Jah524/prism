(ns matrix.default)

(defn sum
  ([v1]
   v1)
  ([v1 v2]
   (when-not (= (count v1) (count v2)) (throw (Exception. "vectors must be same length")))
   (let [n (count v1)
         ret (float-array n)
         _ (dotimes [x n] (aset ^floats ret x (float (+ (aget ^floats v1 x) (aget ^floats v2 x)))))]
     ret))
  ([v1 v2 & more]
   (reduce #(sum %1 %2) (sum v1 v2) more)))

(defn minus
  ([v1 v2]
   (when-not (= (count v1) (count v2)) (throw (Exception. "vectors must be same length")))
   (let [n (count v1)
         ret (float-array n)
         _ (dotimes [x n] (aset ^floats ret x (float (- (aget ^floats v1 x) (aget ^floats v2 x)))))]
     ret))
  ([v1 v2 & more]
   (reduce #(minus %1 %2) (minus v1 v2) more)))

(defn times
  ([v1 v2]
   (when-not (= (count v1) (count v2)) (throw (Exception. "vectors must be same length")))
   (let [n (count v1)
         ret (float-array n)
         _ (dotimes [x n] (aset ^floats ret x (float (* (aget ^floats v1 x) (aget ^floats v2 x)))))]
     ret))
  ([v1 v2 & more]
   (reduce #(times %1 %2) (times v1 v2) more)))

(defn dot [v1 v2]
  (let [s (times v1 v2)]
    (areduce ^floats s i ret (float 0) (+ ret (aget ^floats s i)))))

(defn outer [v1 v2]
  (let [a (count v1)
        b (count v2)
        ret (float-array (* a b))]
    (dotimes [x a]
      (dotimes [y b]
        (aset ^floats ret (+ (* x b) y) (float (* (aget ^floats v1 x) (aget ^floats v2 y))))))
    ret))

(defn transpose
  [row-size matrix]
  (let [col-size (quot (count matrix) row-size)
        ret-mat  (float-array (count matrix))]
    (dotimes [row-index row-size]
      (dotimes [col-index col-size]
        (aset ^floats ret-mat (+ (* row-index col-size) col-index) (aget ^floats matrix (+ (* col-index row-size) row-index)))))
    ret-mat))

(defn gemv
  [matrix v]
  (let [row-n (count v)
        col-n (quot (count matrix) (count v))
        tmp-v (float-array row-n)
        ret-v (float-array col-n)]
    (dotimes [col-index col-n];for a col
      (dotimes [row-index row-n];for a row
        (aset ^floats tmp-v row-index (float (* (aget ^floats matrix (+ (* col-index row-n) row-index)) (aget ^floats v row-index)))))
      (aset ^floats ret-v col-index (float (areduce tmp-v i ret (float 0) (+ ret (aget ^floats tmp-v i))))))
    ret-v))
