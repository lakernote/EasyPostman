package com.laker.postman.panel.performance.runtime;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.plan.PerformanceSampler;
import com.laker.postman.panel.performance.plan.PerformanceTimerElement;
import com.laker.postman.panel.performance.timer.TimerData;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceTimerScopeTest {

    @Test
    public void shouldCarryControllerTimersIntoSamplerScope() {
        PerformanceTimerElement controllerTimer = timerElement("controller timer", 10);
        PerformanceSampler sampler = requestSampler("http request", RequestItemProtocolEnum.HTTP, List.of());

        PerformanceTimerScope scope = PerformanceTimerScope.empty()
                .enter(List.of(controllerTimer, sampler));

        assertEquals(scope.timersForSampler(sampler), List.of(controllerTimer));
    }

    @Test
    public void shouldMergeHttpSamplerChildTimersWithInheritedTimers() {
        PerformanceTimerElement controllerTimer = timerElement("controller timer", 10);
        PerformanceTimerElement childTimer = timerElement("child timer", 20);
        PerformanceSampler sampler = requestSampler("http request", RequestItemProtocolEnum.HTTP, List.of(childTimer));

        PerformanceTimerScope scope = PerformanceTimerScope.empty()
                .enter(List.of(controllerTimer));

        assertEquals(scope.timersForSampler(sampler), List.of(controllerTimer, childTimer));
    }

    @Test
    public void shouldKeepWebSocketChildTimersInSamplerScenarioOrder() {
        PerformanceTimerElement controllerTimer = timerElement("controller timer", 10);
        PerformanceTimerElement childTimer = timerElement("child timer", 20);
        PerformanceSampler sampler = requestSampler("ws request", RequestItemProtocolEnum.WEBSOCKET, List.of(childTimer));

        PerformanceTimerScope scope = PerformanceTimerScope.empty()
                .enter(List.of(controllerTimer));

        assertEquals(scope.timersForSampler(sampler), List.of(controllerTimer));
    }

    private static PerformanceTimerElement timerElement(String name, int delayMs) {
        TimerData timerData = new TimerData();
        timerData.delayMs = delayMs;
        return new PerformanceTimerElement(name, timerData);
    }

    private static PerformanceRequestSampler requestSampler(String name,
                                                            RequestItemProtocolEnum protocol,
                                                            List<PerformancePlanElement> children) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(name + "-id");
        item.setName(name);
        item.setProtocol(protocol);
        return new PerformanceRequestSampler(name, item, null, null, children);
    }
}
