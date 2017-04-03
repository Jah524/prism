(ns sai-ai.nlp.word2vec
  (:require [clojure.string :refer [split]]
            [clojure.java.io :refer [reader writer]]
            [clojure.core.async :refer [go go-loop]]
            [clj-time.local :as l]
            [clj-time.core  :as t]
            [clojure.data.json :as json]
            [matrix.default :as default]
            [sai-ai.util :as util]
            [unit :refer [activation model-rand]]
            ))


(defn subsampling [word freq t]
  (let [take-prob (min 1.0 (Math/sqrt (/ t freq )))]
    (when (< (rand) take-prob)
      word)))

(defn window [coll offset local-window-size]
  (let [n (inc (* 2 local-window-size))
        ret (object-array n)]
    (dotimes [x n] (aset ^objects ret x (aget ^objects coll (+ offset x))))
    ret))

(defn sg-windows [word-list window-size]
  (let [coll (into-array String (concat (repeat window-size "<bos>") word-list (repeat window-size "<eos>")))]
    (loop [word-list word-list,
           i (int 0),
           acc []]
      (if-let [word (first word-list)]
        (let [local-window-size (inc (rand-int window-size))
              offset (+ i (- window-size local-window-size))]
          (recur (rest word-list)
                 (inc i)
                 (conj acc (window coll offset local-window-size))))
        acc))))


(defn skip-gram-training-pair
  [wl all-word-token words & [option]]
  (let [sample (or (:sample option) 1.0e-3)
        window-size (or (:window-size option) 5)
        words (->> words
                   (remove #(= % ""))
                   (keep #(when-let [target-freq (get wl %)]
                            (subsampling % (/ target-freq all-word-token) sample)))
                   (into-array String))
        windows (sg-windows words window-size)]
    (->> windows
         (keep #(let [local-window-size (quot (dec (count %)) 2)
                      target (aget ^objects % local-window-size)
                      target-freq (get wl target)]
                  (when (not (nil? target-freq))
                    ;;                               (subsampling target (/ target-freq all-word-token) sample))
                    (let [context (object-array (* 2 local-window-size))
                          _ (dotimes [x local-window-size]
                              (aset ^objects context x (aget ^objects % x))
                              (aset ^objects context (+ local-window-size x) (aget ^objects % (+ 1 local-window-size x))))
                          context (->> context
                                       (remove (fn [c] (or (= "<bos>" c) (= "<eos>" c) (= "<unk>" c))))
                                       vec)]
                      [target context]))))
         (remove #(empty? (second %)))
         (remove empty?)
         vec)))

(defn uniform->cum-uniform [uniform-dist]
  (->> (sort-by second > uniform-dist)
       (reduce #(let [[acc-dist acc] %1
                      [word v] %2
                      n (float (+ acc v))]
                  [(assoc acc-dist word n) n])
               [{} 0])
       first
       (sort-by second <)
       into-array))


(defn uniform-sampling [cum-dist rnd-list]
  (loop [c (long 0), rnd-list (sort < rnd-list), acc []]
    (let [[word v] (try
                     (aget ^objects cum-dist c)
                     (catch ArrayIndexOutOfBoundsException e ;; due to float range
                       (println "c=>" c "rnd-list=>" rnd-list "cum-dist size=>" (count cum-dist) "max-cum-dist" (second (last cum-dist)))
                       [:error nil]))]
      (cond (= word :error)
            acc
            (empty? rnd-list)
            acc
            (< (first rnd-list) v)
            (recur c (rest rnd-list) (conj acc word))
            :else
            (recur (inc c) rnd-list acc)))))

(defn get-negatives [neg-cum negative-num]
  (let [m (second (last neg-cum))]
    (uniform-sampling neg-cum (repeatedly negative-num #(rand (dec m))))))

(defn init-w2v-model
  [wl hidden-size]
  (println "Initializing word2vec model ... ")
  (let [wl (assoc wl "<unk>" 0)
        vocab-size (count wl)
        _(println (str "making [ " vocab-size " x " hidden-size " ] matrix ..."))
        w (float-array hidden-size)
        _ (dotimes [x hidden-size] (aset ^floats w x (float (model-rand))))
        m {:embedding  {:w    (reduce #(assoc %1 %2 (float-array (take hidden-size (repeatedly model-rand)))) {} (keys wl))
                        :bias (float-array (take hidden-size (aclone w)))}
           :output {:w    (reduce #(assoc %1 %2 (float-array (take hidden-size (repeatedly model-rand)))) {} (keys wl))
                    :bias (reduce #(assoc %1 %2 (float-array [(model-rand)])) {} (keys wl))}
           :hidden-size hidden-size
           :all-word-token (reduce + (vals wl))
           :vocab-size vocab-size
           :wl wl}]
    (println "done")
    m))

(defn hidden-activation [model word]
  (let [hidden-size (:hidden-size model)
        ret (float-array hidden-size)
        w (get (:w (:embedding model)) word)
        bias (:bias (:embedding model))]
    (dotimes [x hidden-size]
      (aset ^floats ret x (+ (aget ^floats w x) (aget ^floats bias x))))
    ret))


(defn negative-sampling
  [model hidden-activation positives negatives & [option]]
  (let [{:keys [hidden-size vocab-size]} model
        negatives (remove (fn [n] (some #(= % n) positives)) negatives)
        words (concat positives negatives)
        output-w  (->> words (mapv #(get (:w (:output model)) %)))
        word-bias (->> words (map #(get (:bias (:output model)) %)) (map #(aget ^floats % 0)) float-array)
        activations  (-> (default/sum (default/gemv' output-w hidden-activation) word-bias)
                         (activation :sigmoid)
                         vec)
        [p n] (split-at (count positives) activations)
        delta (concat (map #(float (- 1 %)) p)
                      (map #(float (- %)) n))]
    (mapv vector words delta)))


(defn some-hot-bp
  "unit-deltas at hidden layer"
  [model word-delta-pairs]
  (let [hidden-size (:hidden-size model)
        output-w (get-in model [:output :w])
        word-w-list (map #(get output-w %) (keys word-delta-pairs))
        mat (float-array (* (count word-delta-pairs) hidden-size))
        _ (doall (map-indexed (fn [i w]
                                (dotimes [x hidden-size]
                                  (aset ^floats mat (+ x (* i hidden-size)) (aget ^floats w x))))
                              word-w-list))
        delta (float-array (vals word-delta-pairs))]
    (default/gemv (default/transpose hidden-size mat) delta)))


(defn output-param-delta [model word-value-pairs hidden-activation]
  (let [hidden-size (:hidden-size model)
        w    (get-in model [:output :w])
        bias (get-in model [:output :bias])
        tmp-delta (float-array hidden-size)
        word-delta (doall (map (fn [ud]
                                 (dotimes [x hidden-size]
                                   (aset ^floats tmp-delta x (float (* ud (aget ^floats hidden-activation x)))))
                                 (aclone tmp-delta))
                               (vals word-value-pairs)))
        bias-delta (vals word-value-pairs)]
    (mapv vector (keys word-value-pairs) word-delta bias-delta)))


(defn back-propagation:negative-sampling [model sg-pair]
  (let [[target-word positives negatives] sg-pair
        h (hidden-activation model target-word)
        word-delta (negative-sampling model h positives negatives)
        output-delta (output-param-delta model word-delta h)
        h-unit-delta (some-hot-bp model word-delta)
        embedding-delta [target-word h-unit-delta]] ;param's delta always equals unit delta, due to its input value have to be 1
    [output-delta word-delta embedding-delta h-unit-delta]))

(defn update-output-params! [model word-w-delta-list word-bias-delta-list learning-rate]
  (let [hidden-size (:hidden-size model)
        output (:output model)
        delta-output (:delta-output model)]
    (mapv
      #(let [word (first %1)
             w    (get (:w output) word)
             bias (get (:bias output) word)
             dw   (get (:w delta-output) word)
             db   (get (:bias delta-output) word)]
         ;;         (when (not= (first %1) (first %2)) (println "doesn't match word"));FIXME
         (dotimes [x hidden-size]
           ;;           (let [[d-acc new-param] (adagrad (aget ^floats w x) (aget ^floats (second %1) x)
           ;;                                            learning-rate (aget ^floats dw x))]
           (let [new-param (+ (aget ^floats w x) (* (aget ^floats (second %1) x) learning-rate))]
             (when (and (< new-param (float 1.0E4)) (> new-param (max (float -1.0E4))))
               ;;               (aset ^floats dw x d-acc)
               (aset ^floats w x new-param))))
         ;;         (let [[d-acc new-param] (adagrad (aget ^floats bias 0) (second %2)
         ;;                                          learning-rate (aget ^floats db 0))]
         (let [new-param (+ (aget ^floats bias 0) (* (second %2) learning-rate))]
           (when (and (< new-param (float 1.0E4)) (> new-param (max (float -1.0E4))))
             ;;             (aset ^floats db 0 d-acc)
             (aset ^floats bias 0 new-param ))))
      word-w-delta-list word-bias-delta-list)
    model))


(defn update-output-params! [model word-w-delta-list word-bias-delta-list learning-rate]
  (let [hidden-size (:hidden-size model)
        output (:output model)
        delta-output (:delta-output model)]
    (mapv
      #(let [word (first %1)
             w    (get (:w output) word)
             bias (get (:bias output) word)
             dw   (get (:w delta-output) word)
             db   (get (:bias delta-output) word)]
         ;; update w
         (dotimes [x hidden-size]
           (let [new-param (+ (aget ^floats w x) (* (aget ^floats (second %1) x) learning-rate))]
             (when (and (< new-param (float 1.0E4)) (> new-param (max (float -1.0E4))))
               (aset ^floats w x new-param))))
         ;; update bias
         (let [new-param (+ (aget ^floats bias 0) (* (second %2) learning-rate))]
           (when (and (< new-param (float 1.0E4)) (> new-param (max (float -1.0E4))))
             (aset ^floats bias 0 new-param ))))
      word-w-delta-list word-bias-delta-list)
    model))

(defn update-embedding-bias! [model embedding-bias-delta learning-rate]
  (let [hidden-size (:hidden-size model)
        model-input-bias (:bias (:embedding model))]
    (dotimes [x hidden-size]
      (let [new-param (+ (aget ^floats model-input-bias x) (* (aget ^floats embedding-bias-delta x) learning-rate))]
        (when (and (< new-param (float 1.0E4)) (> new-param (max (float -1.0E4))))
          (aset ^floats model-input-bias x new-param))))
    model))


(defn update-embedding! [model embedding-delta learning-rate]
  (let [hidden-size (:hidden-size model)
        [target-word delta] embedding-delta
        embedding-set (:w (:embedding model))
        embedding (get embedding-set target-word)]
    (dotimes [x hidden-size]
      (let [new-param (+ (aget ^floats embedding x) (* (aget ^floats delta x) learning-rate))]
        (when (and (< new-param (float 1.0E4)) (> new-param (max (float -1.0E4))))
          (aset ^floats embedding x new-param))))
    model))

;; (update-embedding {:hidden-size 10 :embedding {:w {"A" (float-array (range 10 20))}}} ["A" (float-array (range 10))] 0.01)
;; (mapv #(aget ^floats (get (:w (:embedding (update-embedding {:hidden-size 10 :embedding {:w {"A" (float-array (range 10 20))}}} ["A" (float-array (range 10))] 0.01))) "A") %) (range 10))


(defn train! [model sg learning-rate & [option]]
  (let [[word-w-delta-list word-bias-delta-list embedding-delta embedding-bias-delta]
        (back-propagation:negative-sampling model sg)]
    (update-output-params! model word-w-delta-list word-bias-delta-list learning-rate)
    (update-embedding-bias! model embedding-bias-delta learning-rate)
    (update-embedding! model embedding-delta learning-rate)))


(defn train-word2vec!
  [w2v-model train-path & [option]]
  (let [interval-ms (or (:interval-ms option) 60000) ;; 60 seconds
        workers (or (:workers option) 4)
        negative (or (:negative option 5))
        initial-learning-rate (or (:learning-rate option) 0.025)
        min-learning-rate (or (:min-learning-rate option) 0.0001)
        all-lines-num (with-open [r (reader train-path)] (count (line-seq r)))
        wl (:wl w2v-model)
        neg-wl (dissoc wl "<unk>")
        _(println(str  "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] making distribution for negative sampling ..."))
        wl-unif (reduce #(assoc %1 (first %2) (float (Math/pow (second %2) (/ 3 4)))) {} neg-wl)
        neg-cum (uniform->cum-uniform wl-unif)
        _(println (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] done"))
        all-word-token (reduce #(+ %1 (second %2)) 0 neg-wl)

        local-counter (atom 0)
        done? (atom false)]
    (let [r (reader train-path)]
      (dotimes [w workers]
        (go (loop [negatives (shuffle (get-negatives neg-cum (* negative 100000)))]
              (if-let [train-line (.readLine r)]
                (let [progress (/ @local-counter all-lines-num)
                      learning-rate (max (- initial-learning-rate (* initial-learning-rate progress)) min-learning-rate)
                      sg (skip-gram-training-pair wl all-word-token (split train-line #" ") option)
                      next-negatives (drop (* negative (count sg)) negatives)]
                  (dorun (map-indexed #(train! w2v-model (conj (vec %2) (->> negatives (drop (* negative %1)) (take negative) vec)) learning-rate option) sg))
                  (swap! local-counter inc)
                  (recur (if (empty? next-negatives)  (shuffle (get-negatives neg-cum (* negative 100000))) next-negatives)))
                (reset! done? true)))))
      (loop [counter 0]
        (when-not @done?
          (let [c @local-counter
                next-counter (+ counter c)]
            (println (util/progress-format counter all-lines-num c interval-ms "line/s"))
            (reset! local-counter 0)
            (Thread/sleep interval-ms)
            (recur next-counter))))
      (println "done")
      (.close r))
    :done))

(defn make-word2vec
  [training-path export-path size & [option]]
  (let [_(println "making word list...")
        wl (util/make-wl training-path)
        _(println "done")
        model (init-w2v-model wl size)]
    (train-word2vec! model training-path option)
    (println "Saving model ...")
    (util/save-model model export-path)
    (println "Done")))

(defn l2-normalize
  [^floats v]
  (let [acc (Math/sqrt (reduce + (default/times v v)))
        n (count v)
        ret (float-array n)]
    (amap ^floats v i ret (float (/ (aget ^floats v i) acc)))))

(defn- l2-normalize!
  "caution, this function is destructive but is more memory efficiently"
  [^floats v]
  (let [acc (Math/sqrt (default/sum (default/times v v)))
        n (count v)]
    (dotimes [x n] (aset ^floats v x (float (/ (aget ^floats v x) acc))))))

;; (vec (l2-normalize (float-array (range 1 4))))
;; (map #(/ % (Math/sqrt 14)) [1 2 3])

(defn save-em
  "top-n = 0 represents all words"
  ([model path] (save-em model path false 0))
  ([model path replace? top-n]
   (let [word-em (:w (:embedding model))
         wl (:wl model)
         considered (set (->> (dissoc wl "") (sort-by second >) (map first) (take top-n) (cons "<unk>")))
         word-em (if (or (zero? top-n) (= :all top-n))
                   word-em
                   (reduce (fn [acc [word em]] (if (contains? considered word) (assoc acc word em) acc)) {} word-em))]
     (if replace?
       (do
         (dorun (map #(l2-normalize! (second %)) word-em))
         (util/save-model word-em path))
       (let [l2-em (reduce (fn [acc kv] (assoc acc (first kv) (l2-normalize (second kv)))) {} word-em)]
         (util/save-model l2-em path))))))

;; on embedding ;;

(defn word2vec [embedding word]
  (get embedding word))

(defn similarity
  "cosine similarity between 2 words or vectors
  word vectors are assumed to be l2 normalized vectors"
  ([v1 v2]
   (float (default/dot v1 v2)))
  ([em a b]
   (let [v1 (if (string? a) (word2vec em a) a)
         v2 (if (string? b) (word2vec em b) b)]
     (similarity v1 v2))))

;; (similarity (l2-normalize(float-array [3 2 5])) (l2-normalize (float-array [2 3 5])))

(defn most-sim
  [em word-or-vec target-word-list & [n]]
  (let [targets (->> target-word-list
                     (map (fn [w] {:w w :s (similarity em w word-or-vec)}))
                     (sort-by :s >))]
    (->> (if (= word-or-vec (:w (first targets)))
           (rest targets)
           targets)
         (take (or n 1)))))

(defn most-sim-in-wl
  [em word-or-vec wl & [n limit]]
  (let [limit (if (nil? limit) (count wl) limit)
        target-word-list (->> wl (sort-by second >) (map first) (take limit))]; sort by frequency
    (most-sim em word-or-vec target-word-list n)))
