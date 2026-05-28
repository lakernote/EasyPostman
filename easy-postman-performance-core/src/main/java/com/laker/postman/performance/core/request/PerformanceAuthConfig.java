package com.laker.postman.performance.core.request;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class PerformanceAuthConfig {
    PerformanceAuthType type;
    String username;
    String password;
    String token;

    public PerformanceAuthConfig(PerformanceAuthType type, String username, String password, String token) {
        this.type = type == null ? PerformanceAuthType.INHERIT : type;
        this.username = blankToEmpty(username);
        this.password = blankToEmpty(password);
        this.token = blankToEmpty(token);
    }

    public static PerformanceAuthConfig inherit() {
        return builder().type(PerformanceAuthType.INHERIT).build();
    }

    public static PerformanceAuthConfig basic(String username, String password) {
        return builder()
                .type(PerformanceAuthType.BASIC)
                .username(username)
                .password(password)
                .build();
    }

    public static PerformanceAuthConfig bearer(String token) {
        return builder()
                .type(PerformanceAuthType.BEARER)
                .token(token)
                .build();
    }

    public static PerformanceAuthConfig fromSnapshotFields(PerformanceAuthType authType,
                                                           String username,
                                                           String password,
                                                           String token) {
        return builder()
                .type(authType)
                .username(username)
                .password(password)
                .token(token)
                .build();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
