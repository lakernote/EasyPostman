package com.laker.postman.model;

import org.graalvm.polyglot.Value;

import java.util.*;

// 模拟 Postman 对象
public class Postman {
    public List<TestResult> testResults = new ArrayList<>();
    public Environment environment;
    public Environment env;
    public ResponseAssertion response;
    public Map<String, Object> variables = new HashMap<>();
    public JsRequestWrapper request;

    public Postman(Environment environment) {
        this.environment = environment;
        this.env = environment; // Postman 中 env 和 environment 是同一个对象
    }

    public void setEnvironmentVariable(String key, String value) {
        environment.addVariable(key, value);
    }

    /**
     * 重载setEnvironmentVariable方法，支持Object类型参数
     * 解决JavaScript中传入数字等非String类型的问题
     */
    public void setEnvironmentVariable(String key, Object value) {
        if (value != null) {
            environment.set(key, String.valueOf(value));
        }
    }

    public void setResponse(HttpResponse response) {
        this.response = new ResponseAssertion(response);
    }

    public void setRequest(PreparedRequest request) {
        this.request = new JsRequestWrapper(request);
    }

    // Postman 脚本中的 pm.test(name, fn)
    public void test(String name, Value fn) {
        if (this.response == null) {
            throw new IllegalStateException("pm.response 为空，请确保在调用 pm.test 前已设置 response");
        }
        if (fn != null && fn.canExecute()) {
            try {
                fn.executeVoid();
                testResults.add(new TestResult(name, true, null));
            } catch (Exception e) {
                testResults.add(new TestResult(name, false, e.getMessage()));
                throw e;
            }
        }
    }

    // pm.expect 断言入口
    public Expectation expect(Object actual) {
        if (this.response != null) {
            return this.response.expect(actual);
        }
        return new Expectation(actual);
    }

    // 为前置脚本添加的方法

    /**
     * 设置一个请求变量，仅在当前请求有效
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取请求变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 检查请求变量是否存在
     */
    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }

    /**
     * 删除请求变量
     */
    public void unsetVariable(String key) {
        variables.remove(key);
    }

    /**
     * 清除所有请求变量
     */
    public void clearVariables() {
        variables.clear();
    }

    /**
     * 生成UUID
     */
    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成时间戳
     */
    public long getTimestamp() {
        return System.currentTimeMillis();
    }
}