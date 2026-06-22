package com.laker.postman.http.runtime.model;

/**
 * Runtime capture switches derived from a named execution profile.
 */
public record HttpCapturePolicy(
        boolean captureSentRequest,
        boolean captureSentRequestBody,
        boolean collectMetrics,
        boolean collectEventDetails,
        boolean emitNetworkLog,
        boolean notifyCookieChanges
) {
}
