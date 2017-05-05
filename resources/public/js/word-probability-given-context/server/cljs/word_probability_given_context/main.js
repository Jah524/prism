// Compiled by ClojureScript 1.9.521 {:static-fns true, :optimize-constants true}
goog.provide('server.cljs.word_probability_given_context.main');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('clojure.string');
goog.require('ajax.core');
cljs.core.enable_console_print_BANG_();
server.cljs.word_probability_given_context.main.vm = (new Vue(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [cljs.core.cst$kw$el,"#main",cljs.core.cst$kw$data,new cljs.core.PersistentArrayMap(null, 7, [cljs.core.cst$kw$context,"",cljs.core.cst$kw$context_list,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$word,"",cljs.core.cst$kw$word_list,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$known_list,cljs.core.PersistentVector.EMPTY,cljs.core.cst$kw$btn_text,"go ahead",cljs.core.cst$kw$prob_list,cljs.core.PersistentVector.EMPTY], null),cljs.core.cst$kw$methods,new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$add_context,(function (item){
var me = this;
(me["context_list"]).unshift(cljs.core.clj__GT_js(clojure.string.split.cljs$core$IFn$_invoke$arity$2(item,/ /)));

return (me["context"] = "");
}),cljs.core.cst$kw$add_words,(function (item){
var me = this;
cljs.core.doall.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (me){
return (function (p1__14002_SHARP_){
return (me["word_list"]).unshift(p1__14002_SHARP_);
});})(me))
,clojure.string.split.cljs$core$IFn$_invoke$arity$2(item,/ /)));

return (me["word"] = "");
}),cljs.core.cst$kw$fetch_data,(function (){
var me = this;
var word_list = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["word_list"]));
var context_list = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((me["context_list"]));
(me["btn_text"] = "fetching data ...");

return ajax.core.POST.cljs$core$IFn$_invoke$arity$variadic("/word-probability-given-context",cljs.core.array_seq([new cljs.core.PersistentArrayMap(null, 5, [cljs.core.cst$kw$params,new cljs.core.PersistentArrayMap(null, 2, [cljs.core.cst$kw$word_DASH_list,word_list,cljs.core.cst$kw$context_DASH_list,context_list], null),cljs.core.cst$kw$format,cljs.core.cst$kw$json,cljs.core.cst$kw$response_DASH_format,cljs.core.cst$kw$json,cljs.core.cst$kw$handler,((function (word_list,context_list,me){
return (function (res){
var target = (me["prob_list"]);
var known_list = (me["known_list"]);
target.splice((0),cljs.core.count(target));

known_list.splice((0),cljs.core.count(known_list));

cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(((function (target,known_list,word_list,context_list,me){
return (function (p1__14003_SHARP_){
return known_list.push(cljs.core.clj__GT_js(p1__14003_SHARP_));
});})(target,known_list,word_list,context_list,me))
,cljs.core.get.cljs$core$IFn$_invoke$arity$2(res,"known-items"));

cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(((function (target,known_list,word_list,context_list,me){
return (function (p1__14004_SHARP_){
return target.push(cljs.core.clj__GT_js(p1__14004_SHARP_));
});})(target,known_list,word_list,context_list,me))
,cljs.core.get.cljs$core$IFn$_invoke$arity$2(res,"items"));

return (me["btn_text"] = "go ahead");
});})(word_list,context_list,me))
,cljs.core.cst$kw$error_DASH_handler,((function (word_list,context_list,me){
return (function (p__14006){
var map__14007 = p__14006;
var map__14007__$1 = ((((!((map__14007 == null)))?((((map__14007.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__14007.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__14007):map__14007);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__14007__$1,cljs.core.cst$kw$status);
var status_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__14007__$1,cljs.core.cst$kw$status_DASH_text);
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.array_seq([[cljs.core.str.cljs$core$IFn$_invoke$arity$1("error has occured "),cljs.core.str.cljs$core$IFn$_invoke$arity$1(status),cljs.core.str.cljs$core$IFn$_invoke$arity$1(":"),cljs.core.str.cljs$core$IFn$_invoke$arity$1(status_text)].join('')], 0));
});})(word_list,context_list,me))
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
return cljs.core.doall.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(((function (result,lines,file,reader,me){
return (function (p1__14005_SHARP_){
return (me["context_list"]).push(cljs.core.clj__GT_js(clojure.string.split.cljs$core$IFn$_invoke$arity$2(p1__14005_SHARP_,/ /)));
});})(result,lines,file,reader,me))
,lines));
});})(file,reader,me))
);
});})(me))
);
})], null))));
