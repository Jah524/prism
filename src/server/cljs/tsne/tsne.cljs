(ns server.cljs.tsne.tsne
  (:require [ajax.core :refer [POST]]))

(enable-console-print!)

(defn text-embedding->plotly-data
  [{:strs [item x y]}]
  (println item x y)
  {:x [x]
   :y [y]
   :text [item]
   :hoverinfo "none"
   :showlegend false
   :type "scater"
   :mode "markers+text"
   :textposition "bottom"})

(def sample-word-list
;;   [{:item "あ"} {:item "い"} {:item "忖度"} {:item "今日"} {:item "私"} {:item "は"}{:item "の"}
;;    {:item "それ"} {:item "。"} {:item "?"} {:item "え"} {:item "マジ"} {:item "！"} {:item "に"}
;;    {:item "あなた"} {:item "ね"} {:item "た"} {:item "にも"} {:item "お腹"} {:item "qweqwgei"}
;;    {:item "そこ"} {:item "つまり"} {:item "日"} {:item "〜"} {:item "が"}])
  [{:item "I"} {:item "you"} {:item "happy"} {:item "bad"} {:item "a"} {:item "an"} {:item "and"} {:item "or"}
   {:item "what"} {:item "where"} {:item "god"} {:item "did"} {:item "much"} {:item "beautiful"} {:item "never"}
   {:item "."} {:item "!"} {:item "oh"} {:item "green"} {:item "blue"} {:item "red"} {:item "matter"} {:item "home"}
   {:item "slightly"} {:item "end"} {:item "he"} {:item "she"} {:item "newspaper"} {:item "more"} {:item "when"} {:item "dream"}
   {:item "default"} {:item "international"} {:item "desk"} {:item "chair"} {:item "world"} {:item "new"} {:item "run"} {:item "get"}
   {:item "use"} {:item "luck"} {:item "money"} {:item "year"} {:item "very"} {:item "less"} {:item "computer"}
   {:item "the"} {:item "morning"} {:item "yesterday"} {:item "clock"} {:item "last"} {:item "worth"} {:item "children"}
   {:item "music"} {:item "sports"} {:item "ball"} {:item "math"} {:item "English"} {:item "IT"} {:item "it"} {:item "they"}
   {:item "so"} {:item "sky"} {:item "auto"} {:item "car"}])

(def word-set (atom #{}))

(def vm
  (js/Vue.
    (clj->js
      {:el "#items"
       :data {:test "あばば"
              :plot_area (.getElementById js/document "tsne-plot")
              :perplexity 5.0
              :iters 1000
              :item ""
              :item_list (clj->js sample-word-list)
              :item_list_plot []
              :skipped_items []}
       :methods {
                  :add_item (fn [item]
                              (this-as
                                me
                                (.push (aget me "item_list") (clj->js {:item item}))
                                (aset me "item" "")
                                ))
                  :update_plot
                  (fn []
                    (this-as me
                             (.newPlot js/Plotly (aget me "plot_area")
                                       (->> (aget me "item_list_plot") js->clj (mapv #(text-embedding->plotly-data %)) clj->js)
                                       (clj->js {}))))
                  :fetch_data
                  (fn []
                    (this-as me
                             (let [items (->> (js->clj (aget me "item_list")) (map #(get % "item")) clj->js)]
                               (POST "/"
                                     {:params {:items items
                                               :perplexity (aget me "perplexity")
                                               :iters (aget me "iters")}
                                      :format :json
                                      :response-format :json
                                      :handler (fn [res]
                                                 (let [{:strs [skipped result]} (js->clj res)
                                                       target  (aget me "item_list_plot")
                                                       skipped-items (aget me "skipped_items")]
                                                   (println result)
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
                                                 ((aget me "update_plot")))
                                      :error-handler (fn [{:keys [status status-text]}]
                                                       (println (str "error has occured " status ":" status-text)))}))))}
       :mounted (fn []

                  )})))


