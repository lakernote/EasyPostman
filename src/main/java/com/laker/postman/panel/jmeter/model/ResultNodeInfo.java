package com.laker.postman.panel.jmeter.model;

import cn.hutool.core.util.StrUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.TestResult;

import java.util.List;

// 结果树节点信息
public class ResultNodeInfo {
    public String name;
    public boolean success;
    public String errorMsg;
    public PreparedRequest req;
    public HttpResponse resp;
    public List<TestResult> testResults;

    public ResultNodeInfo(String name, boolean success, String errorMsg, PreparedRequest req, HttpResponse resp, List<TestResult> testResults) {
        this.name = name;
        this.success = success;
        this.errorMsg = errorMsg;
        this.req = req;
        this.resp = resp;
        this.testResults = testResults;
    }

    @Override
    public String toString() {
        return name + (StrUtil.isBlank(errorMsg) ? "" : " - 错误: " + errorMsg);
    }
}