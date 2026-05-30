package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.request.PerformanceRequestFormDataPart;
import com.laker.postman.performance.core.request.PerformanceRequestKeyValue;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceCorePlanDocumentSanitizerTest {

    @Test
    public void enabledOnlyShouldDropDisabledBranchesAndRequestItems() {
        PerformanceCorePlanNode enabledRequest = PerformanceCorePlanNode.builder()
                .name("enabled request")
                .type(NodeType.REQUEST)
                .requestSnapshot(PerformanceRequestSnapshot.builder()
                        .url("https://example.test/upload")
                        .headers(List.of(
                                new PerformanceRequestKeyValue(true, "X-Enabled", "1"),
                                new PerformanceRequestKeyValue(false, "X-Disabled", "2")
                        ))
                        .params(List.of(
                                new PerformanceRequestKeyValue(false, "disabled-param", "1"),
                                new PerformanceRequestKeyValue(true, "enabled-param", "2")
                        ))
                        .formData(List.of(
                                new PerformanceRequestFormDataPart(false, "disabledFile", "File", "disabled.png"),
                                new PerformanceRequestFormDataPart(true, "enabledFile", "File", "enabled.png")
                        ))
                        .build())
                .build();
        PerformanceCorePlanNode disabledRequest = PerformanceCorePlanNode.builder()
                .name("disabled request")
                .type(NodeType.REQUEST)
                .enabled(false)
                .requestSnapshot(PerformanceRequestSnapshot.builder()
                        .url("https://example.test/disabled")
                        .build())
                .build();
        PerformanceCorePlanNode group = PerformanceCorePlanNode.builder()
                .name("enabled group")
                .type(NodeType.THREAD_GROUP)
                .threadGroupData(threadGroupData())
                .children(List.of(enabledRequest, disabledRequest))
                .build();
        PerformanceCorePlanNode disabledGroup = PerformanceCorePlanNode.builder()
                .name("disabled group")
                .type(NodeType.THREAD_GROUP)
                .enabled(false)
                .threadGroupData(threadGroupData())
                .children(List.of(enabledRequest))
                .build();

        PerformanceCorePlanDocument sanitized = PerformanceCorePlanDocumentSanitizer.enabledOnly(
                new PerformanceCorePlanDocument(PerformanceCorePlanNode.builder()
                        .name("root")
                        .type(NodeType.ROOT)
                        .children(List.of(group, disabledGroup))
                        .build())
        );

        List<PerformanceCorePlanNode> groups = sanitized.getRoot().getChildren();
        assertEquals(groups.size(), 1);
        assertEquals(groups.get(0).getName(), "enabled group");
        assertEquals(groups.get(0).getChildren().size(), 1);
        PerformanceRequestSnapshot snapshot = groups.get(0).getChildren().get(0).getRequestSnapshot();
        assertEquals(snapshot.getHeaders().size(), 1);
        assertEquals(snapshot.getHeaders().get(0).getKey(), "X-Enabled");
        assertEquals(snapshot.getParams().size(), 1);
        assertEquals(snapshot.getParams().get(0).getKey(), "enabled-param");
        assertEquals(snapshot.getFormData().size(), 1);
        assertEquals(snapshot.getFormData().get(0).getValue(), "enabled.png");
    }

    private static ThreadGroupData threadGroupData() {
        ThreadGroupData data = new ThreadGroupData();
        data.numThreads = 1;
        return data;
    }
}
