(ns server.tsne
  (:require
    [hiccup.page :refer [html5]]))

(def tsne
  (html5
    [:head
     [:meta  {:charset "utf-8"}]
     [:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
             :integrity "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" :crossorigin "anonymous"}]
     [:style {:type "text/css"} "[v-cloak]{opacity: 0;}"]
     [:title "visualization"]
     ]
    [:body
     [:div {:id "main"}
      [:div {:class "row"}
       [:div {:class "col-sm-1"}]
       [:div {:class "col-sm-10"}
        [:h1 {:style "text-align:center"} "visualization with t-sne"]
        [:div {:id "tsne-plot"}]]]
      [:div {:id "items" :class "row" :v-cloak ""}
       [:div {:class "col-sm-1"}]
       [:div {:class "col-sm-10"}
        [:form
         [:div {:class "btn btn-info" :v-on:click "update_plot"} "TEST!"]
         [:div {:class "input-group"}
          [:input {:id "add-item" :class "form-control" :v-model "item" :placeholder "word or tokens"}]
          [:span {:class "input-group-btn"}
           [:div {:class "btn btn-primary" :v-on:click "add_item(item)" } "add item"]]]
         [:div {:style "text-align:center;"} "and"]
         [:div {:class "btn btn-success btn-block"} "fetch result"]
         [:h3 "Items"]
         [:div {:v-for "item in item_list"}
          "{{item.item}}"]]]]]
     [:script {:type "text/javascript" :src "https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"}]
     [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
               :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
               :crossorigin "anonymous"}]
     [:script {:src "https://unpkg.com/vue"}]
     [:script {:type "text/javascript" :src "https://cdn.plot.ly/plotly-latest.min.js"}]
     [:script {:type "text/javascript" :src "/js/tsne.js"  :charset "utf-8"}]]))

