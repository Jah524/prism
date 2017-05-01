// Compiled by ClojureScript 1.9.521 {:static-fns true, :optimize-constants true}
goog.provide('server.cljs.tsne.tsne');
goog.require('cljs.core');
goog.require('cljs.core.constants');
cljs.core.enable_console_print_BANG_();
server.cljs.tsne.tsne.text_embedding__GT_plotly_data = (function server$cljs$tsne$tsne$text_embedding__GT_plotly_data(text,x,y){
return new cljs.core.PersistentArrayMap(null, 8, [cljs.core.cst$kw$x,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [x], null),cljs.core.cst$kw$y,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [y], null),cljs.core.cst$kw$text,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [text], null),cljs.core.cst$kw$hoverinfo,"none",cljs.core.cst$kw$showlegend,false,cljs.core.cst$kw$type,"scater",cljs.core.cst$kw$mode,"markers+text",cljs.core.cst$kw$textposition,"bottom"], null);
});
server.cljs.tsne.tsne.vm = (new Vue(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [cljs.core.cst$kw$el,"#items",cljs.core.cst$kw$data,new cljs.core.PersistentArrayMap(null, 4, [cljs.core.cst$kw$test,"\u3042\u3070\u3070",cljs.core.cst$kw$plot_area,document.getElementById("tsne-plot"),cljs.core.cst$kw$item,"",cljs.core.cst$kw$item_list,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"\u3042"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"\u3044"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"\u5FD6\u5EA6"], null)], null)], null),cljs.core.cst$kw$methods,new cljs.core.PersistentArrayMap(null, 2, [cljs.core.cst$kw$add_item,(function (item){
var me = this;
(me["item_list"]).push(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,item], null)));

return (me["item"] = "");
}),cljs.core.cst$kw$update_plot,(function (){
var me = this;
return Plotly.newPlot((me["plot_area"]),cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [server.cljs.tsne.tsne.text_embedding__GT_plotly_data("\u3042",0.042,-0.0032),server.cljs.tsne.tsne.text_embedding__GT_plotly_data("\u3044",0.08,0.012),server.cljs.tsne.tsne.text_embedding__GT_plotly_data("\u3046",0.032,-0.0012)], null)),cljs.core.clj__GT_js(cljs.core.PersistentArrayMap.EMPTY));
})], null),cljs.core.cst$kw$mounted,(function (){
return null;
})], null))));
