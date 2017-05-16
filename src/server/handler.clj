(ns server.handler
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [clojure.tools.cli :refer [parse-opts]]
    [compojure.core :refer [defroutes GET POST]]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [ring.middleware.reload :refer [wrap-reload]]
    [org.httpkit.server :refer [run-server]]
    [clojure.core.matrix :as m]
    [think.tsne.core :as tsne]
    [server.view :refer [tsne word-probability-given-context]]
    [prism.nlp.word2vec :as w2v]
    [prism.nlp.rnnlm :as rnnlm]))

(def m-path (atom nil))
(def model-type (atom nil))
(def model (atom nil))

(defroutes app-routes
  (route/resources "/")
  (GET  "/" [] (tsne @model-type))
  (POST "/"
        {:keys [params body] :as req}
        (let [contents (json/read-str (.readLine (io/reader body)))
              {:strs [items perplexity iters]} contents
              item-vec (condp = @model-type
                         "word2vec" (->> items flatten (mapv (fn [item] {:item item :v (w2v/word2vec @model item)})))
                         "rnnlm" (->> items (mapv (fn [words] {:item (->> words (interpose " ") (apply str)) :v (rnnlm/text-vector @model words)})))
                         :model-has-gone)]
          (if (= item-vec :model-has-gone)
            (do (println "you need to restart server, model has gone.") (json/write-str {:condition "model-has-gone"}))
            (let [target-items (->> item-vec (remove (fn [{:keys [v]}] (nil? v))) (map :item))
                  skipped (->> item-vec (filter (fn [{:keys [v]}] (nil? v))))
                  input-matrix (->> item-vec
                                    (keep (fn [{:keys [item v]}]
                                            (when (contains? (set target-items) item)
                                              v)))
                                    object-array)]
              (when-not (= (count target-items) (count input-matrix)) (throw (Exception. "items and matrices have to be same lenght")))
              (json/write-str {:skipped skipped
                               :condition "ok"
                               :result (mapv (fn [item [x y]] {:item item :x x :y y})
                                             target-items
                                             (map seq (tsne/tsne input-matrix 2
                                                                 ;;                                                              :tsne-algorithm :parallel-bht
                                                                 :tsne-algorithm :bht
                                                                 :perplexity perplexity
                                                                 :iters iters)))})))))
  (GET  "/word-probability-given-context" [] (word-probability-given-context))
  (POST "/word-probability-given-context"
        {:keys [params body] :as req}
        (let [contents (json/read-str (.readLine (io/reader body)))
              {:strs [context-list word-list]} contents
              {:keys [wc]} @model
              unk-items (filter #(not (get wc %)) word-list)
              known-items (filter #(get wc %) word-list)
              probs (->> context-list
                         (mapv #(rnnlm/prob-word-given-context @model % known-items)))]
          (json/write-str {:known-items known-items
                           :unk-items unk-items
                           :items (mapv (fn [s p] {:context (->> s (interpose " ") (apply str))
                                                   :prob p})
                                        context-list
                                        probs)})))


  (route/not-found "404 not found"))

(def app
  (-> #'app-routes
      wrap-reload ;; comment out when you use learned model, atoms are changed to nil by reloader
      handler/site))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :default 3000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> [""
        "Usage: lein run -m server.handler [options] model-type model-path"
        ""
        "Options:"
        options-summary
        ""
        "model-type:"
        "  word2vec    Word2Vec embedding"
        "  rnnlm       RNNLM model"
        ""]
       (string/join \newline)))

(defn validate
  [args]
  (let [{:keys [options arguments errors summary] :as args} (parse-opts args cli-options)
        {:keys [help port]} options
        [type model-path] arguments
        model-exists? (and model-path (.exists (io/as-file model-path)))]
    (cond
      help
      {:exit-message (usage summary)}
      (not model-exists?)
      {:exit-message "model doesn't exist"}
      (and (#{"word2vec" "rnnlm"} type)
           model-exists?)
      {:type type :model-path model-path :port port}
      :else
      {:exit-message (usage summary)})))


(defn -main [& args]
  (let [{:keys [port type model-path exit-message]} (validate args)]
    (if (and port type)
      (do
        (println (str "loading " model-path " ..."))
        (reset! m-path model-path)
        (case type
          "word2vec" (do (reset! model-type "word2vec") (reset! model (w2v/load-embedding model-path nil)))
          "rnnlm"    (do (reset! model-type "rnnlm"   ) (reset! model (rnnlm/load-model model-path nil))))
        (println (str @model-type " model was loaded"))
        (run-server app  {:port port})
        (println (str "Server listening on port " port)))
      (println exit-message))))

