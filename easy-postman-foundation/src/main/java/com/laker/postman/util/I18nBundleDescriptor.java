package com.laker.postman.util;

import java.util.Objects;

public record I18nBundleDescriptor(String ownerId, String bundleName, ClassLoader classLoader) {

    public I18nBundleDescriptor {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        if (bundleName == null || bundleName.isBlank()) {
            throw new IllegalArgumentException("bundleName must not be blank");
        }
        Objects.requireNonNull(classLoader, "classLoader");
        ownerId = ownerId.trim();
        bundleName = bundleName.trim();
    }
}
