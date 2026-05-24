package com.laker.postman.panel.performance.runtime;

import com.laker.postman.service.variable.ExecutionVariableContext;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class PerformanceIterationContextFactoryTest {

    @Test
    public void shouldResolveIterationDataThroughProviderInsteadOfSwingPanel() {
        PerformanceVirtualUserCoordinator virtualUsers = new PerformanceVirtualUserCoordinator();
        PerformanceIterationContextFactory factory = new PerformanceIterationContextFactory(
                virtualUserIndex -> Map.of("user", "vu-" + virtualUserIndex),
                virtualUsers
        );
        List<ExecutionVariableContext> contexts = new ArrayList<>();

        virtualUsers.newThread("test-vu", (active, total) -> {
        }, 1, () -> contexts.add(factory.create(3))).run();

        assertEquals(contexts.size(), 1);
        assertEquals(contexts.get(0).getIterationData().get("user"), "vu-0");
    }
}
