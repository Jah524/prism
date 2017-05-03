(ns server.cljs.tsne.tsne
  (:require
    [clojure.string :refer [split-lines]]
    [ajax.core :refer [POST]]))

(enable-console-print!)

(defn text-embedding->plotly-data
  [{:strs [item x y]}]
  {:x [x]
   :y [y]
   :text [item]
   :hoverinfo "none"
   :showlegend false
   :type "scater"
   :mode "markers+text"
   :textposition "bottom"})


(def vm
  (js/Vue.
    (clj->js
      {:el "#main"
       :data {:btn_text "get word position and redraw"
              :perplexity 5.0
              :iters 1000
              :item ""
              :item_list [] ; [word1, word2 ...]
              :item_list_plot [] ; [{:item word1 :x 123 :y 42} ...]
              :skipped_items []}
       :methods {
                  :add_item (fn [item]
                              (this-as
                                me
                                (.push (aget me "item_list") item)
                                (aset me "item" "")
                                ))
                  :update_plot
                  (fn []
                    (this-as me
                             (.newPlot js/Plotly (.getElementById js/document "tsne-plot")
                                       (->> (aget me "item_list_plot") js->clj (mapv #(text-embedding->plotly-data %)) clj->js)
                                       (clj->js {:height 600}))))
                  :fetch_data
                  (fn []
                    (this-as me
                             (let [items (->> (js->clj (aget me "item_list")))]
                               (aset me "btn_text" "fetching data ...")
                               (POST "/"
                                     {:params {:items items
                                               :perplexity (aget me "perplexity")
                                               :iters (aget me "iters")}
                                      :format :json
                                      :response-format :json
                                      :handler (fn [res]
                                                 (let [{:strs [skipped result condition]} (js->clj res)
                                                       target  (aget me "item_list_plot")
                                                       skipped-items (aget me "skipped_items")]
                                                   (when (= condition "model-has-gone") (.alert js/window "model has gone, you need to restart server"))
                                                   ;; add result of words
                                                   (.splice target 0 (count target))
                                                   (->> result
                                                        (map #(.push target (clj->js %)))
                                                        dorun)
                                                   ;; add skipped items
                                                   (.splice skipped-items 0 (count skipped-items))
                                                   (->> skipped
                                                        (map #(.push skipped-items (clj->js %)))
                                                        dorun))
                                                 (aset me "btn_text" "get word position and redraw")
                                                 ((aget me "update_plot")))
                                      :error-handler (fn [{:keys [status status-text]}]
                                                       (println (str "error has occured " status ":" status-text)))}))))}
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
                                                 (->> lines (map #(.push (aget me "item_list") %)) dorun))))))))

                  )})))


