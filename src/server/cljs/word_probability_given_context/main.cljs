(ns server.cljs.word-probability-given-context.main
  (:require
    [clojure.string :refer [split split-lines]]
    [ajax.core :refer [POST]]))


(enable-console-print!)


(def vm
  (js/Vue.
    (clj->js
      {:el "#main"
       :data {:context ""
              :context_list []
              :word ""
              :word_list []
              :known_list []
              :btn_text "go ahead"
              :prob_list []}
       :methods {:add_context (fn [item]
                                 (this-as
                                   me
                                   (.unshift (aget me "context_list") (clj->js (split item #" ")))
                                   (aset me "context" "")
                                   ))
                 :add_words (fn [item]
                              (this-as
                                me
                                (->> (split item #" ") (map #(.unshift (aget me "word_list") %)) doall)
                                (aset me "word" "")
                                ))
                 :fetch_data
                 (fn []
                   (this-as me
                            (let [word-list (js->clj (aget me "word_list"))
                                  context-list (js->clj (aget me "context_list"))]
                              (aset me "btn_text" "fetching data ...")
                              (POST "/word-probability-given-context"
                                    {:params {:word-list word-list
                                              :context-list context-list}
                                     :format :json
                                     :response-format :json
                                     :handler (fn [res]
                                                (let [target (aget me "prob_list")
                                                      known-list (aget me "known_list")]
;;                                                   (println (get res "items"))
;;                                                   (println (get res "known-items"))
                                                  (.splice target 0 (count target))
                                                  (.splice known-list 0 (count known-list))
                                                  (mapv #(.push known-list (clj->js %)) (get res "known-items"))
                                                  (mapv #(.push target (clj->js %)) (get res "items"))
                                                  (aset me "btn_text" "go ahead")))
                                     :error-handler (fn [{:keys [status status-text]}]
                                                      (println (str "error has occured " status ":" status-text)))}))))
                 }
       :mounted (fn []
                  (this-as
                    me
                    (.change (js/$ "#file-upload")
                             (fn [e] (let [file (first (array-seq (aget e "target" "files")))
                                           reader (js/FileReader.)]
                                       (.readAsText reader file)
                                       (aset reader "onload"
                                             (fn []
                                               (let [result (aget reader "result")
                                                     lines (split-lines result)]
                                                 (->> lines (map #(.push (aget me "context_list") (clj->js (split % #" ")))) doall))))))))

                  )})))
