package com.laker.postman.model;

public record RequestImportResult(Status status, int importedCount) {

    public RequestImportResult {
        status = status == null ? Status.UNAVAILABLE : status;
        importedCount = Math.max(importedCount, 0);
    }

    public static RequestImportResult imported(int count) {
        return new RequestImportResult(Status.IMPORTED, count);
    }

    public static RequestImportResult cancelled() {
        return new RequestImportResult(Status.CANCELLED, 0);
    }

    public static RequestImportResult unavailable() {
        return new RequestImportResult(Status.UNAVAILABLE, 0);
    }

    public boolean isImported() {
        return status == Status.IMPORTED && importedCount > 0;
    }

    public enum Status {
        IMPORTED,
        CANCELLED,
        UNAVAILABLE
    }
}
