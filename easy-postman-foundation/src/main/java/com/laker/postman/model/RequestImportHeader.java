package com.laker.postman.model;

public record RequestImportHeader(boolean enabled, String key, String value) {

    public RequestImportHeader {
        key = normalize(key);
        value = normalize(value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
