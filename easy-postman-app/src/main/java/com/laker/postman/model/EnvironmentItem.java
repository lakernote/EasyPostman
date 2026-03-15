package com.laker.postman.model;

import lombok.Getter;

import java.util.Objects;

/**
 * 环境项封装类，用于在下拉框中显示
 */
@Getter
public class EnvironmentItem {
    private final Environment environment;

    public EnvironmentItem(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EnvironmentItem that)) return false;
        return Objects.equals(getEnvironment().getId(), that.getEnvironment().getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(environment.getId());
    }

    @Override
    public String toString() {
        return environment.getName();
    }


}