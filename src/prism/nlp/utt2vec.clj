(ns prism.nlp.utt2vec
  (:require
    [clojure.string :as str]
    [clojure.java.io :refer [reader]]
    [clojure.core.async :refer [go]]
    [prism.nn.sparse-output-lstm :refer [lstm-activation init-model train!]]
    [prism.util :refer [load-model save-model make-wl progress-format l2-normalize similarity]]
    [prism.sampling :refer [uniform->cum-uniform samples]]))


(defn train-utt2vec!
  [model train-path & [option]]
  (let [interval-ms (or (:interval-ms option) 30000) ;; 30 seconds
        workers (or (:workers option) 4)
        negative (or (:negative option) 5)
        initial-learning-rate (or (:learning-rate option) 0.025)
        min-learning-rate (or (:min-learning-rate option) 0.0001)
        all-lines-num (with-open [r (reader train-path)] (count (line-seq r)))

        {:keys [wl em input-type]} model
        neg-wl (dissoc wl "<unk>")
        wl-unif (reduce #(assoc %1 (first %2) (float (Math/pow (second %2) (/ 3 4)))) {} wl)
        neg-cum (uniform->cum-uniform wl-unif)

        local-counter (atom 0)
        done-workers (atom 0)]
    (with-open [r (reader train-path)]
      (dotimes [w workers]
        (go (loop [negatives (shuffle (samples neg-cum (* negative 100000)))]
              (if-let [line (.readLine r)]
                (let [progress (/ @local-counter all-lines-num)
                      learning-rate initial-learning-rate;(max (- initial-learning-rate (* initial-learning-rate progress)) min-learning-rate)
                      cnvs (partition 3 1 (concat [:padding] (str/split line #" <eos> ") [:padding]))
                      ;;                       _(println cnvs)
                      next-negatives (drop (* negative (count cnvs)) negatives)]
                  ;    [x-seq positives negatives] (clojure.edn/read-string line)]
                  (->> cnvs
                       (map-indexed (fn [index cnv]
                                      (let [coll  (str/split (second cnv) #" ")
                                            target (vec (if (= :sparse input-type)
                                                          (->> coll (map #(if (get wl %) % "<unk>")))
                                                          (map #(get em % (get em "<unk>")) coll)))
                                            ;;                                             _(println cnv)
                                            ;;                                             _(println (second cnv))
                                            context (->> (concat [(first cnv)] [(last cnv)])
                                                         (remove #(= :padding %))
                                                         (map #(str/split % #" "))
                                                         flatten
                                                         (filter #(get wl %))
                                                         vec)
                                            negs (->> negatives (drop (* index negative)) (take negative) vec)]
                                        ;;                                         (clojure.pprint/pprint target)
                                        ;;                                         (println "context-> " context)
                                        (train! model target context negs learning-rate option)
                                        )))
                       dorun)
                  (swap! local-counter inc)
                  (recur (if (empty? next-negatives)  (shuffle (samples neg-cum (* negative 100000))) next-negatives)))
                (swap! done-workers inc)))))
      (loop [counter 0]
        (if-not (= @done-workers workers)
          (let [_ (Thread/sleep interval-ms)
                c @local-counter
                next-counter (+ counter c)]
            (println (progress-format next-counter all-lines-num c interval-ms "line/s"))
            (reset! local-counter 0)
            (recur next-counter)))))
    :done))

(defn make-model
  "input-type: :sparse or :embedding"
  [em-path em-size train-path export-path input-type size & [option]]
  (let [wl (make-wl train-path option)
        model (init-model wl (when em-path (load-model em-path)) em-size input-type size)]
    (train-utt2vec! model train-path option)
    (println "Saving model ...")
    (save-model model export-path)
    (println "Done")))

(defn resume-train
  [model-path train-path & [n option]]
  (let [model (load-model model-path)
        n (or n 1)]
    (loop [n n] (when-not (<= n 0)
                  (println n)
                  (train-utt2vec! model train-path option)
                  (println "Saving model ...")
                  (save-model model model-path)
                  (recur (dec n))))
    (println "Done")))


;; work on utt2vec ;;


(defn utt2vec [model words & [option]]
  (let [{:keys [hidden-size em input-type wl]} model]
    (loop [words words,
           previous-hidden-output (float-array hidden-size)
           previous-cell-state    (float-array hidden-size)]
      (if-let [word (first words)]
        (let [x-input (if (= input-type :sparse) (if (get wl word) word "<unk>") (get em word (get em "<unk>")))
              {:keys [activation state]} (lstm-activation model x-input previous-hidden-output previous-cell-state option)]
          (recur (rest words)
                 activation
                 (:cell-state state)))
        previous-hidden-output))))


(defn make-utt2vec-list-from-cnvs
  [model-path cnvs-path export-path & [option]]
  (let [model (load-model model-path)
        {:keys [input-type em]} model
        all-lines-num (with-open [r (reader cnvs-path)] (count (line-seq r)))
        workers (or (:workers option) 6)
        interval-ms (or (:interval-ms option) 10000)
        acc (atom {})
        local-counter (atom 0)
        done-workers (atom 0)]
    (let [r (reader cnvs-path)]
      (dotimes [_ workers]
        (go (loop []
              (let [line (.readLine r)]
                (if (nil? line)
                  (swap! done-workers inc)
                  (let [w-v-list (->> (str/split line #" <eos> ")
                                      (map #(let [words (str/split % #" ")
                                                  v (l2-normalize (utt2vec model words))]
                                              [(apply str words) v])))
                        mini-acc (reduce (fn [local-acc [k v]] (assoc local-acc k v)) {} w-v-list)]
                    (swap! acc merge mini-acc)
                    (swap! local-counter inc)
                    (recur)))))))
      (loop [counter 0]
        (if-not (= @done-workers workers)
          (let [_ (Thread/sleep interval-ms)
                c @local-counter
                next-counter (+ counter c)]
            (println (progress-format next-counter all-lines-num c interval-ms "line/s"))
            (reset! local-counter 0)
            (recur next-counter))
          (do
            (.close r)
            (save-model @acc export-path)))))
    :done))


(defn most-sim-from-utt-list
  [model utt-list words & [top-n comp]]
  (let [n (or top-n 5)
        c (or comp >)
        model (if (string? model) (load-model model) model)
        utt-list (if (string? utt-list) (load-model utt-list) utt-list)
        utt-vec (l2-normalize (utt2vec model words))]
    (->> (vec utt-list)
         (map (fn [u]
                {:utt (first u)
                 :similarity (similarity (second u) utt-vec)}))
         (sort-by :similarity c)
         (take n))))

