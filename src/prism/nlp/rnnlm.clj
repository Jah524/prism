(ns prism.nlp.rnnlm
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [clojure.java.io :refer [reader]]
    [clojure.core.async :refer [go]]
    [clj-time.local :as l]
    [clojure.core.matrix :refer [array]]
    [prism.nn.rnn :as rnn]
    [prism.util :as util]
    [prism.sampling :refer [uniform->cum-uniform samples]]))

(defn convert-rare-word-to-unk
  [wc word]
  (if (get wc word) word "<unk>"))

(defn tok->rnnlm-pairs
  [wc tok-line]
  (let [words (->> (str/split tok-line #" ")
                   (remove #(or (re-find #" |　" %) (= "" %)))
                   (map #(convert-rare-word-to-unk wc %)))]
    (if (empty? words)
      :skip
      (loop [coll words,
             x-acc []
             y-acc []]
        (if-let [f (first coll)]
          (let [s (or (second coll) "<eos>")]
            (recur (rest coll)
                   (conj x-acc (set [f]))
                   (conj y-acc {:pos (set [s])})))
          {:x x-acc :y y-acc})))))

(defn add-negatives
  [rnnlm-pair negative negatives]
  (let [{:keys [y]} rnnlm-pair]
    (when (not= (* (count y) negative) (count negatives))
      (throw (Exception. "Invalid negative count")))
    (assoc rnnlm-pair
      :y
      (->> y
           (map-indexed
             (fn [i train]
               (assoc train :neg (->> negatives (drop (* i negative)) (take negative) set))))))))


(defn train-rnnlm!
  [model train-path & [option]]
  (let [{:keys [interval-ms workers negative initial-learning-rate min-learning-rate
                skip-lines snapshot model-path]
         :or {interval-ms 60000 ;; 1 minutes
              workers 4
              negative 5
              initial-learning-rate 0.005
              min-learning-rate 0.001
              skip-lines 0
              snapshot 60 ;  1 hour when interval-ms is set 60000
              }} option
        all-lines-num (with-open [r (reader train-path)] (count (line-seq r)))
        {:keys [wc em]} model
        _(println(str  "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] making distribution for negative sampling ..."))
        wc-unif (reduce #(assoc %1 (first %2) (float (Math/pow (second %2) (/ 3 4)))) {} (dissoc wc "<unk>"))
        neg-cum (uniform->cum-uniform wc-unif)
        tmp-loss (atom 0)
        cache-size 100000
        interval-counter (atom 0)
        progress-counter (atom 0)
        snapshot-num (atom 1)
        done? (atom false)]
    (with-open [r (reader train-path)]
      (print (str "skipping " skip-lines " lines ..."))
      (loop [skip skip-lines]
        (when (> skip 0)
          (.readLine r)
          (swap! progress-counter inc)
          (recur (dec skip))))
      (println "done")
      (dotimes [w workers]
        (go (loop [negatives (samples neg-cum (* negative cache-size))]
              (if-let [line (.readLine r)]
                (let [progress (/ @progress-counter all-lines-num)
                      learning-rate (max (- initial-learning-rate (* initial-learning-rate progress)) min-learning-rate)
                      rnnlm-pair (tok->rnnlm-pairs wc line)]
                  (swap! interval-counter inc)
                  (swap! progress-counter inc)
                  (if (= :skip rnnlm-pair)
                    (recur negatives)
                    (let [neg-pool-num (* negative 10); (count (:x rnnlm-pair)))
                          neg-pool    (take neg-pool-num negatives)
                          rest-negatives (drop neg-pool-num negatives)
                          {:keys [x y]} (add-negatives rnnlm-pair negative (shuffle (take (* (count (:x rnnlm-pair)) negative)
                                                                                          (cycle neg-pool))))]
                      (try
                        (let [forward (rnn/forward model x (map #(apply clojure.set/union (vals %)) y))
                              {:keys [param-loss loss]} (rnn/bptt model forward y)
                              loss-seq (->> loss
                                            (map #(/ (->> % ; by 1 target and some negatives
                                                          (map (fn [[_ v]] (Math/abs v)))
                                                          (reduce +))
                                                     (inc negative))))];; loss per output-item
                          (swap! tmp-loss #(+ %1 (/ (reduce + loss-seq) (count loss-seq))));; loss per word in line
                          (rnn/update-model! model param-loss learning-rate))
                        (catch Exception e
                          (do
                            ;; debug purpose
                            (clojure.stacktrace/print-stack-trace e)
                            (println line)
                            (pprint x)
                            (pprint y)
                            (Thread/sleep 60000))))
                      (recur (if (< (count rest-negatives) (* 10 negative))
                               (samples neg-cum (* negative cache-size))
                               rest-negatives)))))
                (reset! done? true)))))
      (loop [loop-counter 0]
        (when-not @done?
          (println (str (util/progress-format @progress-counter all-lines-num @interval-counter interval-ms "lines/s") ", loss: " (float (/ @tmp-loss (inc @interval-counter))))); loss per 1 word, and avoiding zero divide
          (reset! tmp-loss 0)
          (reset! interval-counter 0)
          (when (and model-path (not (zero? snapshot)) (not (zero? loop-counter)) (zero? (rem loop-counter snapshot)))
            (let [spath (str model-path "-SNAPSHOT-" @snapshot-num)]
              (println (str "saving " spath))
              (util/save-model model spath)
              (swap! snapshot-num inc)))
          (Thread/sleep interval-ms)
          (recur (inc loop-counter))))
      (println "finished learning")))
  model)


(defn init-rnnlm-model
  [wc hidden-size rnn-type]
  (let [wc-set (conj (set (keys wc)) "<eos>")]
    (-> (rnn/init-model {:input-items wc-set
                         :input-size nil
                         :hidden-size hidden-size
                         :output-type :binary-classification
                         :output-items wc-set
                         :activation :linear
                         :rnn-type rnn-type})
        (assoc :wc wc))))

(defn make-rnnlm
  [training-path export-path hidden-size option]
  (let [_(println "making word list...")
        wc (util/make-wc training-path option)
        _(println "done")
        model (init-rnnlm-model wc hidden-size)
        model-path export-path]
    (train-rnnlm! model training-path (assoc option :model-path model-path))
    (print (str "Saving RNNLM model as " model-path " ... "))
    (util/save-model model model-path)
    (println "Done")
    model))

(defn resume-train
  [training-path model-path option]
  (print "loading model ...")
  (let [model (util/load-model model-path)
        _(println " done")
        model (train-rnnlm! model training-path (assoc option :model-path model-path))]
    (println (str "Saving RNNLM model as " model-path))
    (util/save-model model model-path)
    model))

(defn text-vector [model words]
  (let [{:keys [hidden hidden-size wc]} model]
    (loop [words words,
           previous-activation (array :vectorz (repeat hidden-size 0)),
           previous-cell-state (array :vectorz (repeat hidden-size 0))]
      (if-let [word (first words)]
        (let [word (if (get wc word) word "<unk>")
              {:keys [activation state]} (rnn/hidden-fixed-time model (set [word]) previous-activation previous-cell-state)]
          (recur (rest words)
                 activation
                 (:cell-state state)))
        previous-activation))))

(defn text-similarity
  [model words1 words2 l2?]
  (util/similarity (text-vector model words1) (text-vector model words2) l2?))

(defn word-prob-given-context
  "context is a list of word"
  [model context words]
  (let [wc (:wc model)
        n (dec (count context))
        context-unk (->> context (keep #(if (get wc %) nil %)))
        target-unk (->> words (keep #(if (get wc %) nil %)))
        target-words (->> words (filter #(get wc %)))
        x-seq (->> context (map #(set [(if (get wc %) % "<unk>")])))]
    {:context-unk context-unk
     :target-unk target-unk
     :context (mapv first x-seq)
     :items (-> (rnn/forward model x-seq (conj (vec (repeat n :skip)) target-words))
                last
                :activation
                :output)}))

(defn sentence-prob-given-context
  "sentence is a sequence of words"
  [model context sentence]
  (loop [context (vec context),
         sentence sentence,
         acc []]
    (if-let [f (first sentence)]
      (recur
        (conj (vec context) f)
        (rest sentence)
        (cons (word-prob-given-context model context [f]) acc))
      {:history acc
       :log-prob (->> acc
                      (keep #(when-let [it (-> % :items vals first)]
                               (Math/log it)))
                      (reduce +))})))
