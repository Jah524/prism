// Compiled by ClojureScript 1.9.521 {:static-fns true, :optimize-constants true}
goog.provide('server.cljs.tsne.tsne');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('clojure.string');
goog.require('ajax.core');
cljs.core.enable_console_print_BANG_();
server.cljs.tsne.tsne.text_embedding__GT_plotly_data = (function server$cljs$tsne$tsne$text_embedding__GT_plotly_data(p__11749){
var map__11752 = p__11749;
var map__11752__$1 = ((((!((map__11752 == null)))?((((map__11752.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__11752.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__11752):map__11752);
var item = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11752__$1,"item");
var x = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11752__$1,"x");
var y = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11752__$1,"y");
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.array_seq([item,x,y], 0));

return new cljs.core.PersistentArrayMap(null, 8, [cljs.core.cst$kw$x,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [x], null),cljs.core.cst$kw$y,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [y], null),cljs.core.cst$kw$text,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [item], null),cljs.core.cst$kw$hoverinfo,"none",cljs.core.cst$kw$showlegend,false,cljs.core.cst$kw$type,"scater",cljs.core.cst$kw$mode,"markers+text",cljs.core.cst$kw$textposition,"bottom"], null);
});
server.cljs.tsne.tsne.word_set = (function (){var G__11754 = cljs.core.PersistentHashSet.EMPTY;
return (cljs.core.atom.cljs$core$IFn$_invoke$arity$1 ? cljs.core.atom.cljs$core$IFn$_invoke$arity$1(G__11754) : cljs.core.atom.call(null,G__11754));
})();
server.cljs.tsne.tsne.vm = (new Vue(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [cljs.core.cst$kw$el,"#items",cljs.core.cst$kw$data,new cljs.core.PersistentArrayMap(null, 8, [cljs.core.cst$kw$test,"\u3042\u3070\u3070",cljs.core.cst$kw$plot_area,document.getElementById("tsne-plot"),cljs.core.cst$kw$perplexity,5.0,cljs.core.cst$kw$iters,(1000),cljs.core.cst$kw$item,"",cljs.core.cst$kw$item_list,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$item_list_plot,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$skipped_items,cljs.core.PersistentVector.EMPTY], null),cljs.core.cst$kw$methods,new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$add_item,(function (item){
var me = this;
(me["item_list"]).push(item);

return (me["item"] = "");
}),cljs.core.cst$kw$update_plot,(function (){
var me = this;
return Plotly.newPlot((me["plot_area"]),cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(((function (me){
return (function (p1__11755_SHARP_){
return server.cljs.tsne.tsne.text_embedding__GT_plotly_data(p1__11755_SHARP_);
});})(me))
,cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["item_list_plot"])))),cljs.core.clj__GT_js(cljs.core.PersistentArrayMap.EMPTY));
}),cljs.core.cst$kw$fetch_data,(function (){
var me = this;
var items = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["item_list"]));
return ajax.core.POST.cljs$core$IFn$_invoke$arity$variadic("/",cljs.core.array_seq([new cljs.core.PersistentArrayMap(null, 5, [cljs.core.cst$kw$params,new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$items,items,cljs.core.cst$kw$perplexity,(me["perplexity"]),cljs.core.cst$kw$iters,(me["iters"])], null),cljs.core.cst$kw$format,cljs.core.cst$kw$json,cljs.core.cst$kw$response_DASH_format,cljs.core.cst$kw$json,cljs.core.cst$kw$handler,((function (items,me){
return (function (res){
var map__11759_11764 = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(res);
var map__11759_11765__$1 = ((((!((map__11759_11764 == null)))?((((map__11759_11764.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__11759_11764.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__11759_11764):map__11759_11764);
var skipped_11766 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11759_11765__$1,"skipped");
var result_11767 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11759_11765__$1,"result");
var condition_11768 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11759_11765__$1,"condition");
var target_11769 = (me["item_list_plot"]);
var skipped_items_11770 = (me["skipped_items"]);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(condition_11768,"model-has-gone")){
window.alert("model has gone, you need to restart server");
} else {
}

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.array_seq([result_11767], 0));

target_11769.splice((0),cljs.core.count(target_11769));

cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (map__11759_11764,map__11759_11765__$1,skipped_11766,result_11767,condition_11768,target_11769,skipped_items_11770,items,me){
return (function (p1__11756_SHARP_){
return target_11769.push(cljs.core.clj__GT_js(p1__11756_SHARP_));
});})(map__11759_11764,map__11759_11765__$1,skipped_11766,result_11767,condition_11768,target_11769,skipped_items_11770,items,me))
,result_11767));

skipped_items_11770.splice((0),cljs.core.count(skipped_items_11770));

cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (map__11759_11764,map__11759_11765__$1,skipped_11766,result_11767,condition_11768,target_11769,skipped_items_11770,items,me){
return (function (p1__11757_SHARP_){
return skipped_items_11770.push(cljs.core.clj__GT_js(p1__11757_SHARP_));
});})(map__11759_11764,map__11759_11765__$1,skipped_11766,result_11767,condition_11768,target_11769,skipped_items_11770,items,me))
,skipped_11766));

return (me["update_plot"]).call(null);
});})(items,me))
,cljs.core.cst$kw$error_DASH_handler,((function (items,me){
return (function (p__11761){
var map__11762 = p__11761;
var map__11762__$1 = ((((!((map__11762 == null)))?((((map__11762.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__11762.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__11762):map__11762);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11762__$1,cljs.core.cst$kw$status);
var status_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11762__$1,cljs.core.cst$kw$status_DASH_text);
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.array_seq([[cljs.core.str.cljs$core$IFn$_invoke$arity$1("error has occured "),cljs.core.str.cljs$core$IFn$_invoke$arity$1(status),cljs.core.str.cljs$core$IFn$_invoke$arity$1(":"),cljs.core.str.cljs$core$IFn$_invoke$arity$1(status_text)].join('')], 0));
});})(items,me))
], null)], 0));
})], null),cljs.core.cst$kw$mounted,(function (){
var me = this;
return $("#file-upload").change(((function (me){
return (function (e){
var file = cljs.core.first(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((e["target"]["files"])));
var reader = (new FileReader());
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.array_seq([cljs.core.clj__GT_js(file)], 0));

reader.readAsText(file);

return (reader["onload"] = ((function (file,reader,me){
return (function (){
var result = (reader["result"]);
var lines = clojure.string.split_lines(result);
return cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (result,lines,file,reader,me){
return (function (p1__11758_SHARP_){
return (me["item_list"]).push(p1__11758_SHARP_);
});})(result,lines,file,reader,me))
,lines));
});})(file,reader,me))
);
});})(me))
);
})], null))));
