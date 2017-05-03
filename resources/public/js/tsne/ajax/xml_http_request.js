// Compiled by ClojureScript 1.9.521 {:static-fns true, :optimize-constants true}
goog.provide('ajax.xml_http_request');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('ajax.protocols');
ajax.xml_http_request.ready_state = (function ajax$xml_http_request$ready_state(e){
return new cljs.core.PersistentArrayMap(null, 5, [(0),cljs.core.cst$kw$not_DASH_initialized,(1),cljs.core.cst$kw$connection_DASH_established,(2),cljs.core.cst$kw$request_DASH_received,(3),cljs.core.cst$kw$processing_DASH_request,(4),cljs.core.cst$kw$response_DASH_ready], null).call(null,e.target.readyState);
});
XMLHttpRequest.prototype.ajax$protocols$AjaxImpl$ = cljs.core.PROTOCOL_SENTINEL;

XMLHttpRequest.prototype.ajax$protocols$AjaxImpl$_js_ajax_request$arity$3 = (function (this$,p__9982,handler){
var map__9983 = p__9982;
var map__9983__$1 = ((((!((map__9983 == null)))?((((map__9983.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__9983.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__9983):map__9983);
var uri = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__9983__$1,cljs.core.cst$kw$uri);
var method = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__9983__$1,cljs.core.cst$kw$method);
var body = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__9983__$1,cljs.core.cst$kw$body);
var headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__9983__$1,cljs.core.cst$kw$headers);
var timeout = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__9983__$1,cljs.core.cst$kw$timeout,(0));
var with_credentials = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__9983__$1,cljs.core.cst$kw$with_DASH_credentials,false);
var response_format = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__9983__$1,cljs.core.cst$kw$response_DASH_format);
var this$__$1 = this;
this$__$1.withCredentials = with_credentials;

this$__$1.onreadystatechange = ((function (this$__$1,map__9983,map__9983__$1,uri,method,body,headers,timeout,with_credentials,response_format){
return (function (p1__9981_SHARP_){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.cst$kw$response_DASH_ready,ajax.xml_http_request.ready_state(p1__9981_SHARP_))){
return (handler.cljs$core$IFn$_invoke$arity$1 ? handler.cljs$core$IFn$_invoke$arity$1(this$__$1) : handler.call(null,this$__$1));
} else {
return null;
}
});})(this$__$1,map__9983,map__9983__$1,uri,method,body,headers,timeout,with_credentials,response_format))
;

this$__$1.open(method,uri,true);

this$__$1.timeout = timeout;

var temp__4657__auto___9995 = cljs.core.cst$kw$type.cljs$core$IFn$_invoke$arity$1(response_format);
if(cljs.core.truth_(temp__4657__auto___9995)){
var response_type_9996 = temp__4657__auto___9995;
this$__$1.responseType = cljs.core.name(response_type_9996);
} else {
}

var seq__9985_9997 = cljs.core.seq(headers);
var chunk__9986_9998 = null;
var count__9987_9999 = (0);
var i__9988_10000 = (0);
while(true){
if((i__9988_10000 < count__9987_9999)){
var vec__9989_10001 = chunk__9986_9998.cljs$core$IIndexed$_nth$arity$2(null,i__9988_10000);
var k_10002 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__9989_10001,(0),null);
var v_10003 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__9989_10001,(1),null);
this$__$1.setRequestHeader(k_10002,v_10003);

var G__10004 = seq__9985_9997;
var G__10005 = chunk__9986_9998;
var G__10006 = count__9987_9999;
var G__10007 = (i__9988_10000 + (1));
seq__9985_9997 = G__10004;
chunk__9986_9998 = G__10005;
count__9987_9999 = G__10006;
i__9988_10000 = G__10007;
continue;
} else {
var temp__4657__auto___10008 = cljs.core.seq(seq__9985_9997);
if(temp__4657__auto___10008){
var seq__9985_10009__$1 = temp__4657__auto___10008;
if(cljs.core.chunked_seq_QMARK_(seq__9985_10009__$1)){
var c__7842__auto___10010 = cljs.core.chunk_first(seq__9985_10009__$1);
var G__10011 = cljs.core.chunk_rest(seq__9985_10009__$1);
var G__10012 = c__7842__auto___10010;
var G__10013 = cljs.core.count(c__7842__auto___10010);
var G__10014 = (0);
seq__9985_9997 = G__10011;
chunk__9986_9998 = G__10012;
count__9987_9999 = G__10013;
i__9988_10000 = G__10014;
continue;
} else {
var vec__9992_10015 = cljs.core.first(seq__9985_10009__$1);
var k_10016 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__9992_10015,(0),null);
var v_10017 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__9992_10015,(1),null);
this$__$1.setRequestHeader(k_10016,v_10017);

var G__10018 = cljs.core.next(seq__9985_10009__$1);
var G__10019 = null;
var G__10020 = (0);
var G__10021 = (0);
seq__9985_9997 = G__10018;
chunk__9986_9998 = G__10019;
count__9987_9999 = G__10020;
i__9988_10000 = G__10021;
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
