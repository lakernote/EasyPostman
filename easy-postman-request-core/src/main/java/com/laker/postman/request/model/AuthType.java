package com.laker.postman.request.model;

import lombok.Getter;

/**
 * Authentication type enum
 */
@Getter
public enum AuthType {
    INHERIT("Inherit auth from parent"),
    NONE("No Auth"),
    API_KEY("API Key"),
    BASIC("Basic Auth"),
    BEARER("Bearer Token"),
    DIGEST("Digest Auth");

    private final String constant;

    AuthType(String constant) {
        this.constant = constant;
    }

    /**
     * Get AuthType from constant string
     */
    public static AuthType fromConstant(String constant) {
        if (constant == null) {
            return INHERIT;
        }
        for (AuthType type : values()) {
            if (type.constant.equals(constant)) {
                return type;
            }
        }
        return INHERIT;
    }
}
