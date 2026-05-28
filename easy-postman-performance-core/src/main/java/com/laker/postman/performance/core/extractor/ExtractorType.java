package com.laker.postman.performance.core.extractor;

import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExtractorType {
    JSON_PATH("JSONPath", MessageKeys.PERFORMANCE_EXTRACTOR_TYPE_JSON_PATH, true),
    REGEX("Regex", MessageKeys.PERFORMANCE_EXTRACTOR_TYPE_REGEX, true),
    HEADER("Header", MessageKeys.PERFORMANCE_EXTRACTOR_TYPE_HEADER, false),
    COOKIE("Cookie", MessageKeys.PERFORMANCE_EXTRACTOR_TYPE_COOKIE, false);

    private final String storageValue;
    private final String messageKey;
    private final boolean requiresResponseBody;

    public static ExtractorType fromStorageValue(String value) {
        for (ExtractorType type : values()) {
            if (type.storageValue.equals(value)) {
                return type;
            }
        }
        return JSON_PATH;
    }

    public boolean requiresResponseBody() {
        return requiresResponseBody;
    }

    @Override
    public String toString() {
        return storageValue;
    }
}
