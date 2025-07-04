package com.laker.postman.panel.runner;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;

public class RunnerRowData {
    public boolean selected;
    public String name;
    public String url;
    public String method;
    public long cost;
    public String status;
    public String assertion;
    public HttpRequestItem requestItem;
    public PreparedRequest preparedRequest;
    public HttpResponse response;

    public RunnerRowData(HttpRequestItem item, PreparedRequest prepared) {
        this.selected = true;
        this.name = item.getName();
        this.url = item.getUrl();
        this.method = item.getMethod();
        this.cost = 0;
        this.status = "";
        this.assertion = "";
        this.requestItem = item;
        this.preparedRequest = prepared;
        this.response = null;
    }
}