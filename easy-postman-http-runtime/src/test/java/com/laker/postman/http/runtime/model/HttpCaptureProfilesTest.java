package com.laker.postman.http.runtime.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class HttpCaptureProfilesTest {

    @Test
    public void collectionDiagnosticShouldCaptureDetailedRequestAndEmitNetworkLog() {
        PreparedRequest request = new PreparedRequest();

        HttpCaptureProfiles.apply(request, HttpCaptureProfile.COLLECTION_DIAGNOSTIC);

        assertSame(request.captureProfile, HttpCaptureProfile.COLLECTION_DIAGNOSTIC);
        assertTrue(request.collectBasicInfo);
        assertTrue(request.collectMetricsInfo);
        assertTrue(request.collectEventInfo);
        assertTrue(request.enableNetworkLog);
        assertTrue(request.notifyCookieChanges);
        assertTrue(HttpCaptureProfiles.resolve(request).captureSentRequest());
        assertTrue(HttpCaptureProfiles.resolve(request).captureSentRequestBody());
        assertTrue(HttpCaptureProfiles.resolve(request).emitNetworkLog());
    }

    @Test
    public void functionalDiagnosticShouldCaptureDetailedRequestWithoutNetworkLog() {
        PreparedRequest request = new PreparedRequest();

        HttpCaptureProfiles.apply(request, HttpCaptureProfile.FUNCTIONAL_DIAGNOSTIC);

        assertSame(request.captureProfile, HttpCaptureProfile.FUNCTIONAL_DIAGNOSTIC);
        assertTrue(request.collectBasicInfo);
        assertTrue(request.collectMetricsInfo);
        assertTrue(request.collectEventInfo);
        assertFalse(request.enableNetworkLog);
        assertTrue(request.notifyCookieChanges);
        assertTrue(HttpCaptureProfiles.resolve(request).captureSentRequest());
        assertTrue(HttpCaptureProfiles.resolve(request).captureSentRequestBody());
        assertFalse(HttpCaptureProfiles.resolve(request).emitNetworkLog());
    }

    @Test
    public void performanceMetricsShouldAvoidRequestSnapshotAndNetworkLog() {
        PreparedRequest request = new PreparedRequest();

        HttpCaptureProfiles.apply(request, HttpCaptureProfile.PERFORMANCE_METRICS);

        assertSame(request.captureProfile, HttpCaptureProfile.PERFORMANCE_METRICS);
        assertFalse(request.collectBasicInfo);
        assertTrue(request.collectMetricsInfo);
        assertFalse(request.collectEventInfo);
        assertFalse(request.enableNetworkLog);
        assertFalse(request.notifyCookieChanges);
        assertFalse(HttpCaptureProfiles.resolve(request).captureSentRequest());
        assertFalse(HttpCaptureProfiles.resolve(request).captureSentRequestBody());
        assertFalse(HttpCaptureProfiles.resolve(request).emitNetworkLog());
    }

    @Test
    public void performanceEventTraceShouldCollectEventsWithoutRequestSnapshotOrNetworkLog() {
        PreparedRequest request = new PreparedRequest();

        HttpCaptureProfiles.apply(request, HttpCaptureProfile.PERFORMANCE_EVENT_TRACE);

        assertSame(request.captureProfile, HttpCaptureProfile.PERFORMANCE_EVENT_TRACE);
        assertFalse(request.collectBasicInfo);
        assertTrue(request.collectMetricsInfo);
        assertTrue(request.collectEventInfo);
        assertFalse(request.enableNetworkLog);
        assertFalse(request.notifyCookieChanges);
        assertFalse(HttpCaptureProfiles.resolve(request).captureSentRequest());
        assertFalse(HttpCaptureProfiles.resolve(request).captureSentRequestBody());
        assertFalse(HttpCaptureProfiles.resolve(request).emitNetworkLog());
        assertTrue(HttpCaptureProfiles.resolve(request).collectEventDetails());
    }
}
