package com.laker.postman.http.runtime.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpCaptureProfiles {
    private static final HttpCapturePolicy NO_CAPTURE =
            new HttpCapturePolicy(false, false, false, false, false, false);

    public void apply(PreparedRequest request, HttpCaptureProfile profile) {
        if (request == null || profile == null) {
            return;
        }
        request.captureProfile = profile;
        HttpCapturePolicy policy = profile.policy();
        request.collectBasicInfo = profile == HttpCaptureProfile.COLLECTION_DIAGNOSTIC
                || profile == HttpCaptureProfile.FUNCTIONAL_DIAGNOSTIC;
        request.collectMetricsInfo = policy.collectMetrics();
        request.collectEventInfo = policy.collectEventDetails();
        request.enableNetworkLog = policy.emitNetworkLog();
        request.notifyCookieChanges = policy.notifyCookieChanges();
    }

    public HttpCapturePolicy resolve(PreparedRequest request) {
        if (request == null) {
            return NO_CAPTURE;
        }
        if (request.captureProfile != null) {
            return request.captureProfile.policy();
        }
        return legacyPolicy(request);
    }

    private HttpCapturePolicy legacyPolicy(PreparedRequest request) {
        boolean emitNetworkLog = request.enableNetworkLog;
        boolean collectEventDetails = request.collectEventInfo;
        boolean collectMetrics = request.collectMetricsInfo || collectEventDetails || emitNetworkLog;
        boolean captureSentRequest = emitNetworkLog
                || (request.collectBasicInfo && request.collectEventInfo && !request.collectMetricsInfo);
        return new HttpCapturePolicy(
                captureSentRequest,
                captureSentRequest,
                collectMetrics,
                collectEventDetails,
                emitNetworkLog,
                request.notifyCookieChanges
        );
    }
}
