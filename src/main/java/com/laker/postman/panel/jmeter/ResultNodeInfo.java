package com.laker.postman.panel.jmeter;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import java.util.List;

// 结果树节点信息
public class ResultNodeInfo {
    String name;
    boolean success;
    String detail;
    PreparedRequest req;
    HttpResponse resp;
    List<com.laker.postman.model.TestResult> testResults;

    ResultNodeInfo(String name, boolean success, String detail, PreparedRequest req, HttpResponse resp) {
        this.name = name;
        this.success = success;
        this.detail = detail;
        this.req = req;
        this.resp = resp;
    }
    ResultNodeInfo(String name, boolean success, String detail, PreparedRequest req, HttpResponse resp, List<com.laker.postman.model.TestResult> testResults) {
        this.name = name;
        this.success = success;
        this.detail = detail;
        this.req = req;
        this.resp = resp;
        this.testResults = testResults;
    }

    @Override
    public String toString() {
        return name;
    }
}