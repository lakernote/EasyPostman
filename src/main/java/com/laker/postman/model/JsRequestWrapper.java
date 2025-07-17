package com.laker.postman.model;

import com.laker.postman.service.http.HttpRequestUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JS专用请求包装类，所有参数均支持 add 方法
 */
public class JsRequestWrapper {
    public final PreparedRequest raw;
    public ParamMapWrapper headers;
    public ParamMapWrapper formData;
    public ParamMapWrapper formFiles;
    public ParamMapWrapper urlencoded;
    public ParamMapWrapper params;
    public String id;
    public String url;
    public String method;
    public String body;
    public boolean isMultipart;
    public boolean followRedirects;
    public boolean logEvent;

    public JsRequestWrapper(PreparedRequest req) {
        this.raw = req;
        this.headers = new ParamMapWrapper(req.headers != null ? req.headers : new LinkedHashMap<>());
        this.formData = new ParamMapWrapper(req.formData != null ? req.formData : new LinkedHashMap<>());
        this.formFiles = new ParamMapWrapper(req.formFiles != null ? req.formFiles : new LinkedHashMap<>());
        this.urlencoded = new ParamMapWrapper(req.urlencoded != null ? req.urlencoded : new LinkedHashMap<>());
        this.params = new ParamMapWrapper(new LinkedHashMap<>(), this); // params自动同步url
        this.id = req.id;
        this.url = req.url;
        this.method = req.method;
        this.body = req.body;
        this.isMultipart = req.isMultipart;
        this.followRedirects = req.followRedirects;
        this.logEvent = req.logEvent;
    }

    // params变更时自动更新url
    public void updateUrlWithParams(Map<String, String> params) {
        this.url = HttpRequestUtil.buildUrlWithParams(this.url, params);
        raw.url = this.url;
    }

    // urlencoded变更时自动更新body
    public void updateBodyWithUrlencoded() {
        if (this.urlencoded != null && !this.urlencoded.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : this.urlencoded.entrySet()) {
                if (!sb.isEmpty()) sb.append("&");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            this.body = sb.toString();
            raw.body = this.body;
        } else {
            this.body = "";
            raw.body = "";
        }
    }
}
