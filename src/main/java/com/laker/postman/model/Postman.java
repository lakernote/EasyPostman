package com.laker.postman.model;

import org.graalvm.polyglot.Value;

// 模拟 Postman 对象
public class Postman {
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
                // 这里可以记录断言通过
                System.out.println("[Test] " + name + " 通过");
            } catch (Exception e) {
                // 这里可以记录断言失败
                System.err.println("[Test] " + name + " 失败: " + e.getMessage());
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