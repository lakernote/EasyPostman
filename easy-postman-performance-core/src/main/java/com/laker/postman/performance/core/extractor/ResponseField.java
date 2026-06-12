package com.laker.postman.performance.core.extractor;

import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseField {
    STATUS_CODE("Status Code", MessageKeys.PERFORMANCE_EXTRACTOR_RESPONSE_FIELD_STATUS_CODE, "statusCode"),
    RESPONSE_TIME_MS("Response Time (ms)", MessageKeys.PERFORMANCE_EXTRACTOR_RESPONSE_FIELD_RESPONSE_TIME_MS, "responseTimeMs"),
    BODY_SIZE_BYTES("Body Size (bytes)", MessageKeys.PERFORMANCE_EXTRACTOR_RESPONSE_FIELD_BODY_SIZE_BYTES, "bodySizeBytes"),
    HEADERS_SIZE_BYTES("Headers Size (bytes)", MessageKeys.PERFORMANCE_EXTRACTOR_RESPONSE_FIELD_HEADERS_SIZE_BYTES, "headersSizeBytes"),
    PROTOCOL("Protocol", MessageKeys.PERFORMANCE_EXTRACTOR_RESPONSE_FIELD_PROTOCOL, "protocol");

    private final String storageValue;
    private final String messageKey;
    private final String defaultVariableName;

    public static ResponseField fromStorageValue(String value) {
        for (ResponseField field : values()) {
            if (field.storageValue.equals(value)) {
                return field;
            }
        }
        return STATUS_CODE;
    }

    @Override
    public String toString() {
        return storageValue;
    }
}
