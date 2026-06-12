package com.laker.postman.service.curl;

record CurlOptionSpec(
        String longName,
        Character shortName,
        CurlOptionValueMode valueMode,
        CurlOptionAction action
) {
    boolean requiresValue() {
        return valueMode == CurlOptionValueMode.REQUIRED;
    }

    boolean acceptsOptionalValue() {
        return valueMode == CurlOptionValueMode.OPTIONAL;
    }

    String displayName() {
        if (longName != null && !longName.isBlank()) {
            return "--" + longName;
        }
        if (shortName != null) {
            return "-" + shortName;
        }
        return "<unknown>";
    }
}
