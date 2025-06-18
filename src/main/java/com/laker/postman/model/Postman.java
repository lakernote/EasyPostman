package com.laker.postman.model;

// 模拟 Postman 对象
public class Postman {
    public final Environment environment;
    public final Environment env;

    public Postman(Environment environment) {
        this.environment = environment;
        this.env = environment; // Postman 中 env 和 environment 是同一个对象
    }

    public void setEnvironmentVariable(String key, String value) {
        environment.addVariable(key, value);
    }
}