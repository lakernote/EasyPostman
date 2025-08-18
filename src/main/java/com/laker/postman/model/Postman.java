package com.laker.postman.model;

import com.laker.postman.service.EnvironmentService;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Postman {
    public List<TestResult> testResults = new ArrayList<>();
    public Environment environment; // Postman 中 env 和 environment 是同一个对象，保持一致性
    public Environment env; // Postman 中 env 和 environment 是同一个对象，保持一致性
    public ResponseAssertion response; // Postman 中 pm.response 对应的对象
    public PostmanVariables variables = new PostmanVariables(); // 局部变量 Postman 中 pm.variables 对应的对象
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


    /**
     * 设置一个请求变量，仅在当前请求有效
     */
    public void setVariable(String key, Object value) {
        variables.set(key, value != null ? value.toString() : null);
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
        return variables.has(key);
    }

    /**
     * 删除请求变量
     */
    public void unsetVariable(String key) {
        variables.unset(key);
    }

    /**
     * 清除所有请求变量
     */
    public void clearVariables() {
        variables.clear();
    }

    // 内部类，用于支持 pm.variables.set() 语法
    public static class PostmanVariables {
        /**
         * 设置局部变量 - 对应 pm.variables.set()
         * 支持多种数据类型
         */
        public void set(String key, Object value) {
            if (value != null) {
                EnvironmentService.setTemporaryVariable(key, String.valueOf(value));
            } else {
                EnvironmentService.setTemporaryVariable(key, null);
            }
        }

        /**
         * 重载方法，保持与原有 String 参数的兼容性
         */
        public void set(String key, String value) {
            EnvironmentService.setTemporaryVariable(key, value);
        }

        /**
         * 获取局部变量 - 对应 pm.variables.get()
         */
        public String get(String key) {
            return EnvironmentService.getTemporaryVariable(key);
        }

        /**
         * 检查局部变量是否存在 - 对应 pm.variables.has()
         */
        public boolean has(String key) {
            return EnvironmentService.getTemporaryVariable(key) != null;
        }

        /**
         * 删除局部变量 - 对应 pm.variables.unset()
         */
        public void unset(String key) {
            EnvironmentService.setTemporaryVariable(key, null);
        }

        /**
         * 清除所有局部变量 - 对应 pm.variables.clear()
         */
        public void clear() {
            EnvironmentService.clearTemporaryVariables();
        }
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