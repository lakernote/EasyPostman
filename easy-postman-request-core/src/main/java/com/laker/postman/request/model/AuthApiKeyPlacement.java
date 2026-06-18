package com.laker.postman.request.model;

import lombok.Getter;

@Getter
public enum AuthApiKeyPlacement {
    HEADER("Header", "header"),
    QUERY_PARAMS("Query Params", "query");

    private final String constant;
    private final String postmanValue;

    AuthApiKeyPlacement(String constant, String postmanValue) {
        this.constant = constant;
        this.postmanValue = postmanValue;
    }

    public static AuthApiKeyPlacement fromConstant(String constant) {
        if (constant == null || constant.trim().isEmpty()) {
            return HEADER;
        }
        for (AuthApiKeyPlacement placement : values()) {
            if (placement.constant.equals(constant)) {
                return placement;
            }
        }
        return HEADER;
    }

    public static AuthApiKeyPlacement fromPostmanValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return HEADER;
        }
        for (AuthApiKeyPlacement placement : values()) {
            if (placement.postmanValue.equalsIgnoreCase(value.trim())) {
                return placement;
            }
        }
        return HEADER;
    }
}
