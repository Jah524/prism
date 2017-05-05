// Compiled by ClojureScript 1.9.521 {:static-fns true, :optimize-constants true}
goog.provide('ajax.xml_http_request');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('ajax.protocols');
ajax.xml_http_request.ready_state = (function ajax$xml_http_request$ready_state(e){
return new cljs.core.PersistentArrayMap(null, 5, [(0),cljs.core.cst$kw$not_DASH_initialized,(1),cljs.core.cst$kw$connection_DASH_established,(2),cljs.core.cst$kw$request_DASH_received,(3),cljs.core.cst$kw$processing_DASH_request,(4),cljs.core.cst$kw$response_DASH_ready], null).call(null,e.target.readyState);
});
XMLHttpRequest.prototype.ajax$protocols$AjaxImpl$ = cljs.core.PROTOCOL_SENTINEL;

XMLHttpRequest.prototype.ajax$protocols$AjaxImpl$_js_ajax_request$arity$3 = (function (this$,p__13185,handler){
var map__13186 = p__13185;
var map__13186__$1 = ((((!((map__13186 == null)))?((((map__13186.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__13186.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__13186):map__13186);
var uri = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13186__$1,cljs.core.cst$kw$uri);
var method = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13186__$1,cljs.core.cst$kw$method);
var body = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13186__$1,cljs.core.cst$kw$body);
var headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13186__$1,cljs.core.cst$kw$headers);
var timeout = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__13186__$1,cljs.core.cst$kw$timeout,(0));
var with_credentials = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__13186__$1,cljs.core.cst$kw$with_DASH_credentials,false);
var response_format = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13186__$1,cljs.core.cst$kw$response_DASH_format);
var this$__$1 = this;
this$__$1.withCredentials = with_credentials;

this$__$1.onreadystatechange = ((function (this$__$1,map__13186,map__13186__$1,uri,method,body,headers,timeout,with_credentials,response_format){
return (function (p1__13184_SHARP_){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.cst$kw$response_DASH_ready,ajax.xml_http_request.ready_state(p1__13184_SHARP_))){
return (handler.cljs$core$IFn$_invoke$arity$1 ? handler.cljs$core$IFn$_invoke$arity$1(this$__$1) : handler.call(null,this$__$1));
} else {
return null;
}
});})(this$__$1,map__13186,map__13186__$1,uri,method,body,headers,timeout,with_credentials,response_format))
;

this$__$1.open(method,uri,true);

this$__$1.timeout = timeout;

var temp__4657__auto___13198 = cljs.core.cst$kw$type.cljs$core$IFn$_invoke$arity$1(response_format);
if(cljs.core.truth_(temp__4657__auto___13198)){
var response_type_13199 = temp__4657__auto___13198;
this$__$1.responseType = cljs.core.name(response_type_13199);
} else {
}

var seq__13188_13200 = cljs.core.seq(headers);
var chunk__13189_13201 = null;
var count__13190_13202 = (0);
var i__13191_13203 = (0);
while(true){
if((i__13191_13203 < count__13190_13202)){
var vec__13192_13204 = chunk__13189_13201.cljs$core$IIndexed$_nth$arity$2(null,i__13191_13203);
var k_13205 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13192_13204,(0),null);
var v_13206 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13192_13204,(1),null);
this$__$1.setRequestHeader(k_13205,v_13206);

var G__13207 = seq__13188_13200;
var G__13208 = chunk__13189_13201;
var G__13209 = count__13190_13202;
var G__13210 = (i__13191_13203 + (1));
seq__13188_13200 = G__13207;
chunk__13189_13201 = G__13208;
count__13190_13202 = G__13209;
i__13191_13203 = G__13210;
continue;
} else {
var temp__4657__auto___13211 = cljs.core.seq(seq__13188_13200);
if(temp__4657__auto___13211){
var seq__13188_13212__$1 = temp__4657__auto___13211;
if(cljs.core.chunked_seq_QMARK_(seq__13188_13212__$1)){
var c__7842__auto___13213 = cljs.core.chunk_first(seq__13188_13212__$1);
var G__13214 = cljs.core.chunk_rest(seq__13188_13212__$1);
var G__13215 = c__7842__auto___13213;
var G__13216 = cljs.core.count(c__7842__auto___13213);
var G__13217 = (0);
seq__13188_13200 = G__13214;
chunk__13189_13201 = G__13215;
count__13190_13202 = G__13216;
i__13191_13203 = G__13217;
continue;
} else {
var vec__13195_13218 = cljs.core.first(seq__13188_13212__$1);
var k_13219 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13195_13218,(0),null);
var v_13220 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13195_13218,(1),null);
this$__$1.setRequestHeader(k_13219,v_13220);

var G__13221 = cljs.core.next(seq__13188_13212__$1);
var G__13222 = null;
var G__13223 = (0);
var G__13224 = (0);
seq__13188_13200 = G__13221;
chunk__13189_13201 = G__13222;
count__13190_13202 = G__13223;
i__13191_13203 = G__13224;
continue;
}
} else {
}
}
break;
}

this$__$1.send((function (){var or__7023__auto__ = body;
if(cljs.core.truth_(or__7023__auto__)){
return or__7023__auto__;
} else {
return "";
}
})());

return this$__$1;
});

XMLHttpRequest.prototype.ajax$protocols$AjaxRequest$ = cljs.core.PROTOCOL_SENTINEL;

XMLHttpRequest.prototype.ajax$protocols$AjaxRequest$_abort$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1.abort();
});

XMLHttpRequest.prototype.ajax$protocols$AjaxResponse$ = cljs.core.PROTOCOL_SENTINEL;

XMLHttpRequest.prototype.ajax$protocols$AjaxResponse$_body$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1.response;
});

XMLHttpRequest.prototype.ajax$protocols$AjaxResponse$_status$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1.status;
});

XMLHttpRequest.prototype.ajax$protocols$AjaxResponse$_status_text$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1.statusText;
});

XMLHttpRequest.prototype.ajax$protocols$AjaxResponse$_get_response_header$arity$2 = (function (this$,header){
var this$__$1 = this;
return this$__$1.getResponseHeader(header);
});

XMLHttpRequest.prototype.ajax$protocols$AjaxResponse$_was_aborted$arity$1 = (function (this$){
var this$__$1 = this;
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((0),this$__$1.readyState);
});
