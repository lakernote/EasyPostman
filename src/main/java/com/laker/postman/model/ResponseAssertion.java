package com.laker.postman.model;

import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.Map;

/**
 * 支持 Postman 脚本链式断言的响应断言对象
 */
public class ResponseAssertion {
    private HttpResponse response;
    public ResponseAssertion to = this;
    public ResponseAssertion have = this;
    public ResponseAssertion be = this;

    public ResponseAssertion(HttpResponse response) {
        this.response = response;
    }

    public ResponseAssertion to() {
        return this;
    }

    // pm.response.to.have.status(200)
    public void status(int code) {
        if (response == null || response.code != code) {
            throw new AssertionError("响应状态码断言失败: 期望=" + code + ", 实际=" + (response == null ? null : response.code));
        }
    }

    // pm.response.to.have.header('Content-Type')
    public void header(String name) {
        if (response == null || response.headers == null) {
            throw new AssertionError("响应头不存在");
        }
        boolean found = false;
        for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AssertionError("响应头不存在: " + name);
        }
    }

    // pm.expect(pm.response.responseTime).to.be.below(1000)
    public void below(long ms) {
        if (response == null || response.costMs >= ms) {
            throw new AssertionError("响应耗时断言失败: 期望小于" + ms + ", 实际=" + (response == null ? null : response.costMs));
        }
    }

    // 获取响应体文本
    public String text() {
        return response != null ? response.body : null;
    }

    // 获取响应体 JSON
    public Object json() {
        try {
            if (response != null && response.body != null) {
                return JSONUtil.parse(response.body);
            }
        } catch (Exception e) {
            throw new AssertionError("响应体不是有效的JSON: " + e.getMessage());
        }
        return null;
    }

    // 获取响应时间
    public long responseTime() {
        return response != null ? response.costMs : -1;
    }

    // pm.expect 断言入口
    public Expectation expect(Object actual) {
        return new Expectation(actual);
    }
}