package com.laker.postman.functional.model;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.script.model.TestResult;

import java.util.List;

public class RunnerRowData {
    public boolean selected;
    public String name;
    public String url;
    public String method;
    public long cost;
    public String status;
    public AssertionResult assertion;
    public HttpRequestItem requestItem;
    public PreparedRequest preparedRequest;
    public HttpResponse response;
    public List<TestResult> testResults;

    public RunnerRowData(HttpRequestItem item, PreparedRequest prepared) {
        this.selected = true;
        this.name = item.getName();
        this.url = item.getUrl();
        this.method = item.getMethod();
        this.cost = 0;
        this.status = "";
        this.assertion = AssertionResult.NO_TESTS;
        this.requestItem = item;
        this.preparedRequest = prepared;
        this.response = null;
        this.testResults = null;
    }
}
