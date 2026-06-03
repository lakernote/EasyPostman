package com.laker.postman.util;

import java.util.List;

public record I18nDiagnosticsReport(List<I18nMissingKey> missingKeys, List<I18nDuplicateKey> duplicateKeys) {

    public I18nDiagnosticsReport {
        missingKeys = List.copyOf(missingKeys);
        duplicateKeys = List.copyOf(duplicateKeys);
    }

    public boolean hasIssues() {
        return !missingKeys.isEmpty() || !duplicateKeys.isEmpty();
    }
}
