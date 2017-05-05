(ns server.view
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
     [:h1 {:style "text-align:center"} "Visualization with t-sne"]
     [:div {:id "main" :v-cloak ""}
      [:div {:class "row"}
       [:div {:class "col-sm-1"}]
       [:div {:class "col-sm-10"}
        [:div {:id "tsne-plot"}]]]
      [:div {:id "items" :class "row"}
       [:div {:class "col-sm-1"}]
       [:div {:class "col-sm-10"}
        [:form {:action "javascript:void(0);"}
         [:h2 "Items"]
         [:div {:class "row"}
          [:div {:class "col-sm-6"}
           [:h4 "add items by file"]
           [:input {:type "file" :id "file-upload"}]]
          [:div {:class "col-sm-6"}
           [:h4 "add item as you like"]
           [:div {:class "input-group"}
            [:span {:class "input-group-btn"}
             [:input {:id "add-item" :class "form-control" :v-model "item" :placeholder "word or tokens"}]
             [:div {:class "btn btn-primary" :v-on:click "add_item(item)" } "add"]]]]]
         [:h2 "t-sne parameters"]
         [:div {:class "row"}
          [:div {:class "col-sm-6"}
           [:div {:class "input-group"}
            [:span {:class "input-group-addon" :id "perplexity-desc"} "Perplexity"]
            [:input {:class "form-control" :v-model.number "perplexity" :placeholder "number of t-sne perplexity (over 5.0)"
                     :aria-describedby "perplexity-desc" :type "number"}]]]
          [:div {:class "col-sm-6"}
           [:div {:class "input-group"}
            [:span {:class "input-group-addon" :id "iters-desc"} "iterations"]
            [:input {:type "number" :class "form-control" :id "iters" :aria-describedby "iters-desc"
                     :placeholder "number of t-sne iterations" :v-model.number "iters"}]]]]
         [:div {:style "text-align:center;margin: 18px 0px;"} "and"]
         [:div {:class "btn btn-success btn-block btn-lg" :v-on:click "fetch_data()"} "{{btn_text}}"]
         [:hr]
         [:div {:class "row"}
          [:div {:class "col-sm-6"}
           [:h4 "All items"]
           [:div {:v-for "item in item_list"}
            "{{item}}"]]
          [:div {:class "col-sm-6"}
           [:h4 "Skipped items"]
           [:div {:v-for "skipped in skipped_items"} "{{skipped.item}}"]]]
         ]]]]
     [:script {:type "text/javascript" :src "https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"}]
     [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
               :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
               :crossorigin "anonymous"}]
     [:script {:src "https://unpkg.com/vue"}]
     [:script {:type "text/javascript" :src "https://cdn.plot.ly/plotly-latest.min.js"}]
     [:script {:type "text/javascript" :src "/js/tsne.js"  :charset "utf-8"}]]))



(defn word-probability-given-context []
  (html5
    [:head
     [:meta  {:charset "utf-8"}]
     [:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
             :integrity "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" :crossorigin "anonymous"}]
     [:style {:type "text/css"} "[v-cloak]{opacity: 0;}"]
     [:title "Word probability given context"]
     ]
    [:body
     [:h1 {:style "text-align:center;margin-bottom:32px;"} "Word probability given context"]
     [:div {:id "main" };:v-cloak ""}
      [:div {:class "row"}
       [:div {:class "col-sm-2"}]
       [:div {:class "col-sm-8"}
        [:form {:action "javascript:void(0);"}
         [:h2 "1. context"]
         [:div {:class "row"}
          [:div {:class "col-sm-6"}
           [:h4 "add context by file"]
           [:input {:type "file" :id "file-upload"}]]
          [:div {:class "col-sm-6"}
           [:h4 "add context as you like"]
           [:div {:class "input-group"}
            [:span {:class "input-group-btn"}
             [:input {:id "add-context" :class "form-control" :v-model "context" :placeholder "tokens (e.g. natural language)"}]
             [:div {:class "btn btn-primary" :v-on:click "add_context(context)" } "add"]]]]]
         [:div {:v-for "context in context_list"} "{{context}}"]
         [:h2 "2. Target word"]
         [:div {:class "input-group"}
          [:span {:class "input-group-btn"}
           [:input {:id "add-word" :class "form-control" :v-model "word" :placeholder "words (e.g. processing)"}]
           [:div {:class "btn btn-primary" :v-on:click "add_words(word)" } "add"]]]
         [:div [:span {:v-for "word in word_list"} "{{word}} "]]
         [:div {:style "text-align:center;margin:12px;"} "and"]
         [:div {:class "btn btn-success btn-block btn-lg" :v-on:click "fetch_data()"} "{{btn_text}}"]]
        [:hr]
        [:table {:class "table"}
         [:thead
          [:th "context"]
          [:th "unknown"]
          [:th {:v-for "k in known_list"} "{{k}}"]
         [:tbody
          [:tr {:v-for "prob in prob_list"}
           [:th "{{prob.context}}"]
           [:th "{{prob.prob['context-unk']}}"]
           [:td {:v-for "p in prob.prob.items"} "{{p}}"]]]]

         ]]]]
     [:script {:type "text/javascript" :src "https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"}]
     [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
               :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
               :crossorigin "anonymous"}]
     [:script {:src "https://unpkg.com/vue"}]
     [:script {:type "text/javascript" :src "/js/word-probability-given-context.js"  :charset "utf-8"}]]))
