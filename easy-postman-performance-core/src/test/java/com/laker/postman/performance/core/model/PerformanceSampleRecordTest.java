package com.laker.postman.performance.core.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceSampleRecordTest {

    @Test
    public void shouldConvertCoreSampleRecordToRequestResult() {
        PerformanceSampleRecord record = PerformanceSampleRecord.builder()
                .apiId("api")
                .apiName("API")
                .protocol(PerformanceProtocol.WEBSOCKET)
                .startTimeMs(100)
                .endTimeMs(250)
                .elapsedTimeMs(150)
                .responseCode(101)
                .sentMessages(2)
                .receivedMessages(3)
                .matchedMessages(1)
                .sentBytes(128)
                .receivedBytes(256)
                .firstMessageLatencyMs(42)
                .successful(true)
                .build();

        RequestResult result = record.toRequestResult();

        assertEquals(result.apiId, "api");
        assertEquals(result.getApiName(), "API");
        assertEquals(result.protocol, PerformanceProtocol.WEBSOCKET);
        assertEquals(result.startTime, 100L);
        assertEquals(result.endTime, 250L);
        assertTrue(result.success);
        assertEquals(result.sentMessages, 2);
        assertEquals(result.receivedMessages, 3);
        assertEquals(result.matchedMessages, 1);
        assertEquals(result.sentBytes, 128L);
        assertEquals(result.receivedBytes, 256L);
        assertEquals(result.firstMessageLatencyMs, 42L);
    }
}
