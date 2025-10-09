package com.laker.postman.service.http.ssl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SSL验证结果封装类
 */
public class SSLValidationResult {
    private final boolean valid;
    private final List<String> warnings;
    private final List<String> errors;

    private SSLValidationResult(boolean valid, List<String> warnings, List<String> errors) {
        this.valid = valid;
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public static SSLValidationResult success() {
        return new SSLValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }

    public static SSLValidationResult successWithWarnings(List<String> warnings) {
        return new SSLValidationResult(true, warnings, Collections.emptyList());
    }

    public static SSLValidationResult failure(List<String> errors) {
        return new SSLValidationResult(false, Collections.emptyList(), errors);
    }

    public static SSLValidationResult failureWithWarnings(List<String> errors, List<String> warnings) {
        return new SSLValidationResult(false, warnings, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public String getSummary() {
        List<String> messages = new ArrayList<>();
        if (!errors.isEmpty()) {
            messages.addAll(errors);
        }
        if (!warnings.isEmpty()) {
            messages.addAll(warnings);
        }
        return String.join("; ", messages);
    }

    @Override
    public String toString() {
        return "SSLValidationResult{valid=" + valid +
               ", warnings=" + warnings.size() +
               ", errors=" + errors.size() + "}";
    }
}

