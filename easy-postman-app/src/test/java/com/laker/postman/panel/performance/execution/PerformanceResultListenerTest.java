package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceSampleEvent;
import com.laker.postman.panel.performance.model.PerformanceSampleResult;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class PerformanceResultListenerTest {

    @Test
    public void recorderShouldPublishProtocolNeutralSampleEventsToListeners() {
        List<PerformanceSampleEvent> events = new ArrayList<>();
        PerformanceResultRecorder recorder = new PerformanceResultRecorder(List.of(events::add));

        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.costMs = 42;
        response.endTime = 142;

        PerformanceRequestExecutionResult executionResult = new PerformanceRequestExecutionResult(
                "api",
                "API",
                new PreparedRequest(),
                response,
                "",
                List.of(),
                false,
                false,
                PerformanceProtocol.HTTP,
                100L,
                0L
        );

        recorder.record(executionResult, true);

        assertEquals(events.size(), 1);
        PerformanceSampleEvent event = events.get(0);
        PerformanceSampleResult sampleResult = event.sampleResult();
        assertSame(event.executionResult(), executionResult);
        assertTrue(event.efficientMode());
        assertEquals(sampleResult.getApiId(), "api");
        assertEquals(sampleResult.getElapsedTimeMs(), 42L);
        assertTrue(sampleResult.isSuccessful());
    }
}
