package com.laker.postman.http.request;

public final class HttpRequestValidationResult {
    private static final HttpRequestValidationResult OK =
            new HttpRequestValidationResult(true, false, null, false, false, null);

    private final boolean valid;
    private final boolean warning;
    private final String message;
    private final boolean focusUrlField;
    private final boolean confirmationRequired;
    private final String confirmationTitle;

    private HttpRequestValidationResult(boolean valid,
                                        boolean warning,
                                        String message,
                                        boolean focusUrlField,
                                        boolean confirmationRequired,
                                        String confirmationTitle) {
        this.valid = valid;
        this.warning = warning;
        this.message = message;
        this.focusUrlField = focusUrlField;
        this.confirmationRequired = confirmationRequired;
        this.confirmationTitle = confirmationTitle;
    }

    public static HttpRequestValidationResult ok() {
        return OK;
    }

    public static HttpRequestValidationResult error(String message, boolean focusUrlField) {
        return new HttpRequestValidationResult(false, false, message, focusUrlField, false, null);
    }

    public static HttpRequestValidationResult warning(String message, boolean focusUrlField) {
        return new HttpRequestValidationResult(false, true, message, focusUrlField, false, null);
    }

    public static HttpRequestValidationResult okWithWarning(String message) {
        return new HttpRequestValidationResult(true, true, message, false, false, null);
    }

    public static HttpRequestValidationResult requiresConfirmation(String message, String confirmationTitle) {
        return new HttpRequestValidationResult(true, false, message, false, true, confirmationTitle);
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isWarning() {
        return warning;
    }

    public String getMessage() {
        return message;
    }

    public boolean shouldFocusUrlField() {
        return focusUrlField;
    }

    public boolean requiresConfirmation() {
        return confirmationRequired;
    }

    public String getConfirmationTitle() {
        return confirmationTitle;
    }
}
