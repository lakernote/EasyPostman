package com.laker.postman.model;

import java.util.ArrayList;

/**
 * JS专用请求包装类，直接操作 List
 */
public class JsRequestWrapper {
    public final PreparedRequest raw;
    public JsListWrapper<HttpHeader> headers;
    public JsListWrapper<HttpFormData> formData;
    public JsListWrapper<HttpFormUrlencoded> urlencoded;
    public String id;
    public String url;
    public String method;
    public String body;
    public boolean isMultipart;
    public boolean followRedirects;
    public boolean logEvent;

    public JsRequestWrapper(PreparedRequest req) {
        this.raw = req;

        // 确保 List 不为 null，并关联到 PreparedRequest
        if (req.headersList == null) {
            req.headersList = new ArrayList<>();
        }
        if (req.formDataList == null) {
            req.formDataList = new ArrayList<>();
        }
        if (req.urlencodedList == null) {
            req.urlencodedList = new ArrayList<>();
        }

        // 直接包装 PreparedRequest 中的 List，确保前置脚本修改能生效
        this.headers = new JsListWrapper<>(req.headersList, JsListWrapper.ListType.HEADER);
        this.formData = new JsListWrapper<>(req.formDataList, JsListWrapper.ListType.FORM_DATA);
        this.urlencoded = new JsListWrapper<>(req.urlencodedList, JsListWrapper.ListType.URLENCODED);

        this.id = req.id;
        this.url = req.url;
        this.method = req.method;
        this.body = req.body;
        this.isMultipart = req.isMultipart;
        this.followRedirects = req.followRedirects;
        this.logEvent = req.logEvent;
    }
}
