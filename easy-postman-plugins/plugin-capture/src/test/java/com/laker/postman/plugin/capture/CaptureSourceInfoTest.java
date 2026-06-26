package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CaptureSourceInfoTest {

    @Test
    public void shouldPreferProcessNameAndPidForTableText() {
        CaptureSourceInfo sourceInfo = CaptureSourceInfo.network(
                        "127.0.0.1",
                        53421,
                        "127.0.0.1",
                        8888
                )
                .withProcess("12345", "Chrome Helper", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome Helper");

        assertEquals(sourceInfo.tableText(), "Chrome Helper · PID 12345");
        assertTrue(sourceInfo.detailText().contains("127.0.0.1:53421"));
        assertTrue(sourceInfo.detailText().contains("127.0.0.1:8888"));
        assertTrue(sourceInfo.detailText().contains("/Applications/Google Chrome.app"));
    }

    @Test
    public void shouldFallBackToClientEndpointWhenProcessIsUnknown() {
        CaptureSourceInfo sourceInfo = CaptureSourceInfo.network("192.168.1.23", 61234, "127.0.0.1", 8888);

        assertEquals(sourceInfo.tableText(), "192.168.1.23:61234");
        assertTrue(sourceInfo.detailText().contains(CaptureI18n.t(MessageKeys.TOOLBOX_CAPTURE_SOURCE_UNRESOLVED)));
    }
}
