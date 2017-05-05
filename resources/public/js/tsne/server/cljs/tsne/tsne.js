// Compiled by ClojureScript 1.9.521 {:static-fns true, :optimize-constants true}
goog.provide('server.cljs.tsne.tsne');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('clojure.string');
goog.require('ajax.core');
cljs.core.enable_console_print_BANG_();
server.cljs.tsne.tsne.text_embedding__GT_plotly_data = (function server$cljs$tsne$tsne$text_embedding__GT_plotly_data(p__13969){
var map__13972 = p__13969;
var map__13972__$1 = ((((!((map__13972 == null)))?((((map__13972.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__13972.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__13972):map__13972);
var item = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13972__$1,"item");
var x = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13972__$1,"x");
var y = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13972__$1,"y");
return new cljs.core.PersistentArrayMap(null, 8, [cljs.core.cst$kw$x,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [x], null),cljs.core.cst$kw$y,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [y], null),cljs.core.cst$kw$text,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [item], null),cljs.core.cst$kw$hoverinfo,"none",cljs.core.cst$kw$showlegend,false,cljs.core.cst$kw$type,"scater",cljs.core.cst$kw$mode,"markers+text",cljs.core.cst$kw$textposition,"bottom"], null);
});
server.cljs.tsne.tsne.vm = (new Vue(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [cljs.core.cst$kw$el,"#main",cljs.core.cst$kw$data,new cljs.core.PersistentArrayMap(null, 7, [cljs.core.cst$kw$btn_text,"get word position and draw",cljs.core.cst$kw$perplexity,5.0,cljs.core.cst$kw$iters,(1000),cljs.core.cst$kw$item,"",cljs.core.cst$kw$item_list,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$item_list_plot,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$skipped_items,cljs.core.PersistentVector.EMPTY], null),cljs.core.cst$kw$methods,new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$add_item,(function (item){
var me = this;
(me["item_list"]).push(cljs.core.clj__GT_js(clojure.string.split.cljs$core$IFn$_invoke$arity$2(item,/ /)));

return (me["item"] = "");
}),cljs.core.cst$kw$update_plot,(function (){
var me = this;
return Plotly.newPlot(document.getElementById("tsne-plot"),cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(((function (me){
return (function (p1__13974_SHARP_){
return server.cljs.tsne.tsne.text_embedding__GT_plotly_data(p1__13974_SHARP_);
});})(me))
,cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["item_list_plot"])))),cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$height,(640)], null)));
}),cljs.core.cst$kw$fetch_data,(function (){
var me = this;
var items = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["item_list"]));
(me["btn_text"] = "fetching data ...");

return ajax.core.POST.cljs$core$IFn$_invoke$arity$variadic("/",cljs.core.array_seq([new cljs.core.PersistentArrayMap(null, 5, [cljs.core.cst$kw$params,new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$items,items,cljs.core.cst$kw$perplexity,(me["perplexity"]),cljs.core.cst$kw$iters,(me["iters"])], null),cljs.core.cst$kw$format,cljs.core.cst$kw$json,cljs.core.cst$kw$response_DASH_format,cljs.core.cst$kw$json,cljs.core.cst$kw$handler,((function (items,me){
return (function (res){
var map__13978_13983 = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(res);
var map__13978_13984__$1 = ((((!((map__13978_13983 == null)))?((((map__13978_13983.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__13978_13983.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__13978_13983):map__13978_13983);
var skipped_13985 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13978_13984__$1,"skipped");
var result_13986 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13978_13984__$1,"result");
var condition_13987 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13978_13984__$1,"condition");
var target_13988 = (me["item_list_plot"]);
var skipped_items_13989 = (me["skipped_items"]);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(condition_13987,"model-has-gone")){
window.alert("model has gone, you need to restart server");
} else {
}

target_13988.splice((0),cljs.core.count(target_13988));

cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (map__13978_13983,map__13978_13984__$1,skipped_13985,result_13986,condition_13987,target_13988,skipped_items_13989,items,me){
return (function (p1__13975_SHARP_){
return target_13988.push(cljs.core.clj__GT_js(p1__13975_SHARP_));
});})(map__13978_13983,map__13978_13984__$1,skipped_13985,result_13986,condition_13987,target_13988,skipped_items_13989,items,me))
,result_13986));

skipped_items_13989.splice((0),cljs.core.count(skipped_items_13989));

cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (map__13978_13983,map__13978_13984__$1,skipped_13985,result_13986,condition_13987,target_13988,skipped_items_13989,items,me){
return (function (p1__13976_SHARP_){
return skipped_items_13989.push(cljs.core.clj__GT_js(p1__13976_SHARP_));
});})(map__13978_13983,map__13978_13984__$1,skipped_13985,result_13986,condition_13987,target_13988,skipped_items_13989,items,me))
,skipped_13985));

(me["btn_text"] = "get word position and redraw");

return (me["update_plot"]).call(null);
});})(items,me))
,cljs.core.cst$kw$error_DASH_handler,((function (items,me){
return (function (p__13980){
var map__13981 = p__13980;
var map__13981__$1 = ((((!((map__13981 == null)))?((((map__13981.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__13981.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__13981):map__13981);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13981__$1,cljs.core.cst$kw$status);
var status_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13981__$1,cljs.core.cst$kw$status_DASH_text);
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.array_seq([[cljs.core.str.cljs$core$IFn$_invoke$arity$1("error has occured "),cljs.core.str.cljs$core$IFn$_invoke$arity$1(status),cljs.core.str.cljs$core$IFn$_invoke$arity$1(":"),cljs.core.str.cljs$core$IFn$_invoke$arity$1(status_text)].join('')], 0));
});})(items,me))
], null)], 0));
})], null),cljs.core.cst$kw$mounted,(function (){
var me = this;
return $("#file-upload").change(((function (me){
return (function (e){
var file = cljs.core.first(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((e["target"]["files"])));
var reader = (new FileReader());
reader.readAsText(file);

return (reader["onload"] = ((function (file,reader,me){
return (function (){
var result = (reader["result"]);
var lines = clojure.string.split_lines(result);
return cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (result,lines,file,reader,me){
return (function (p1__13977_SHARP_){
return (me["item_list"]).push(cljs.core.clj__GT_js(clojure.string.split.cljs$core$IFn$_invoke$arity$2(p1__13977_SHARP_,/ /)));
});})(result,lines,file,reader,me))
,lines));
});})(file,reader,me))
);
});})(me))
);
})], null))));
