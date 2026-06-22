package com.laker.postman.http.runtime.model;

public enum HttpCaptureProfile {
    COLLECTION_DIAGNOSTIC(new HttpCapturePolicy(true, true, true, true, true, true)),
    FUNCTIONAL_DIAGNOSTIC(new HttpCapturePolicy(true, true, true, true, false, true)),
    PERFORMANCE_METRICS(new HttpCapturePolicy(false, false, true, false, false, false)),
    PERFORMANCE_EVENT_TRACE(new HttpCapturePolicy(false, false, true, true, false, false));

    private final HttpCapturePolicy policy;

    HttpCaptureProfile(HttpCapturePolicy policy) {
        this.policy = policy;
    }

    public HttpCapturePolicy policy() {
        return policy;
    }
}
