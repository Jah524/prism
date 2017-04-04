(ns sai-ai.negative-sampling)

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

