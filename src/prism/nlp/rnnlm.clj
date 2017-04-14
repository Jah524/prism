(ns prism.nlp.rnnlm
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [clojure.java.io :refer [reader]]
    [clojure.core.async :refer [go]]
    [prism.nn.lstm :as lstm];:refer [lstm-activation init-model train!]]
    [prism.util :as util]
    [prism.sampling :refer [uniform->cum-uniform samples]]))

(defn convert-rare-word-to-unk
  [wl word]
  (if (get wl word) word "<unk>"))

(defn tok->rnnlm-pairs
  [wl tok-line]
  (loop [coll (->> (str/split tok-line #" ")
                   (remove #(or (re-find #" |　" %) (= "" %)))
                   (map #(convert-rare-word-to-unk wl %)))
         x-acc []
         y-acc []]
    (if-let [f (first coll)]
      (let [s (or (second coll) "<eos>")]
        (recur (rest coll)
               (conj x-acc (set [f]))
               (conj y-acc {:pos (set [s])})))
      {:x x-acc :y y-acc})))

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
  (let [interval-ms (or (:interval-ms option) 30000) ;; 30 seconds
        workers (or (:workers option) 4)
        negative (or (:negative option) 5)
        initial-learning-rate (or (:learning-rate option) 0.05)
        min-learning-rate (or (:min-learning-rate option) 0.001)
        all-lines-num (with-open [r (reader train-path)] (count (line-seq r)))
        {:keys [wl em input-type]} model
        wl-unif (reduce #(assoc %1 (first %2) (float (Math/pow (second %2) (/ 3 4)))) {} (dissoc wl "<unk>"))
        neg-cum (uniform->cum-uniform wl-unif)

        local-counter (atom 0)
        done? (atom false)]
    (with-open [r (reader train-path)]
      (dotimes [w workers]
        (go (loop [negatives (samples neg-cum (* negative 100000))]
              (if-let [line (.readLine r)]
                (let [;progress (/ @local-counter all-lines-num)
                       learning-rate initial-learning-rate;(max (- initial-learning-rate (* initial-learning-rate progress)) min-learning-rate)
                       rnnlm-pair (tok->rnnlm-pairs wl line)
                       neg-pool-num (* negative 10); (count (:x rnnlm-pair)))
                       neg-pool    (take neg-pool-num negatives)
                       rest-negatives (drop neg-pool-num negatives)
                       {:keys [x y]} (add-negatives rnnlm-pair negative (shuffle (take (* (count (:x rnnlm-pair)) negative)
                                                                                       (cycle neg-pool))))]
                  (lstm/train! model x y learning-rate option)
                  (swap! local-counter inc)
                  (recur (if (< (count rest-negatives) (* 10 negative))
                           (samples neg-cum (* negative 100000))
                           rest-negatives)))
                (reset! done? true)))))
      (loop [counter 0]
        (when-not @done?
          (let [c @local-counter
                next-counter (+ counter c)]
            (println (util/progress-format counter all-lines-num c interval-ms "lines/s"))
            (reset! local-counter 0)
            (Thread/sleep interval-ms)
            (recur next-counter))))
      (println "done")))
  model)


(defn init-rnnlm-model
  [wl hidden-size]
  (let [wl-set (conj (set (keys wl)) "<eos>")]
    (-> (lstm/init-model {:input-type :sparse
                          :input-items wl-set
                          :input-size nil
                          :hidden-size hidden-size
                          :output-type :binary-classification
                          :output-items wl-set
                          :activation :linear})
        (assoc :wl wl))))

(defn make-rnnlm
  [training-path export-path hidden-size & [option]]
  (let [_(println "making word list...")
        wl (util/make-wl training-path option)
        _(println "done")
        model (init-rnnlm-model wl hidden-size)
        model-path     (str export-path ".rnnlm")
        embedding-path (str export-path ".rnnlm.em")]
    (train-rnnlm! model training-path option)
    (println (str "Saving RNNLM model as " model-path))
    (util/save-model model model-path)
    (println (str "Saving RNNLM embedding model as " embedding-path))
;;     (save-embedding model embedding-path)
    (println "Done")
    model))


