// Compiled by ClojureScript 1.9.521 {:static-fns true, :optimize-constants true}
goog.provide('server.cljs.tsne.tsne');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('clojure.string');
goog.require('ajax.core');
cljs.core.enable_console_print_BANG_();
server.cljs.tsne.tsne.text_embedding__GT_plotly_data = (function server$cljs$tsne$tsne$text_embedding__GT_plotly_data(p__10623){
var map__10626 = p__10623;
var map__10626__$1 = ((((!((map__10626 == null)))?((((map__10626.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__10626.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__10626):map__10626);
var item = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10626__$1,"item");
var x = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10626__$1,"x");
var y = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10626__$1,"y");
return new cljs.core.PersistentArrayMap(null, 8, [cljs.core.cst$kw$x,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [x], null),cljs.core.cst$kw$y,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [y], null),cljs.core.cst$kw$text,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [item], null),cljs.core.cst$kw$hoverinfo,"none",cljs.core.cst$kw$showlegend,false,cljs.core.cst$kw$type,"scater",cljs.core.cst$kw$mode,"markers+text",cljs.core.cst$kw$textposition,"bottom"], null);
});
server.cljs.tsne.tsne.vm = (new Vue(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [cljs.core.cst$kw$el,"#main",cljs.core.cst$kw$data,new cljs.core.PersistentArrayMap(null, 7, [cljs.core.cst$kw$btn_text,"get word position and redraw",cljs.core.cst$kw$perplexity,5.0,cljs.core.cst$kw$iters,(1000),cljs.core.cst$kw$item,"",cljs.core.cst$kw$item_list,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$item_list_plot,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$skipped_items,cljs.core.PersistentVector.EMPTY], null),cljs.core.cst$kw$methods,new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$add_item,(function (item){
var me = this;
(me["item_list"]).push(cljs.core.clj__GT_js(clojure.string.split.cljs$core$IFn$_invoke$arity$2(item,/ /)));

return (me["item"] = "");
}),cljs.core.cst$kw$update_plot,(function (){
var me = this;
return Plotly.newPlot(document.getElementById("tsne-plot"),cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(((function (me){
return (function (p1__10628_SHARP_){
return server.cljs.tsne.tsne.text_embedding__GT_plotly_data(p1__10628_SHARP_);
});})(me))
,cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["item_list_plot"])))),cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$height,(640)], null)));
}),cljs.core.cst$kw$fetch_data,(function (){
var me = this;
var items = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["item_list"]));
(me["btn_text"] = "fetching data ...");

return ajax.core.POST.cljs$core$IFn$_invoke$arity$variadic("/",cljs.core.array_seq([new cljs.core.PersistentArrayMap(null, 5, [cljs.core.cst$kw$params,new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$items,items,cljs.core.cst$kw$perplexity,(me["perplexity"]),cljs.core.cst$kw$iters,(me["iters"])], null),cljs.core.cst$kw$format,cljs.core.cst$kw$json,cljs.core.cst$kw$response_DASH_format,cljs.core.cst$kw$json,cljs.core.cst$kw$handler,((function (items,me){
return (function (res){
var map__10632_10637 = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(res);
var map__10632_10638__$1 = ((((!((map__10632_10637 == null)))?((((map__10632_10637.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__10632_10637.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__10632_10637):map__10632_10637);
var skipped_10639 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10632_10638__$1,"skipped");
var result_10640 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10632_10638__$1,"result");
var condition_10641 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10632_10638__$1,"condition");
var target_10642 = (me["item_list_plot"]);
var skipped_items_10643 = (me["skipped_items"]);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(condition_10641,"model-has-gone")){
window.alert("model has gone, you need to restart server");
} else {
}

target_10642.splice((0),cljs.core.count(target_10642));

cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (map__10632_10637,map__10632_10638__$1,skipped_10639,result_10640,condition_10641,target_10642,skipped_items_10643,items,me){
return (function (p1__10629_SHARP_){
return target_10642.push(cljs.core.clj__GT_js(p1__10629_SHARP_));
});})(map__10632_10637,map__10632_10638__$1,skipped_10639,result_10640,condition_10641,target_10642,skipped_items_10643,items,me))
,result_10640));

skipped_items_10643.splice((0),cljs.core.count(skipped_items_10643));

cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (map__10632_10637,map__10632_10638__$1,skipped_10639,result_10640,condition_10641,target_10642,skipped_items_10643,items,me){
return (function (p1__10630_SHARP_){
return skipped_items_10643.push(cljs.core.clj__GT_js(p1__10630_SHARP_));
});})(map__10632_10637,map__10632_10638__$1,skipped_10639,result_10640,condition_10641,target_10642,skipped_items_10643,items,me))
,skipped_10639));

(me["btn_text"] = "get word position and redraw");

return (me["update_plot"]).call(null);
});})(items,me))
,cljs.core.cst$kw$error_DASH_handler,((function (items,me){
return (function (p__10634){
var map__10635 = p__10634;
var map__10635__$1 = ((((!((map__10635 == null)))?((((map__10635.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__10635.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__10635):map__10635);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10635__$1,cljs.core.cst$kw$status);
var status_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10635__$1,cljs.core.cst$kw$status_DASH_text);
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
return (function (p1__10631_SHARP_){
return (me["item_list"]).push(cljs.core.clj__GT_js(clojure.string.split.cljs$core$IFn$_invoke$arity$2(p1__10631_SHARP_,/ /)));
});})(result,lines,file,reader,me))
,lines));
});})(file,reader,me))
);
});})(me))
);
})], null))));
