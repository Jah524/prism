(ns nlp.utt2vec
  (:require
   [clojure.string :as str]
   [clj-time.local :as l]
   [clj-time.core  :as t]
   [clojure.data.json :as json]
   [taoensso.nippy :refer [freeze-to-out! thaw-from-in!]]
   [clojure.core.async :refer [go go-loop thread >! <! >!! <!! chan timeout alt! alts! close!]]
   [clojure.java.io :refer [reader writer]]
   [matrix.default :refer [sum minus times dot outer transpose gemv]]
   [unit :refer [derivative tanh sigmoid softmax model-rand]]))

(defn save-model [obj target-path]
  (with-open [w (clojure.java.io/output-stream target-path)]
    (freeze-to-out! (java.io.DataOutputStream. w) obj)))

(defn load-model [target-path]
  (with-open [w (clojure.java.io/input-stream target-path)]
    (thaw-from-in! (java.io.DataInputStream. w))))


(defn dump-model-as-json [utt2vec-model-path export-path]
  (let [u2v-model (load-model utt2vec-model-path)
        mjson (-> u2v-model (dissoc :wl :output :em) json/write-str)]
    (with-open [w (writer export-path)]
      (.write w mjson))))

(defn dump-light-model [utt2vec-model-path top-n]
  (let [u2v-model (load-model utt2vec-model-path)
        {:keys [wl em]} u2v-model
        considered (set (->> (dissoc wl "") (sort-by second >) (map first) (take top-n) (cons "<unk>")))
        word-em (reduce (fn [acc [word em]] (if (contains? considered word) (assoc acc word em) acc)) {} em)]
    (-> u2v-model
        (dissoc :wl :output)
        (assoc :em word-em)
        (save-model (str utt2vec-model-path ".top" top-n ".light")))))


(defn- wl-progress-format [done all interval-done interval-ms unit]
  (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] "
       done "/" all ", "
       (if (zero? interval-ms)
         "0.0"
         (format "%.1f" (float (/ interval-done (/ interval-ms 1000)))))
       " " unit " "
       (format "(%.2f" (float (* 100 (/ done all))))
       "%)"))

;; (progress-format 4 10 2 10000 "lines/s")

(defn make-wl
  [input-file & [option]]; {:keys [min-count wl workers wait-ms step] :or {min-count 5 wl {} workers 4 wait-ms 30000 step 1000000}}]
  (let [min-count (or (:min-count option) 5)
        wl        (or (:wl option) {})
        workers   (or (:workers option) 4)
        wait-ms   (or (:wait-ms  option) 30000)
        step      (or (:step option) 1000000)
        all-lines (with-open [r (reader input-file)]
                    (count (line-seq r)))
        over-all     (atom 0)
        done-lines   (atom 0)
        done-workers (atom 0)
        all-wl (atom {})]
    (with-open [r (reader input-file)]
      (dotimes [w workers]
        (go-loop [local-wl {} local-counnter step]
                 (if-let [line (.readLine r)]
                   (let [word-freq (frequencies (remove #(or (= "<eos>" %) (= "" %) (= " " %) (= "　" %)) (str/split line #" ")))
                         updated-wl (merge-with + local-wl word-freq)]
                     (swap! done-lines inc)
                     (if (zero? local-counnter)
                       (do
                         (reset! all-wl (merge-with + @all-wl updated-wl))
                         (recur {} step))
                       (recur updated-wl (dec step))))
                   (do
                     (reset! all-wl (merge-with + @all-wl local-wl))
                     (swap! done-workers inc)))))
      (loop [c 0]
        (if-not (= @done-workers workers)
          (let [diff @done-lines
                updated-c (+ c diff)
                _ (reset! done-lines 0)]
            (println (wl-progress-format updated-c all-lines diff wait-ms "lines/s"))
            (Thread/sleep wait-ms)
            (recur updated-c))
          (reduce (fn [acc [k v]] (if (>= v min-count)
                                    (assoc acc k (+ v (get acc k 0)))
                                    (assoc acc "<unk>" (+ v (get acc "<unk>" 0)))))
                  wl
                  @all-wl))))))

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

(defn get-negatives [neg-cum-dist negative-num]
  (let [m (second (last neg-cum-dist))]
    (uniform-sampling neg-cum-dist (repeatedly negative-num #(rand (dec m))))))


;; LSTM

(defn output-activation [model input-list positives negatives & [lstm-option]]
  (let [model-layer (:output model)
        {:keys [w bias]} model-layer]
    (reduce (fn [acc word]
              (assoc acc word (sigmoid (+ (reduce + (times (get w word) input-list)) (aget ^floats (get bias word) 0)))))
            {}
            (concat positives negatives))))

(defn lstm-activation [model x-input recurrent-input-list previous-cell-state & [lstm-option]]
  (let [gemv (if-let [it (:gemv lstm-option)] it gemv)
        lstm-layer (:utt-embedding model)
        {:keys [block-wr block-bias input-gate-wr input-gate-bias input-gate-peephole
                forget-gate-wr forget-gate-bias forget-gate-peephole
                output-gate-wr output-gate-bias output-gate-peephole peephole unit-num sparse?
                sparses]} lstm-layer
        [block' input-gate' forget-gate' output-gate']
        (if sparse?
          (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses x-input)]
            [block-w input-gate-w forget-gate-w output-gate-w])
          (let [{:keys [block-w input-gate-w forget-gate-w output-gate-w]} lstm-layer
                lstm-mat [block-w input-gate-w forget-gate-w output-gate-w]]
            ;;             (clojure.pprint/pprint x-input)
            (mapv #(gemv % x-input) lstm-mat)))
        lstm-mat-r  [block-wr input-gate-wr forget-gate-wr output-gate-wr]
        [block-r' input-gate-r' forget-gate-r' output-gate-r'] (mapv #(gemv % recurrent-input-list) lstm-mat-r)
        ;;         _(clojure.pprint/pprint block')
        block       (sum block' block-r' block-bias)
        input-gate  (sum input-gate' input-gate-r' input-gate-bias    (times input-gate-peephole  previous-cell-state))
        forget-gate (sum forget-gate' forget-gate-r' forget-gate-bias (times forget-gate-peephole previous-cell-state))
        output-gate (sum output-gate' output-gate-r' output-gate-bias (times output-gate-peephole previous-cell-state))
        cell-state  (float-array unit-num)
        _ (dotimes [x unit-num]
            (aset ^floats cell-state x
                  (float (+ (* (tanh (aget ^floats block x)) (sigmoid (aget ^floats input-gate x)))
                            (* (sigmoid (aget ^floats forget-gate x)) (aget ^floats previous-cell-state x))))))
        lstm (float-array unit-num)
        _ (dotimes [x unit-num]
            (aset ^floats lstm x (float (* (sigmoid (aget ^floats output-gate x)) (tanh (aget ^floats cell-state x))))))]
    {:activation lstm
     :state {:lstm lstm :block block :input-gate input-gate :forget-gate forget-gate :output-gate output-gate :cell-state cell-state}}))

(defn lstm-model-output
  [model x-input positives negatives previous-hidden-output previous-cell-state & [lstm-option]]
  (let [{:keys [activation state]} (lstm-activation model x-input previous-hidden-output previous-cell-state lstm-option)
        output (output-activation model activation positives negatives lstm-option)]
    {:activation [x-input activation output]
     :state  [x-input state nil]}))


(defn sequential-output [model x-seq positives negatives & [lstm-option]]
  (let [hidden-size (:hidden-size model)]
    (loop [x-seq x-seq,
           previous-hidden-output (float-array hidden-size)
           previous-cell-state    (float-array hidden-size)
           acc []]
      (if-let [x-list (first x-seq)]
        (let [model-output (lstm-model-output model x-list positives negatives previous-hidden-output previous-cell-state lstm-option)
              {:keys [activation state]} model-output]
          (recur (rest x-seq)
                 (second activation)
                 (:cell-state (second state))
                 (cons model-output acc)))
        (vec (reverse acc))))))


;;;;    Back Propagation Through Time    ;;;;

(defn negative-sampling
  [hidden-size model-output-seq positives negatives & [option]]
  (let [;{:keys [hidden-num]} model
        negatives (remove (fn [n] (some #(= % n) positives)) negatives)
        ;;         output-seq (sequential-output model x-seq positives negatives option)
        output (last (:activation (last model-output-seq)))
        ps (map (fn [p] [p (float (- 1 (get output p)))]) positives)
        ns (map (fn [n] [n (float (- (get output n)))]) negatives)]
    (vec (concat ps ns))))


(defn lstm-part-delta
  "propagation through a unit"
  [hidden-size propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
   peephole-w-input-gate peephole-w-forget-gate peephole-w-output-gate]
  (let [output-gate-delta (float-array hidden-size)
        _dog (derivative (:output-gate lstm-state) :sigmoid)
        _cell-state (:cell-state lstm-state)
        _ (dotimes [x hidden-size] (aset ^floats output-gate-delta x
                                         (float (* (aget ^floats _dog x) (tanh (aget ^floats _cell-state x)) (aget ^floats propagated-delta x)))))
        cell-state-delta (float-array hidden-size)
        _og (:output-gate lstm-state)
        _dcs (derivative (:cell-state lstm-state) :tanh)
        _fg:t+1 (:forget-gate lstm-state:t+1)
        _csd:t+1 (:cell-state-delta self-delta:t+1)
        _igd:t+1 (:input-gate-delta self-delta:t+1)
        _fgd:t+1 (:forget-gate-delta self-delta:t+1)
        _ (dotimes [x hidden-size] (aset ^floats cell-state-delta x
                                         (float (+ (* (sigmoid (aget ^floats _og x)) (aget ^floats _dcs x) (aget ^floats propagated-delta x))
                                                   (* (sigmoid (aget ^floats _fg:t+1 x)) (aget ^floats _csd:t+1 x))
                                                   (* (aget ^floats peephole-w-input-gate  x) (aget ^floats _igd:t+1 x))
                                                   (* (aget ^floats peephole-w-forget-gate x) (aget ^floats _fgd:t+1 x))
                                                   (* (aget ^floats peephole-w-output-gate x) (aget ^floats output-gate-delta x))))))
        block-delta (float-array hidden-size)
        _ig (:input-gate lstm-state)
        _db (derivative (:block lstm-state) :tanh)
        _ (dotimes [x hidden-size] (aset ^floats block-delta x
                                         (float (* (sigmoid (aget ^floats _ig x)) (aget ^floats _db x) (aget ^floats cell-state-delta x)))))
        forget-gate-delta (float-array hidden-size)
        _dfg (derivative (:forget-gate lstm-state) :sigmoid)
        _ (dotimes [x hidden-size] (aset ^floats forget-gate-delta x
                                         (float (* (aget ^floats _dfg x) (aget ^floats cell-state:t-1 x) (aget ^floats cell-state-delta x)))))
        input-gate-delta (float-array hidden-size)
        _dig (derivative (:input-gate lstm-state) :sigmoid)
        _b (:block lstm-state)
        _ (dotimes [x hidden-size] (aset ^floats input-gate-delta x
                                         (float (* (aget ^floats _dig x) (tanh (aget ^floats _b x)) (aget ^floats cell-state-delta x)))))]
    {:output-gate-delta output-gate-delta :cell-state-delta cell-state-delta :block-delta block-delta
     :forget-gate-delta forget-gate-delta :input-gate-delta input-gate-delta}))

(defn lstm-param-delta
  [model lstm-part-delta x-input self-activation:t-1 self-state:t-1]
  (let [{:keys [utt-embedding hidden-size]} model
        {:keys [sparse? sparses]} utt-embedding
        {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
        block-wr-delta       (outer block-delta self-activation:t-1)
        input-gate-wr-delta  (outer input-gate-delta self-activation:t-1)
        forget-gate-wr-delta (outer forget-gate-delta self-activation:t-1)
        output-gate-wr-delta (outer output-gate-delta self-activation:t-1)
        peephole-input-gate  (times input-gate-delta  (:cell-state self-state:t-1))
        peephole-forget-gate (times forget-gate-delta (:cell-state self-state:t-1))
        peephole-output-gate (times output-gate-delta (:cell-state self-state:t-1))
        template {:block-wr-delta block-wr-delta :input-gate-wr-delta input-gate-wr-delta
                  :forget-gate-wr-delta forget-gate-wr-delta :output-gate-wr-delta output-gate-wr-delta
                  :block-bias-delta block-delta
                  :input-gate-bias-delta input-gate-delta
                  :forget-gate-bias-delta forget-gate-delta
                  :output-gate-bias-delta output-gate-delta
                  :peephole-input-gate-delta peephole-input-gate :peephole-forget-gate-delta peephole-forget-gate
                  :peephole-output-gate-delta peephole-output-gate}
        param-delta (if sparse?
                      (-> template (assoc :sparses-delta (hash-map x-input {:block-w-delta block-delta
                                                                            :input-gate-w-delta input-gate-delta
                                                                            :forget-gate-w-delta forget-gate-delta
                                                                            :output-gate-w-delta output-gate-delta})))
                      (let [block-w-delta        (outer block-delta x-input)
                            input-gate-w-delta   (outer input-gate-delta x-input)
                            forget-gate-w-delta  (outer forget-gate-delta x-input)
                            output-gate-w-delta  (outer output-gate-delta x-input)]
                        (-> template (assoc
                                       :block-w-delta block-w-delta :input-gate-w-delta input-gate-w-delta
                                       :forget-gate-w-delta forget-gate-w-delta :output-gate-w-delta output-gate-w-delta))))]
    param-delta))

(defn output-param-delta
  [word-delta-pairs hidden-size hidden-activation]
  (->> word-delta-pairs
       (mapv (fn [[word delta]]
               {:word word
                :w-delta    (times hidden-activation (float-array (repeat hidden-size delta)))
                :bias-delta delta}))))

(defn bptt
  [model x-seq positives negatives & [option]]
  (let [{:keys [hidden-size output utt-embedding]} model
        {:keys [block-wr input-gate-wr forget-gate-wr output-gate-wr
                input-gate-peephole forget-gate-peephole output-gate-peephole]} utt-embedding
        model-output-seq (sequential-output model x-seq positives negatives option)
        output-w (:w output)
        output-delta (negative-sampling hidden-size model-output-seq positives negatives)
        output-param-delta (output-param-delta output-delta hidden-size (second (:activation (last model-output-seq))))
        propagated-delta (->> output-delta
                              (map (fn [[word delta]]
                                     (let [w (get output-w word)
                                           v (float-array (repeat hidden-size delta))]
                                       (times w v))))
                              (apply sum))]
    (loop [propagated-delta propagated-delta,
           oseq (reverse model-output-seq)
           x-seq (reverse x-seq)
           self-delta:t+1 {:block-delta       (float-array hidden-size)
                           :input-gate-delta  (float-array hidden-size)
                           :forget-gate-delta (float-array hidden-size)
                           :output-gate-delta (float-array hidden-size)
                           :cell-state-delta  (float-array hidden-size)}
           lstm-state:t+1 {:forget-gate (float-array hidden-size)}
           acc nil]
      (if-let [output-layer (first oseq)]
        (let [lstm-state (second (:state (first oseq)))
              cell-state:t-1 (or (:cell-state (second (:state (second oseq)))) (float-array hidden-size))
              lstm-part-delta (lstm-part-delta hidden-size propagated-delta self-delta:t+1 lstm-state lstm-state:t+1 cell-state:t-1
                                               input-gate-peephole forget-gate-peephole output-gate-peephole)
              x-input (first x-seq)
              self-activation:t-1 (or (second (:activation (second oseq))) (float-array hidden-size))
              self-state:t-1      (or (second (:state      (second oseq))) {:cell-state (float-array hidden-size)})
              lstm-param-delta (lstm-param-delta model lstm-part-delta x-input self-activation:t-1 self-state:t-1)
              {:keys [block-delta input-gate-delta forget-gate-delta output-gate-delta]} lstm-part-delta
              propagated-delta:t-1 (->> (map (fn [w d]
                                               (gemv (transpose hidden-size w) d))
                                             [block-wr    input-gate-wr     forget-gate-wr    output-gate-wr]
                                             [block-delta input-gate-delta forget-gate-delta output-gate-delta])
                                        (apply sum))]
          (recur propagated-delta:t-1
                 (rest oseq)
                 (rest x-seq)
                 lstm-part-delta
                 (second (:state output-layer))
                 (if (nil? acc)
                   lstm-param-delta
                   (merge-with #(if (map? %1); if sparses
                                  (merge-with (fn [accw dw]
                                                (merge-with (fn [acc d]
                                                              (sum acc d))
                                                            accw dw))
                                              %1 %2)
                                  (sum %1 %2))
                               acc lstm-param-delta))))
        {:output-delta output-param-delta
         :utt-embedding-delta acc}))))


(defn update-model!
  [model param-delta-list learning-rate]
  (let [{:keys [output utt-embedding hidden-size]} model
        {:keys [output-delta utt-embedding-delta]} param-delta-list
        _ (->> output-delta
               (map (fn [{:keys [word w-delta bias-delta]}]
                      (let [w    (get (:w output) word)
                            bias (get (:bias output) word)]
                        (aset ^floats bias 0 (float (+ (aget ^floats bias 0) (* learning-rate bias-delta))))
                        (dotimes [x hidden-size]
                          (aset ^floats w x (float (+ (aget ^floats w x) (* learning-rate (aget ^floats w-delta x)))))))))
               doall)
        {:keys [block-w-delta block-wr-delta block-bias-delta input-gate-w-delta input-gate-wr-delta input-gate-bias-delta
                forget-gate-w-delta forget-gate-wr-delta forget-gate-bias-delta output-gate-w-delta output-gate-wr-delta output-gate-bias-delta
                peephole-input-gate-delta peephole-forget-gate-delta peephole-output-gate-delta sparses-delta]} utt-embedding-delta
        {:keys [block-w block-wr block-bias input-gate-w input-gate-wr input-gate-bias
                forget-gate-w forget-gate-wr forget-gate-bias output-gate-w output-gate-wr output-gate-bias
                input-gate-peephole forget-gate-peephole output-gate-peephole
                unit-num sparse? sparses]} utt-embedding
        ;update input connection
        _ (if sparse?
            (->> sparses-delta
                 vec
                 (mapv (fn [[word lstm-w-delta]]
                         (let [{:keys [block-w-delta input-gate-w-delta forget-gate-w-delta output-gate-w-delta]} lstm-w-delta
                               {:keys [block-w input-gate-w forget-gate-w output-gate-w]} (get sparses word)]
                           ;;                            (clojure.pprint/pprint block-w)
                           (dotimes [x hidden-size]
                             (aset ^floats block-w x (float (+ (aget ^floats block-w x) (* learning-rate (aget ^floats block-w-delta x)))))
                             (aset ^floats input-gate-w x (float (+ (aget ^floats input-gate-w x) (* learning-rate (aget ^floats input-gate-w-delta x)))))
                             (aset ^floats forget-gate-w x (float (+ (aget ^floats forget-gate-w x) (* learning-rate (aget ^floats forget-gate-w-delta x)))))
                             (aset ^floats output-gate-w x (float (+ (aget ^floats output-gate-w x) (* learning-rate (aget ^floats output-gate-w-delta x)))))))))
                 doall)
            (dotimes [x (count block-w-delta)]
              (aset ^floats block-w x (float (+ (aget ^floats block-w x) (* learning-rate (aget ^floats block-w-delta x)))))
              (aset ^floats input-gate-w x (float (+ (aget ^floats input-gate-w x) (* learning-rate (aget ^floats input-gate-w-delta x)))))
              (aset ^floats forget-gate-w x (float (+ (aget ^floats forget-gate-w x) (* learning-rate (aget ^floats forget-gate-w-delta x)))))
              (aset ^floats output-gate-w x (float (+ (aget ^floats output-gate-w x) (* learning-rate (aget ^floats output-gate-w-delta x)))))))
        ;update recurrent connection
        _ (dotimes [x (* hidden-size hidden-size)]
            (aset ^floats block-wr x (float (+ (aget ^floats block-wr x) (* learning-rate (aget ^floats block-wr-delta x)))))
            (aset ^floats input-gate-wr x (float (+ (aget ^floats input-gate-wr x) (* learning-rate (aget ^floats input-gate-wr-delta x)))))
            (aset ^floats forget-gate-wr x (float (+ (aget ^floats forget-gate-wr x) (* learning-rate (aget ^floats forget-gate-wr-delta x)))))
            (aset ^floats output-gate-wr x (float (+ (aget ^floats output-gate-wr x) (* learning-rate (aget ^floats output-gate-wr-delta x))))))
        ;update lstm biase and peephole
        _ (dotimes [x hidden-size]
            (aset ^floats block-bias x (float (+ (aget ^floats block-bias x) (* learning-rate (aget ^floats block-bias-delta x)))))
            (aset ^floats input-gate-bias x (float (+ (aget ^floats input-gate-bias x) (* learning-rate (aget ^floats input-gate-bias-delta x)))))
            (aset ^floats forget-gate-bias x (float (+ (aget ^floats forget-gate-bias x) (* learning-rate (aget ^floats forget-gate-bias-delta x)))))
            (aset ^floats output-gate-bias x (float (+ (aget ^floats output-gate-bias x) (* learning-rate (aget ^floats output-gate-bias-delta x)))))
            ;and peephole
            (when (aset ^floats input-gate-peephole  x
                        (float (+ (aget ^floats input-gate-peephole x)  (* learning-rate (aget ^floats peephole-input-gate-delta  x))))))
            (when (aset ^floats forget-gate-peephole x
                        (float (+ (aget ^floats forget-gate-peephole x) (* learning-rate (aget ^floats peephole-forget-gate-delta x))))))
            (when (aset ^floats output-gate-peephole x
                        (float (+ (aget ^floats output-gate-peephole x) (* learning-rate (aget ^floats peephole-output-gate-delta x)))))))]
    :done))

(defn random-array [n]
  (let [it (float-array n)]
    (dotimes [x n] (aset ^floats it x (model-rand)))
    it))

(defn init-utt2vec-model
  [wl em em-size input-type hidden-size]
  (let [wl (assoc wl "<unk>" 0);(if (string? wl) (clojure.edn/read-string (slurp wl)) wl)
        em (if (string? em) (load-model em) em)
        wl-keys (keys wl)]
    {:utt-embedding (let [bwr (random-array (* hidden-size hidden-size));for recurrent connection
                          bb  (random-array hidden-size)
                          iwr (random-array (* hidden-size hidden-size))
                          ib  (random-array hidden-size)
                          ip  (random-array hidden-size)
                          fwr (random-array (* hidden-size hidden-size))
                          fb  (random-array hidden-size)
                          fp  (random-array hidden-size)
                          owr (random-array (* hidden-size hidden-size))
                          ob  (random-array hidden-size)
                          op  (random-array hidden-size)
                          template {:unit-num hidden-size
                                    :block-wr       bwr   :block-bias           bb
                                    :input-gate-wr  iwr   :input-gate-bias      ib
                                    :forget-gate-wr fwr   :forget-gate-bias     fb
                                    :output-gate-wr owr   :output-gate-bias     ob
                                    :input-gate-peephole  ip  :forget-gate-peephole fp
                                    :output-gate-peephole op}]
                      (if (= input-type :sparse)
                        (let [sparses (reduce (fn [acc sparse]
                                                (assoc acc sparse {:block-w       (random-array hidden-size)
                                                                   :input-gate-w  (random-array hidden-size)
                                                                   :forget-gate-w (random-array hidden-size)
                                                                   :output-gate-w (random-array hidden-size)}))
                                              {} (keys wl))]
                          (-> template (assoc :sparses sparses :sparse? (= input-type :sparse))))
                        (let [bw  (random-array (* em-size hidden-size))
                              iw  (random-array (* em-size hidden-size))
                              fw  (random-array (* em-size hidden-size))
                              ow  (random-array (* em-size hidden-size))]
                          (-> template (assoc :block-w bw :input-gate-w iw  :forget-gate-w fw :output-gate-w ow)))))
     :output {:w    (reduce #(assoc %1 %2 (random-array hidden-size)) {} wl-keys)
              :bias (reduce #(assoc %1 %2 (float-array [(model-rand)])) {} wl-keys)}
     :input-type input-type
     :em-size em-size
     :hidden-size hidden-size
     :wl wl
     :em em}))



(defn train!
  [model x-seq positives negatives learning-rate & [option]]
  (let [delta-list (bptt model x-seq positives negatives option)]
    (update-model! model delta-list learning-rate)))



(defn- progress-format [done all interval-done interval-ms unit]
  (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] "
       done "/" all ", "
       (if (zero? interval-ms)
         "0.0"
         (format "%.1f" (float (/ interval-done (/ interval-ms 1000)))))
       " " unit " "
       (format "(%.2f" (float (* 100 (/ done all))))
       "%)"))
(comment
  (defn train-by-file!
    [model train-path & [option]]
    (let [interval-ms (or (:interval-ms option) 10000) ;; 10 seconds
          workers (or (:workers option) 4)
          initial-learning-rate (or (:learning-rate option) 0.05)
          min-learning-rate (or (:min-learning-rate option) 0.0001)
          all-lines-num (with-open [r (reader train-path)] (count (line-seq r)))
          local-counter (atom 0)
          done-workers (atom 0)]
      (let [r (reader train-path)]
        (dotimes [w workers]
          (go-loop [] (if-let [line (.readLine r)]
                        (let [progress (/ @local-counter all-lines-num)
                              learning-rate initial-learning-rate;(max (- initial-learning-rate (* initial-learning-rate progress)) min-learning-rate)
                              [x-seq positives negatives] (clojure.edn/read-string line)]
                          (train! model x-seq positives negatives learning-rate option)
                          (swap! local-counter inc)
                          (recur))
                        (swap! done-workers inc))))
        (loop [counter 0]
          (Thread/sleep interval-ms)
          (when-not (= @done-workers workers)
            (let [c @local-counter
                  next-counter (+ counter c)]
              (println (progress-format counter all-lines-num c interval-ms "line/s"))
              (reset! local-counter 0)
              (recur next-counter))))
        (.close r))
      ;;     (clojure.pprint/pprint option)
      :done))
  )
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
        (go (loop [negatives (shuffle (get-negatives neg-cum (* negative 100000)))]
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
                  (recur (if (empty? next-negatives)  (shuffle (get-negatives neg-cum (* negative 100000))) next-negatives)))
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

(defn make-utt2vec-model
  "input-type: :sparse or :embedding"
  [em-path em-size train-path export-path input-type size & [option]]
  (let [wl (make-wl train-path option)
        model (init-utt2vec-model wl (when em-path (load-model em-path)) em-size input-type size)]
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

(defn l2-normalize
  [^floats v]
  (let [acc (Math/sqrt (reduce + (times v v)))
        n (count v)
        ret (float-array n)]
    (amap ^floats v i ret (float (/ (aget ^floats v i) acc)))))

(defn similarity
  ([v1 v2 l2?]
   (float (if l2?
            (dot v1 v2)
            (dot (l2-normalize v1) (l2-normalize v2)))))
  ([v1 v2]
   (similarity v1 v2 true)))


;;;;;;;


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


