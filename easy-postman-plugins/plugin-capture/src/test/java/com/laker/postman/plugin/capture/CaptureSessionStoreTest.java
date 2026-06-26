package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureSessionStoreTest {

    @Test
    public void shouldThrottleRepeatedTlsIssueRowsForSameHost() {
        CaptureSessionStore store = new CaptureSessionStore();

        boolean firstRecorded = store.recordTlsIssue("api.apple-cloudkit.com", 443, "first");
        boolean duplicateRecorded = store.recordTlsIssue("api.apple-cloudkit.com", 443, "second");

        assertTrue(firstRecorded);
        assertFalse(duplicateRecorded);
        assertEquals(store.snapshot().size(), 1);
        assertEquals(store.snapshot().get(0).method(), "TLS");
        assertEquals(store.snapshot().get(0).statusCode(), 495);
    }

    @Test
    public void shouldAttachSourceAndDiagnosticsToTlsIssueRows() {
        CaptureSessionStore store = new CaptureSessionStore();
        CaptureSourceInfo sourceInfo = CaptureSourceInfo.network("127.0.0.1", 53421, "127.0.0.1", 8888)
                .withProcess("222", "Cisco Agent", "/Applications/Cisco Agent.app/Contents/MacOS/Cisco Agent");

        boolean recorded = store.recordTlsIssue(
                "identify.prod.nam.csc.cisco.com",
                443,
                "SSLHandshakeException: unknown_ca",
                "connection-1",
                sourceInfo,
                List.of(
                        CaptureDiagnosticEvent.info(
                                CaptureDiagnosticPhase.CONNECT,
                                CaptureDiagnosticRole.CLIENT_CONNECTION,
                                "Client requested HTTPS tunnel",
                                "CONNECT identify.prod.nam.csc.cisco.com:443",
                                ""
                        ),
                        CaptureDiagnosticEvent.error(
                                CaptureDiagnosticPhase.CLIENT_TLS,
                                CaptureDiagnosticRole.SOURCE_APP,
                                "Client rejected EasyPostman certificate",
                                "SSLHandshakeException: unknown_ca",
                                "Bypass this host or import the EasyPostman Root CA into the source app trust store."
                        )
                )
        );

        assertTrue(recorded);
        CaptureFlow flow = store.snapshot().get(0);
        assertEquals(flow.sourceInfo().tableText(), "Cisco Agent · PID 222");
        assertTrue(flow.diagnosticsDetailText().contains("CONNECT"));
        assertTrue(flow.diagnosticsDetailText().contains("CLIENT_TLS"));
        assertTrue(flow.diagnosticsDetailText().contains("unknown_ca"));
    }

    @Test
    public void shouldAppendDiagnosticsToExistingFlowsForConnection() {
        CaptureSessionStore store = new CaptureSessionStore();
        CaptureFlow flow = store.createFlow(
                "GET",
                "https://example.com/api",
                "example.com",
                "/api",
                Map.of(),
                new byte[0],
                "connection-1",
                CaptureSourceInfo.network("127.0.0.1", 53421, "127.0.0.1", 8888),
                List.of()
        );

        store.appendDiagnosticEventForConnection(
                "connection-1",
                CaptureDiagnosticEvent.info(
                        CaptureDiagnosticPhase.SOURCE_RESOLVE,
                        CaptureDiagnosticRole.SOURCE_APP,
                        "Resolved source process",
                        "Chrome Helper · PID 12345",
                        ""
                )
        );

        assertTrue(store.find(flow.id()).diagnosticsDetailText().contains("SOURCE_RESOLVE"));
        assertTrue(store.find(flow.id()).diagnosticsDetailText().contains("Chrome Helper"));
    }

    @Test
    public void shouldTrimOldestFlowsWhenMaxFlowLimitIsReached() {
        CaptureSessionStore store = new CaptureSessionStore(50);

        CaptureFlow first = null;
        CaptureFlow newest = null;
        for (int i = 1; i <= 51; i++) {
            CaptureFlow flow = store.createFlow(
                    "GET",
                    "https://example.com/" + i,
                    "example.com",
                    "/" + i,
                    Map.of(),
                    new byte[0]);
            if (i == 1) {
                first = flow;
            }
            newest = flow;
        }

        assertEquals(store.snapshot().size(), 50);
        assertEquals(store.snapshot().get(0).id(), newest.id());
        String firstFlowId = first.id();
        assertFalse(store.snapshot().stream().anyMatch(flow -> flow.id().equals(firstFlowId)));
        assertEquals(store.maxFlows(), 50);
    }
}
