package com.laker.postman.model;

import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;

// 模拟 Postman 对象
public class Postman {
    public List<TestResult> testResults = new ArrayList<>();
    public Environment environment;
    public Environment env;
    public ResponseAssertion response;

    public Postman(Environment environment) {
        this.environment = environment;
        this.env = environment; // Postman 中 env 和 environment 是同一个对象
    }

    public void setEnvironmentVariable(String key, String value) {
        environment.addVariable(key, value);
    }

    public void setResponse(HttpResponse response) {
        this.response = new ResponseAssertion(response);
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
}