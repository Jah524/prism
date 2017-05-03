(ns server.tsne
  (:require
    [hiccup.page :refer [html5]]))

(defn tsne [model-type]
  (html5
    [:head
     [:meta  {:charset "utf-8"}]
     [:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
             :integrity "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" :crossorigin "anonymous"}]
     [:style {:type "text/css"} "[v-cloak]{opacity: 0;}"]
     [:title "Visualization"]
     ]
    [:body
     [:div {:id "main"}
      [:div {:class "row"}
       [:div {:class "col-sm-1"}]
       [:div {:class "col-sm-10"}
        [:h1 {:style "text-align:center"} "Visualization with t-sne"]
        [:div {:id "tsne-plot"}]]]
      [:div {:id "items" :class "row" :v-cloak ""}
       [:div {:class "col-sm-1"}]
       [:div {:class "col-sm-10"}
        [:form
         [:h3 "Items"]
         [:h4 "Skipped items"]
         [:div {:v-for "skipped in skipped_items"}
          "{{skipped.item}}"]
         [:h4 "add item"]
         [:div {:class "input-group"}
          [:span {:class "input-group-btn"}
           [:input {:id "add-item" :class "form-control" :v-model "item" :placeholder "word or tokens"}]
           [:div {:class "btn btn-primary" :v-on:click "add_item(item)" } "add"]]]
         [:h3 "t-sne parameters"]
         [:div {:class "row"}
          [:div {:class "col-sm-6"}
           [:div {:class "input-group"}
            [:span {:class "input-group-addon" :id "perplexity-desc"} "Perplexity"]
            [:input {:class "form-control" :v-model.number "perplexity" :placeholder "nnumber of t-sne perplexity (over 5.0)"
                     :aria-describedby "perplexity-desc" :type "number"}]]]
          [:div {:class "col-sm-6"}
           [:div {:class "input-group"}
            [:span {:class "input-group-addon" :id "iters-desc"} "iterations"]
            [:input {:type "number" :class "form-control" :id "iters" :aria-describedby "iters-desc"
                     :placeholder "number of t-sne iterations" :v-model.number "iters"}]]]]
         [:div {:style "text-align:center;margin: 18px 0px;"} "and"]
         [:div {:class "btn btn-success btn-block" :v-on:click "fetch_data()"} "fetch data and redraw"]
         [:h3 "Items"]
;;          [:div {:v-for "item in item_list"}
;;           "{{item.item}}"]
]]]]
     [:script {:type "text/javascript" :src "https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"}]
     [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
               :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
               :crossorigin "anonymous"}]
     [:script {:src "https://unpkg.com/vue"}]
     [:script {:type "text/javascript" :src "https://cdn.plot.ly/plotly-latest.min.js"}]
     [:script {:type "text/javascript" :src "/js/tsne.js"  :charset "utf-8"}]]))

