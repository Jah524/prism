// Compiled by ClojureScript 1.9.521 {:static-fns true, :optimize-constants true}
goog.provide('server.cljs.tsne.tsne');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('ajax.core');
cljs.core.enable_console_print_BANG_();
server.cljs.tsne.tsne.text_embedding__GT_plotly_data = (function server$cljs$tsne$tsne$text_embedding__GT_plotly_data(p__11284){
var map__11287 = p__11284;
var map__11287__$1 = ((((!((map__11287 == null)))?((((map__11287.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__11287.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__11287):map__11287);
var item = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11287__$1,"item");
var x = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11287__$1,"x");
var y = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11287__$1,"y");
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.array_seq([item,x,y], 0));

return new cljs.core.PersistentArrayMap(null, 8, [cljs.core.cst$kw$x,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [x], null),cljs.core.cst$kw$y,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [y], null),cljs.core.cst$kw$text,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [item], null),cljs.core.cst$kw$hoverinfo,"none",cljs.core.cst$kw$showlegend,false,cljs.core.cst$kw$type,"scater",cljs.core.cst$kw$mode,"markers+text",cljs.core.cst$kw$textposition,"bottom"], null);
});
server.cljs.tsne.tsne.sample_word_list = cljs.core.PersistentVector.fromArray([new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"I"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"you"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"happy"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"bad"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"a"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"an"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"and"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"or"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"what"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"where"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"god"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"did"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"much"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"beautiful"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"never"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"."], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"!"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"oh"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"green"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"blue"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"red"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"matter"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"home"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"slightly"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"end"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"he"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"she"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"newspaper"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"more"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"when"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"dream"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"default"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"international"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"desk"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"chair"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"world"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"new"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"run"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"get"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"use"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"luck"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"money"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"year"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"very"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"less"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"computer"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"the"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"morning"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"yesterday"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"clock"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"last"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"worth"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"children"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"music"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"sports"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"ball"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"math"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"English"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"IT"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"it"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"they"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"so"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"sky"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"auto"], null),new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,"car"], null)], true);
server.cljs.tsne.tsne.word_set = (function (){var G__11289 = cljs.core.PersistentHashSet.EMPTY;
return (cljs.core.atom.cljs$core$IFn$_invoke$arity$1 ? cljs.core.atom.cljs$core$IFn$_invoke$arity$1(G__11289) : cljs.core.atom.call(null,G__11289));
})();
server.cljs.tsne.tsne.vm = (new Vue(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [cljs.core.cst$kw$el,"#items",cljs.core.cst$kw$data,new cljs.core.PersistentArrayMap(null, 8, [cljs.core.cst$kw$test,"\u3042\u3070\u3070",cljs.core.cst$kw$plot_area,document.getElementById("tsne-plot"),cljs.core.cst$kw$perplexity,5.0,cljs.core.cst$kw$iters,(1000),cljs.core.cst$kw$item,"",cljs.core.cst$kw$item_list,cljs.core.clj__GT_js(server.cljs.tsne.tsne.sample_word_list),cljs.core.cst$kw$item_list_plot,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$skipped_items,cljs.core.PersistentVector.EMPTY], null),cljs.core.cst$kw$methods,new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$add_item,(function (item){
var me = this;
(me["item_list"]).push(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$item,item], null)));

return (me["item"] = "");
}),cljs.core.cst$kw$update_plot,(function (){
var me = this;
return Plotly.newPlot((me["plot_area"]),cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(((function (me){
return (function (p1__11290_SHARP_){
return server.cljs.tsne.tsne.text_embedding__GT_plotly_data(p1__11290_SHARP_);
});})(me))
,cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["item_list_plot"])))),cljs.core.clj__GT_js(cljs.core.PersistentArrayMap.EMPTY));
}),cljs.core.cst$kw$fetch_data,(function (){
var me = this;
var items = cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (me){
return (function (p1__11291_SHARP_){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(p1__11291_SHARP_,"item");
});})(me))
,cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["item_list"]))));
return ajax.core.POST.cljs$core$IFn$_invoke$arity$variadic("/",cljs.core.array_seq([new cljs.core.PersistentArrayMap(null, 5, [cljs.core.cst$kw$params,new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$items,items,cljs.core.cst$kw$perplexity,(me["perplexity"]),cljs.core.cst$kw$iters,(me["iters"])], null),cljs.core.cst$kw$format,cljs.core.cst$kw$json,cljs.core.cst$kw$response_DASH_format,cljs.core.cst$kw$json,cljs.core.cst$kw$handler,((function (items,me){
return (function (res){
var map__11294_11299 = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(res);
var map__11294_11300__$1 = ((((!((map__11294_11299 == null)))?((((map__11294_11299.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__11294_11299.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__11294_11299):map__11294_11299);
var skipped_11301 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11294_11300__$1,"skipped");
var result_11302 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11294_11300__$1,"result");
var target_11303 = (me["item_list_plot"]);
var skipped_items_11304 = (me["skipped_items"]);
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.array_seq([result_11302], 0));

target_11303.splice((0),cljs.core.count(target_11303));

cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (map__11294_11299,map__11294_11300__$1,skipped_11301,result_11302,target_11303,skipped_items_11304,items,me){
return (function (p1__11292_SHARP_){
return target_11303.push(cljs.core.clj__GT_js(p1__11292_SHARP_));
});})(map__11294_11299,map__11294_11300__$1,skipped_11301,result_11302,target_11303,skipped_items_11304,items,me))
,result_11302));

skipped_items_11304.splice((0),cljs.core.count(skipped_items_11304));

cljs.core.dorun.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (map__11294_11299,map__11294_11300__$1,skipped_11301,result_11302,target_11303,skipped_items_11304,items,me){
return (function (p1__11293_SHARP_){
return skipped_items_11304.push(cljs.core.clj__GT_js(p1__11293_SHARP_));
});})(map__11294_11299,map__11294_11300__$1,skipped_11301,result_11302,target_11303,skipped_items_11304,items,me))
,skipped_11301));

return (me["update_plot"]).call(null);
});})(items,me))
,cljs.core.cst$kw$error_DASH_handler,((function (items,me){
return (function (p__11296){
var map__11297 = p__11296;
var map__11297__$1 = ((((!((map__11297 == null)))?((((map__11297.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__11297.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__11297):map__11297);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11297__$1,cljs.core.cst$kw$status);
var status_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11297__$1,cljs.core.cst$kw$status_DASH_text);
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.array_seq([[cljs.core.str.cljs$core$IFn$_invoke$arity$1("error has occured "),cljs.core.str.cljs$core$IFn$_invoke$arity$1(status),cljs.core.str.cljs$core$IFn$_invoke$arity$1(":"),cljs.core.str.cljs$core$IFn$_invoke$arity$1(status_text)].join('')], 0));
});})(items,me))
], null)], 0));
})], null),cljs.core.cst$kw$mounted,(function (){
return null;
})], null))));
