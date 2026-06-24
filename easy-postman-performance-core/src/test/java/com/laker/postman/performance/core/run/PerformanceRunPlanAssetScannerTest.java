package com.laker.postman.performance.core.run;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceRunPlanAssetScannerTest {

    @Test
    public void shouldIncludeBinaryBodyFileAsRequestAsset() {
        PerformanceCorePlanDocument document = new PerformanceCorePlanDocument(
                PerformanceCorePlanNode.builder()
                        .name("root")
                        .type(NodeType.THREAD_GROUP)
                        .children(List.of(PerformanceCorePlanNode.builder()
                                .name("Binary Upload")
                                .type(NodeType.REQUEST)
                                .requestSnapshot(PerformanceRequestSnapshot.builder()
                                        .bodyType("binary")
                                        .body("assets/files/upload.bin")
                                        .build())
                                .build()))
                        .build()
        );

        List<PerformanceRunAsset> assets = PerformanceRunPlanAssetScanner.scan(document);

        assertEquals(assets.size(), 1);
        assertEquals(assets.get(0).getType(), PerformanceRunAsset.TYPE_FILE);
        assertEquals(assets.get(0).getPath(), "assets/files/upload.bin");
    }
}
