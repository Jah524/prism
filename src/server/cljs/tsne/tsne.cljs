(ns server.cljs.tsne.tsne)

(enable-console-print!)

(defn text-embedding->plotly-data
  [text x y]
  {:x [x]
   :y [y]
   :text [text]
   :hoverinfo "none"
   :showlegend false
   :type "scater"
   :mode "markers+text"
   :textposition "bottom"})


(def vm
  (js/Vue.
    (clj->js
      {:el "#items"
       :data {:test "あばば"
              :plot_area (.getElementById js/document "tsne-plot")
              :item ""
              :item_list [{:item "あ"} {:item "い"} {:item "忖度"}]}
       :methods {
                  :add_item (fn [item]
                              (this-as
                                me
                                (.push (aget me "item_list") (clj->js {:item item}))
                                (aset me "item" "")
                                ))
                  :update_plot　;fetch data and redraw plot
                  (fn []
                    (this-as me
                             (.newPlot js/Plotly (aget me "plot_area")
                                       (clj->js [(text-embedding->plotly-data "あ" 0.042 -0.0032)
                                                 (text-embedding->plotly-data "い" 0.08 0.012)
                                                 (text-embedding->plotly-data "う" 0.032 -0.0012)])
                                       (clj->js {}))))}
       :mounted (fn []

                  )})))


