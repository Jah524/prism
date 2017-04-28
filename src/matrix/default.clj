(ns matrix.default
  (require [clojure.pprint :refer [pprint]]))

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

(defn merger!
  "takes matrices or vectors and return sum of each element"
  [m m!]
  (cond
    (= (type m) (Class/forName "[Ljava.lang.Object;")); if matrix
    (let [col-n (alength (aget ^objects m 0))]
      (dotimes [x (alength m)]
        (let [v  (aget ^objects m x)
              v! (aget ^objects m! x)]
          (dotimes [y col-n]
            (aset ^floats v! y (float (+ (aget ^floats v y) (aget ^floats v! y)))))))
      m!)
    (= (type m) (Class/forName "[F")); if default vector
    (sum m m!)))


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
  [a ^floats v]
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
        mat (object-array a)]
    (dotimes [x a]
      (let [tmp (float-array b)]
        (dotimes [y b]
          (aset ^floats tmp y (float (* (aget ^floats v1 x) (aget ^floats v2 y)))))
        (aset ^objects mat x tmp)))
    mat))


(defn transpose
  [^objects matrix]
  (let [row-n (alength matrix)
        col-n (alength (aget ^objects matrix 0))
        ret (object-array col-n)]
    (dotimes [y col-n]
      (let [tmp (float-array row-n)]
        (dotimes [x row-n]
          (aset ^floats tmp x (aget ^floats (aget ^objects matrix x) y)))
        (aset ^objects ret y tmp)))
    ret))


(defn gemv
  [^objects matrix ^floats v]
  (let [mn (alength matrix)
        vn (alength v)
        tmp (float-array vn)
        ret (float-array mn)]
    (dotimes [x mn]
      (dotimes [y vn]
        (aset ^floats tmp y (float (* (aget ^floats (aget ^objects matrix x) y) (aget ^floats v y)))))
      (aset ^floats ret x (float (areduce tmp i ret (float 0) (+ ret (aget ^floats tmp i))))))
    ret))

(defn rewrite-vector!
  [alpha ^floats v! ^floats v2]
  (when-not (= (alength v!) (alength v2)) (throw (Exception. "vectors must be same length")))
  (dotimes [x (alength v!)]
    (aset ^floats v! x (float (+ (aget ^floats v! x) (* alpha (aget ^floats v2 x)))))))

(defn rewrite-matrix!
  [alpha ^objects matrix! ^objects m2]
  (dotimes [x (alength matrix!)]
    (rewrite-vector! alpha (aget ^objects matrix! x) (aget ^objects m2 x))))

(defn sigmoid [x] (float (/ 1 (+ 1 (Math/exp (-  (float x)))))))

(defn tanh [x] (float (Math/tanh (float x))))

(defn alter-vec
  "f should take unboxed value and return unboxed value to work faster"
  [^floats v f]
  (let [tmp (aclone v)]
    (dotimes [i (alength tmp)]
      (aset ^floats tmp i (float (f (aget ^floats v i)))))
    tmp))

(defn model-rand [] (float (/ (- (rand 16) 8) 1000)))

(defn random-array [^Integer n]
  (let [it (float-array n)]
    (dotimes [x n] (aset ^floats it x (model-rand)))
    it))

(def default-matrix-kit
  {:type :default
   :sum sum
   :merger! merger!
   :minus minus
   :times times
   :scal scal
   :dot dot
   :outer outer
   :transpose transpose
   :gemv gemv
   :init-vector random-array
   :init-matrix (fn [input-num hidden-num]
                  (let [mat (object-array hidden-num)]
                    (dotimes [x hidden-num]
                      (aset ^objects mat x (random-array input-num)))
                    mat))
   :make-vector float-array
   :make-matrix (fn [input-num hidden-num v]
                  (let [mat (object-array hidden-num)]
                    (dotimes [x hidden-num]
                      (let [tmp (float-array input-num)]
                        (dotimes [y input-num]
                          (aset ^floats tmp y (float (nth v y))))
                        (aset ^objects mat tmp)))
                    mat))
   :rewrite-vector! rewrite-vector!
   :rewrite-matrix! rewrite-matrix!
   :sigmoid sigmoid
   :sigmoid-derivative (fn [x] (let [s (sigmoid x)] (float (* s (- 1 s)))))
   :tanh tanh
   :tanh-derivative (fn [x] (let [it (Math/tanh x)] (float (- 1 (* it it)))))
   :linear-derivative-vector (fn [v] (float-array (take (alength v) (repeat 1))))
   :alter-vec alter-vec})
