(ns sai-ai.nn.sparse-output-feedforward
  (:require [clojure.string :refer [split]]
            [clojure.java.io :refer [reader writer]]
            [clojure.core.async :refer [go go-loop]]
            [clj-time.local :as l]
            [clj-time.core  :as t]
            [clojure.data.json :as json]
            [matrix.default :as default]
            [sai-ai.util :refer [l2-normalize similarity] :as util]
            [sai-ai.unit :refer [activation model-rand]]
            [sai-ai.negative-sampling :refer [uniform->cum-uniform uniform-sampling get-negatives]]))


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
        embedding-delta [target-word h-unit-delta]] ;param's delta always equals unit delta, its input value have to be 1
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


(defn train! [model sg learning-rate & [option]]
  (let [[word-w-delta-list word-bias-delta-list embedding-delta embedding-bias-delta]
        (back-propagation:negative-sampling model sg)]
    (update-output-params! model word-w-delta-list word-bias-delta-list learning-rate)
    (update-embedding-bias! model embedding-bias-delta learning-rate)
    (update-embedding! model embedding-delta learning-rate)))

(defn init-model
  [wl output-type hidden-size]
  (println "Initializing word2vec model ... ")
  (let [wl (assoc wl "<unk>" 0)
        vocab-size (count wl)
        _(println (str "making [ " vocab-size " x " hidden-size " ] matrix ..."))
        w (float-array hidden-size)
        _ (dotimes [x hidden-size] (aset ^floats w x (float (model-rand))))
        m {:embedding  {:w    (reduce #(assoc %1 %2 (float-array (take hidden-size (repeatedly model-rand)))) {} (keys wl))
                        :bias (float-array (take hidden-size (aclone w)))}
           :output {:w    (reduce #(assoc %1 %2 (float-array (take hidden-size (repeatedly model-rand)))) {} output-type)
                    :bias (reduce #(assoc %1 %2 (float-array [(model-rand)])) {} output-type)}
           :hidden-size hidden-size
           :all-word-token (reduce + (vals wl))
           :vocab-size vocab-size
           :wl wl}]
    (println "done")
    m))
