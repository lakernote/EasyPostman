package com.laker.postman.model;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

/**
 * Authentication type enum
 */
public enum AuthType {
    INHERIT("Inherit auth from parent", MessageKeys.AUTH_TYPE_INHERIT),
    NONE("No Auth", MessageKeys.AUTH_TYPE_NONE),
    BASIC("Basic Auth", MessageKeys.AUTH_TYPE_BASIC),
    BEARER("Bearer Token", MessageKeys.AUTH_TYPE_BEARER);

    private final String constant;
    private final String i18nKey;

    AuthType(String constant, String i18nKey) {
        this.constant = constant;
        this.i18nKey = i18nKey;
    }

    public String getConstant() {
        return constant;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public String getDisplayText() {
        return I18nUtil.getMessage(i18nKey);
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

    /**
     * Get AuthType from display text (for backward compatibility)
     */
    public static AuthType fromDisplayText(String displayText) {
        if (displayText == null) {
            return INHERIT;
        }
        for (AuthType type : values()) {
            if (type.getDisplayText().equals(displayText)) {
                return type;
            }
        }
        return INHERIT;
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}

