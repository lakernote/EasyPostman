package com.laker.postman.panel.performance.plan;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.timer.TimerData;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformancePlanContractsTest {

    @Test
    public void loopShouldExposeControllerContract() {
        LoopData loopData = new LoopData();
        loopData.iterations = 3;
        PerformanceTimerElement timer = timerElement("timer", 100);

        PerformanceController controller = new PerformanceLoopController("loop", loopData, List.of(timer));

        assertEquals(controller.getName(), "loop");
        assertEquals(controller.getIterationCount(), 3);
        assertEquals(controller.getElements(), List.of(timer));
    }

    @Test
    public void loopShouldExposeNormalizedIterationCount() {
        LoopData loopData = new LoopData();
        loopData.iterations = 0;

        PerformanceController controller = new PerformanceLoopController("loop", loopData, List.of());

        assertEquals(controller.getIterationCount(), LoopData.MIN_ITERATIONS);
    }

    @Test
    public void requestShouldExposeSamplerContract() {
        PerformanceTimerElement timer = timerElement("timer", 50);

        PerformanceSampler sampler = new PerformanceRequestSampler(
                "http request",
                requestItem("http request", RequestItemProtocolEnum.HTTP),
                null,
                List.of(timer)
        );

        assertEquals(sampler.getName(), "http request");
        assertEquals(sampler.getChildren(), List.of(timer));
        assertFalse(sampler.executesChildrenInSamplerOrder());
    }

    @Test
    public void webSocketRequestShouldDeclareScenarioOrderedChildren() {
        PerformanceSampler sampler = new PerformanceRequestSampler(
                "ws request",
                requestItem("ws request", RequestItemProtocolEnum.WEBSOCKET),
                null,
                List.of(timerElement("ws timer", 25))
        );

        assertTrue(sampler.executesChildrenInSamplerOrder());
    }

    private static PerformanceTimerElement timerElement(String name, int delayMs) {
        TimerData timerData = new TimerData();
        timerData.delayMs = delayMs;
        return new PerformanceTimerElement(name, timerData);
    }

    private static HttpRequestItem requestItem(String name, RequestItemProtocolEnum protocol) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(name + "-id");
        item.setName(name);
        item.setProtocol(protocol);
        return item;
    }
}
