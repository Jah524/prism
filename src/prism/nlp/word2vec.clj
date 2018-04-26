(ns prism.nlp.word2vec
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.string :refer [split]]
    [clojure.java.io :refer [reader writer]]
    [clojure.core.async :refer [go go-loop]]
    [clj-time.local :as l]
    [clj-time.core  :as t]
    [clojure.data.json :as json]
    [clojure.core.matrix :refer [array]]
    [prism.util :refer [l2-normalize similarity] :as util]
    [prism.sampling :refer [uniform->cum-uniform uniform-sampling samples]]
    [prism.nn.feedforward :as ff]))


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
  [wc all-word-token words & [option]]
  (let [sample (or (:sample option) 1.0e-3)
        window-size (or (:window-size option) 5)
        words (->> words
                   (remove #(= % ""))
                   (keep #(when-let [target-freq (get wc %)]
                            (subsampling % (/ target-freq all-word-token) sample)))
                   (into-array String))
        windows (sg-windows words window-size)]
    (->> windows
         (keep #(let [local-window-size (quot (dec (count %)) 2)
                      target (aget ^objects % local-window-size)
                      target-freq (get wc target)]
                  (when (not (nil? target-freq))
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


(defn train-word2vec!
  [w2v-model train-path option]
  (let [{:keys [interval-ms workers negative initial-learning-rate min-learning-rate
                snapshot-interval]
         :or {interval-ms 60000　; 60 seconds
              snapshot-interval 60 ; 1 hour
              workers 4
              negative 5
              initial-learning-rate 0.025
              min-learning-rate　0.0001}} option
        _(print "counting lines ... ")
        all-lines-num (with-open [r (reader train-path)] (count (line-seq r)))
        _(println (str "done, "all-lines-num " lines considered"))
        wc (:wc w2v-model)
        neg-wc (dissoc wc "<unk>")
        _(println(str  "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] making distribution for negative sampling ..."))
        wc-unif (reduce #(assoc %1 (first %2) (float (Math/pow (second %2) (/ 3 4)))) {} neg-wc)
        neg-cum (uniform->cum-uniform wc-unif)
        _(println (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] done"))
        all-word-token (reduce #(+ %1 (second %2)) 0 neg-wc)
        tmp-loss (atom 0)
        interval-counter (atom 0)
        progress-counter (atom 0)
        done? (atom false)]
    (with-open [r (reader train-path)]
      (dotimes [w workers]
        (go (loop [negatives (samples neg-cum (* negative 100000))]
              (if-let [train-line (.readLine r)]
                (let [progress (/ @progress-counter all-lines-num)
                      learning-rate (max (- initial-learning-rate (* initial-learning-rate progress)) min-learning-rate)
                      sg (skip-gram-training-pair wc all-word-token (split train-line #" ") option)
                      next-negatives (drop (* negative (count sg)) negatives)]
                  (dorun (map-indexed (fn [i [target context]]
                                        (let [positive-items (set context)
                                              negative-items (->> negatives (drop (* negative i)) (take negative) set)
                                              all-items (clojure.set/union positive-items negative-items)]
                                          (try
                                            (let [forward (ff/forward w2v-model (set [target]) all-items)
                                                  {:keys [param-loss loss]} (ff/back-propagation w2v-model forward {:pos positive-items :neg negative-items})
                                                  loss-sum (->> loss (map (fn [[_ v]] (Math/abs v))) (reduce +))]
                                              (swap! tmp-loss #(+ %1 (/ loss-sum (count loss))))
                                              (ff/update-model! w2v-model param-loss learning-rate))
                                            (catch Exception e
                                              (do
                                                ;; debug purpose
                                                (clojure.stacktrace/print-stack-trace e)
                                                (println train-line)
                                                (pprint target)
                                                (pprint all-items)
                                                (Thread/sleep 60000))))))
                                      sg))
                  (swap! progress-counter inc)
                  (swap! interval-counter inc)
                  (recur (if (empty? next-negatives)
                           (samples neg-cum (* negative 100000))
                           next-negatives)))
                (reset! done? true)))))
      (loop []
        (when-not @done?
          (println (str (util/progress-format @progress-counter all-lines-num @interval-counter interval-ms "lines/s") ", acc-loss: " (float @tmp-loss)))
          (reset! tmp-loss 0)
          (reset! interval-counter 0)
          (Thread/sleep interval-ms)
          (recur)))
      (println "done"))
    :done))

(defn init-w2v-model
  [wc hidden-size]
  (let [wc-set (set (keys wc))]
    (-> (ff/init-model {:input-items wc-set
                        :input-size nil
                        :hidden-size hidden-size
                        :output-type :binary-classification
                        :output-items wc-set
                        :activation :linear})
        (assoc :wc wc))))


(defn leave-freq-word
  [w2v top-n]
  (let [{:keys [wc]} w2v
        freq-words (->> wc
                        (sort-by val >)
                        (take top-n)
                        (map first)
                        (cons "<unk>"))]
    (reduce (fn [acc word]
              (assoc acc word (-> w2v :hidden :sparses (get word))))
            {}
            freq-words)))

(defn save-embedding
  [w2v path top-n]
  (let [{:keys [hidden wc]} w2v
        rest-word-em (if (= :all top-n)
                       (:sparses hidden)
                       (leave-freq-word w2v top-n))
        l2-em (reduce (fn [acc [k v]] (assoc acc k (l2-normalize v))) {} rest-word-em)]
    (util/save-model l2-em path)
    l2-em))

(defn make-word2vec
  [training-path export-path hidden-size option]
  (let [_(println "making word list...")
        wc (util/make-wc training-path option)
        top-n (or (:top-n option) :all)
        _(println "done")
        model (init-w2v-model wc hidden-size)
        model-path     export-path
        embedding-path (str export-path ".em")]
    (train-word2vec! model training-path option)
    (println (str "Saving word2vec model as " model-path))
    (util/save-model model model-path)
    (println (str "Saving embedding as " embedding-path))
    (save-embedding model embedding-path top-n)
    (println "Done")
    model))

(defn resume-train
  "model-path will get path of your trained model without file extensions"
  [training-path model-path & [option]]
  (let [model (util/load-model model-path)
        model-path     (str model-path ".w2v")
        embedding-path (str model-path "w2v.em")]
    (train-word2vec! model training-path option)
    (println (str "Saving word2vec model as " model-path))
    (util/save-model model model-path)
    (println (str "Saving embedding as " embedding-path))
    (save-embedding model embedding-path)
    (println "Done")
    model))

;; work on embedding ;;

(defn most-sim
  "embeddings have to be l2-normalized
  a word doesn't exist in embeddings are not removed"
  [embedding reference target-list & {:keys [n l2?], :or {n 5 l2? true}}]
  (let [{:keys [em]} embedding]
    (when-let [reference-vec (if (string? reference) (get embedding reference) reference)]
      (let [targets (->> target-list
                         (keep (fn [w]
                                 (when-let [v (get embedding w)]
                                   {:word w
                                    :sim (float (min 1 (similarity reference-vec v l2?)))})))
                         (sort-by :sim >))]
        (->> (if (= reference (:word (first targets)))
               (rest targets)
               targets)
             (take n))))))



(defn most-sim-in-model
  [model word-or-vec n & [limit]]
  (let [{:keys [wc hidden]} model
        {em :sparses} hidden
        limit (or limit (count wc))
        target-word-list (->> wc (sort-by second >) (map first) (take limit))]; sort by frequency
    (most-sim em word-or-vec target-word-list :n n :l2? false)))

