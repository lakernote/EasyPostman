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
    String apiKeyName;
    String apiKeyValue;
    String apiKeyPlacement;

    public PerformanceAuthConfig(PerformanceAuthType type,
                                 String username,
                                 String password,
                                 String token,
                                 String apiKeyName,
                                 String apiKeyValue,
                                 String apiKeyPlacement) {
        this.type = type == null ? PerformanceAuthType.INHERIT : type;
        this.username = blankToEmpty(username);
        this.password = blankToEmpty(password);
        this.token = blankToEmpty(token);
        this.apiKeyName = blankToEmpty(apiKeyName);
        this.apiKeyValue = blankToEmpty(apiKeyValue);
        this.apiKeyPlacement = blankToDefault(apiKeyPlacement, PerformanceRequestSnapshot.AUTH_API_KEY_PLACEMENT_HEADER);
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
                                                           String token,
                                                           String apiKeyName,
                                                           String apiKeyValue,
                                                           String apiKeyPlacement) {
        return builder()
                .type(authType)
                .username(username)
                .password(password)
                .token(token)
                .apiKeyName(apiKeyName)
                .apiKeyValue(apiKeyValue)
                .apiKeyPlacement(apiKeyPlacement)
                .build();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
