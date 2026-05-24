package com.laker.postman.panel.performance.assertion;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssertionType {
    RESPONSE_CODE("Response Code", MessageKeys.PERFORMANCE_ASSERTION_TYPE_RESPONSE_CODE, false),
    CONTAINS("Contains", MessageKeys.PERFORMANCE_ASSERTION_TYPE_CONTAINS, true),
    JSON_PATH("JSONPath", MessageKeys.PERFORMANCE_ASSERTION_TYPE_JSON_PATH, true);

    private final String storageValue;
    private final String messageKey;
    private final boolean requiresResponseBody;

    public static AssertionType fromStorageValue(String value) {
        for (AssertionType type : values()) {
            if (type.storageValue.equals(value)) {
                return type;
            }
        }
        return RESPONSE_CODE;
    }

    public String displayName() {
        return I18nUtil.getMessage(messageKey);
    }

    public boolean requiresResponseBody() {
        return requiresResponseBody;
    }

    @Override
    public String toString() {
        return displayName();
    }
}
