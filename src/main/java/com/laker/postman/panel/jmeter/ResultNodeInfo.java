package com.laker.postman.panel.jmeter;

import cn.hutool.core.util.StrUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.TestResult;

import java.util.List;

// 结果树节点信息
public class ResultNodeInfo {
    String name;
    boolean success;
    String errorMsg;
    PreparedRequest req;
    HttpResponse resp;
    List<TestResult> testResults;

    ResultNodeInfo(String name, boolean success, String errorMsg, PreparedRequest req, HttpResponse resp, List<TestResult> testResults) {
        this.name = name;
        this.success = success;
        this.errorMsg = errorMsg;
        this.req = req;
        this.resp = resp;
        this.testResults = testResults;
    }

    @Override
    public String toString() {
        return name + " - " + (success ? "成功" : "失败") + (StrUtil.isBlank(errorMsg) ? "" : " - 错误: " + errorMsg);
    }
}