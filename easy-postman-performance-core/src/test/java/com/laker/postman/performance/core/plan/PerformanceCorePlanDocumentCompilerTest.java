package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.request.PerformanceRequestKeyValue;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;

public class PerformanceCorePlanDocumentCompilerTest {

    @Test
    public void shouldCompilePureCoreDocumentWithoutAppRequestTypes() {
        LoopData loopData = new LoopData();
        loopData.iterations = 2;
        PerformanceRequestSnapshot snapshot = PerformanceRequestSnapshot.builder()
                .id("request-1")
                .name("request")
                .url("wss://example.test/ws")
                .protocol(PerformanceProtocol.WEBSOCKET)
                .headers(List.of(new PerformanceRequestKeyValue(true, "X-Test", "yes")))
                .build();

        PerformanceCorePlanNode request = PerformanceCorePlanNode.builder()
                .name("request")
                .type(NodeType.REQUEST)
                .requestSnapshot(snapshot)
                .build();
        PerformanceCorePlanNode loop = PerformanceCorePlanNode.builder()
                .name("loop")
                .type(NodeType.LOOP)
                .loopData(loopData)
                .children(List.of(request))
                .build();
        PerformanceCorePlanNode disabledGroup = PerformanceCorePlanNode.builder()
                .name("disabled")
                .type(NodeType.THREAD_GROUP)
                .enabled(false)
                .threadGroupData(threadGroupData())
                .children(List.of(request))
                .build();
        PerformanceCorePlanNode enabledGroup = PerformanceCorePlanNode.builder()
                .name("enabled")
                .type(NodeType.THREAD_GROUP)
                .threadGroupData(threadGroupData())
                .children(List.of(loop))
                .build();

        PerformanceTestPlan compiled = PerformanceCorePlanDocumentCompiler.compile(
                new PerformanceCorePlanDocument(PerformanceCorePlanNode.builder()
                        .name("plan")
                        .type(NodeType.ROOT)
                        .children(List.of(disabledGroup, enabledGroup))
                        .build())
        );

        assertEquals(compiled.getThreadGroups().size(), 1);
        PerformanceThreadGroupPlan group = compiled.getThreadGroups().get(0);
        assertEquals(group.getName(), "enabled");
        assertTrue(group.getElements().get(0) instanceof PerformanceLoopController);
        PerformanceLoopController loopController = (PerformanceLoopController) group.getElements().get(0);
        assertEquals(loopController.getIterationCount(), 2);
        assertTrue(loopController.getElements().get(0) instanceof PerformanceCoreRequestSampler);

        PerformanceCoreRequestSampler sampler = (PerformanceCoreRequestSampler) loopController.getElements().get(0);
        assertEquals(sampler.getRequestSnapshot().getUrl(), "wss://example.test/ws");
        assertTrue(sampler.executesChildrenInSamplerOrder());
        assertNotSame(sampler.getRequestSnapshot(), snapshot);
    }

    @Test
    public void requestSamplerShouldNotInventEmptyRequestSnapshot() {
        PerformanceCoreRequestSampler sampler = new PerformanceCoreRequestSampler(
                "request",
                null,
                null,
                List.of()
        );

        assertEquals(sampler.getName(), "request");
        assertFalse(sampler.executesChildrenInSamplerOrder());
        assertEquals(sampler.getChildren(), List.of());
    }

    private static ThreadGroupData threadGroupData() {
        ThreadGroupData data = new ThreadGroupData();
        data.numThreads = 1;
        data.useTime = false;
        data.loops = 1;
        return data;
    }
}
