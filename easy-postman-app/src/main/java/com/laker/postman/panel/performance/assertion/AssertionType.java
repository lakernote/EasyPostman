package com.laker.postman.panel.performance.assertion;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssertionType {
    RESPONSE_CODE("Response Code", MessageKeys.PERFORMANCE_ASSERTION_TYPE_RESPONSE_CODE, false),
    RESPONSE_TIME("Response Time", MessageKeys.PERFORMANCE_ASSERTION_TYPE_RESPONSE_TIME, false),
    CONTAINS("Contains", MessageKeys.PERFORMANCE_ASSERTION_TYPE_CONTAINS, true),
    JSON_PATH("JSONPath", MessageKeys.PERFORMANCE_ASSERTION_TYPE_JSON_PATH, true),
    REGEX("Regex", MessageKeys.PERFORMANCE_ASSERTION_TYPE_REGEX, true),
    HEADER_EXISTS("Header Exists", MessageKeys.PERFORMANCE_ASSERTION_TYPE_HEADER_EXISTS, false),
    HEADER_EQUALS("Header Equals", MessageKeys.PERFORMANCE_ASSERTION_TYPE_HEADER_EQUALS, false),
    BODY_SIZE("Body Size", MessageKeys.PERFORMANCE_ASSERTION_TYPE_BODY_SIZE, false);

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
