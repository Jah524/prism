(ns prism.nlp.word2vec
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.string :refer [split]]
    [clojure.java.io :refer [reader writer]]
    [clojure.core.async :refer [go go-loop]]
    [clj-time.local :as l]
    [clj-time.core  :as t]
    [clojure.data.json :as json]
    [matrix.default :as default]
    [prism.util :refer [l2-normalize l2-normalize! similarity] :as util]
    [prism.unit :refer [activation model-rand]]
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
  [w2v-model train-path & [option]]
  (let [{:keys [interval-ms workers negative initial-learning-rate min-learning-rate]
         :or {interval-ms 60000　; 60 seconds
              workers 4
              negative 5
              initial-learning-rate 0.025
              min-learning-rate　0.0001}} option
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
        (go (loop [negatives (samples neg-cum (* negative 100000))]
              (if-let [train-line (.readLine r)]
                (let [progress (/ @local-counter all-lines-num)
                      learning-rate (max (- initial-learning-rate (* initial-learning-rate progress)) min-learning-rate)
                      sg (skip-gram-training-pair wl all-word-token (split train-line #" ") option)
                      next-negatives (drop (* negative (count sg)) negatives)]
                  (dorun (map-indexed (fn [i [target context]]
                                        (let [positive-items (set context)
                                              negative-items (->> negatives (drop (* negative i)) (take negative) set)
                                              all-items (clojure.set/union positive-items negative-items)]
                                          (try
                                            (let [forward (ff/network-output w2v-model (set [target]) all-items option)
                                                  param-delta (ff/back-propagation w2v-model forward {:pos positive-items :neg negative-items} option)]
                                              (ff/update-model! w2v-model param-delta learning-rate))
                                            (catch Exception e
                                              (do
                                                ;; debug purpose
                                                (clojure.stacktrace/print-stack-trace e)
                                                (println train-line)
                                                (pprint target)
                                                (pprint all-items)
                                                (Thread/sleep 60000))))))
                                      sg))
                  (swap! local-counter inc)
                  (recur (if (empty? next-negatives)
                           (samples neg-cum (* negative 100000))
                           next-negatives)))
                (reset! done? true)))))
      (loop [counter 0]
        (when-not @done?
          (let [c @local-counter
                next-counter (+ counter c)]
            (println (util/progress-format counter all-lines-num c interval-ms "lines/s"))
            (reset! local-counter 0)
            (Thread/sleep interval-ms)
            (recur next-counter))))
      (println "done")
      (.close r))
    :done))

(defn init-w2v-model
  [wl hidden-size]
  (let [wl-set (set (keys wl))]
    (-> (ff/init-model {:input-type :sparse
                        :input-items wl-set
                        :input-size nil
                        :hidden-size hidden-size
                        :output-type :binary-classification
                        :output-items wl-set
                        :activation :linear})
        (assoc :wl wl))))

(defn save-embedding
  "top-n = 0 represents all words"
  ([model path] (save-embedding model path false 0))
  ([model path replace? top-n]
   (let [{:keys [hidden wl]} model
         word-em (:w hidden)
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

(defn make-word2vec
  [training-path export-path hidden-size & [option]]
  (let [_(println "making word list...")
        wl (util/make-wl training-path option)
        _(println "done")
        model (init-w2v-model wl hidden-size)
        model-path     (str export-path ".w2v")
        embedding-path (str export-path "w2v.em")]
    (train-word2vec! model training-path option)
    (println (str "Saving word2vec model as " model-path))
    (util/save-model model model-path)
    (println (str "Saving embedding as " embedding-path))
    (save-embedding model embedding-path)
    (println "Done")
    model))


;; work on embedding ;;

(defn word2vec [embedding word]
  (get embedding word))

(defn most-sim
  [em reference-word word-list & [n l2?]]
  (let [reference-em (word2vec em reference-word)
        targets (->> word-list
                     (map (fn [w]
                            {:word w
                             :similarity (similarity reference-em (word2vec em w) l2?)}))
                     (sort-by :similarity >))]
    (->> (if (= reference-word (:word (first targets)))
           (rest targets)
           targets)
         (take (or n 5)))))

(defn most-sim-in-model-words
  [model word-or-vec & [n limit]]
  (let [{:keys [wl hidden]} model
        {em :w} hidden
        limit (if (nil? limit) (count wl) limit)
        target-word-list (->> wl (sort-by second >) (map first) (take limit))]; sort by frequency
    (most-sim em word-or-vec target-word-list n false)))

