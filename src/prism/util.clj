(ns prism.util
  (:require
    [clojure.java.io :refer [reader writer]]
    [clojure.string :as str]
    [clojure.core.async :refer [go go-loop thread >! <! >!! <!! chan timeout alt! alts! close!]]
    [clj-time.local :as l]
    [clj-time.core  :as t]
    [taoensso.nippy :refer [freeze-to-out! thaw-from-in!]]
    [matrix.default :refer [sum times dot]]
    ))

(defn save-model [obj target-path]
  (with-open [w (clojure.java.io/output-stream target-path)]
    (freeze-to-out! (java.io.DataOutputStream. w) obj)))

(defn load-model [target-path]
  (with-open [w (clojure.java.io/input-stream target-path)]
    (thaw-from-in! (java.io.DataInputStream. w))))

(defn progress-format [done all interval-done interval-ms unit]
  (str "["(l/format-local-time (l/local-now) :basic-date-time-no-ms)"] "
       done "/" all ", "
       (if (zero? interval-ms)
         "0.0"
         (format "%.1f" (float (/ interval-done (/ interval-ms 1000)))))
       " " unit " "
       (format "(%.3f" (float (* 100 (/ done all))))
       "%)"))

(defn make-wc
  [input-file & [option]]
  (let [{:keys [min-count wc workers interval-ms step]
         :or {min-count 5
              wc {}
              workers 4
              interval-ms 30000 ; 30 seconds
              step 1000000}} option
        all-lines (with-open [r (reader input-file)] (count (line-seq r)))
        over-all     (atom 0)
        done-lines   (atom 0)
        done-workers (atom 0)
        all-wc (atom {})]
    (with-open [r (reader input-file)]
      (dotimes [w workers]
        (go-loop [local-wc {} local-counnter step]
                 (if-let [line (.readLine r)]
                   (let [word-freq (frequencies (remove #(or (= "<eos>" %) (= "" %) (= " " %) (= "　" %)) (str/split line #" ")))
                         updated-wc (merge-with + local-wc word-freq)]
                     (swap! done-lines inc)
                     (if (zero? local-counnter)
                       (do
                         (reset! all-wc (merge-with + @all-wc updated-wc))
                         (recur {} step))
                       (recur updated-wc (dec step))))
                   (do
                     (reset! all-wc (merge-with + @all-wc local-wc))
                     (swap! done-workers inc)))))
      (loop [c 0]
        (if-not (= @done-workers workers)
          (let [diff @done-lines
                updated-c (+ c diff)
                _ (reset! done-lines 0)]
            (println (progress-format updated-c all-lines diff interval-ms "lines/s"))
            (Thread/sleep interval-ms)
            (recur updated-c))
          (reduce (fn [acc [k v]] (if (>= v min-count)
                                    (assoc acc k (+ v (get acc k 0)))
                                    (assoc acc "<unk>" (+ v (get acc "<unk>" 0)))))
                  wc
                  @all-wc))))))


(defn l2-normalize
  [^floats v]
  (let [acc (Math/sqrt (reduce + (times v v)))
        n (count v)
        ret (float-array n)]
    (amap ^floats v i ret (float (/ (aget ^floats v i) acc)))))

(defn l2-normalize!
  "caution, this function is destructive but is more memory efficiently"
  [^floats v]
  (let [acc (Math/sqrt (sum (times v v)))
        n (count v)]
    (dotimes [x n] (aset ^floats v x (float (/ (aget ^floats v x) acc))))))


(defn similarity
  ([v1 v2 l2?]
   (float (if l2?
            (dot v1 v2)
            (dot (l2-normalize v1) (l2-normalize v2)))))
  ([v1 v2]
   (similarity v1 v2 true)))
