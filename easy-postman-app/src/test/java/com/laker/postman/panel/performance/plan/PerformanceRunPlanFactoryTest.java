package com.laker.postman.panel.performance.plan;

import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.Variable;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.request.PerformanceRequestFormDataPart;
import com.laker.postman.performance.core.run.PerformanceRunAsset;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlanJsonStorage;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceRunPlanFactoryTest {

    @Test
    public void shouldExportExecutableRunPlanFromAppConfiguration() {
        PerformancePlanConfiguration configuration = PerformancePlanConfiguration.builder()
                .planDocument(new PerformancePlanDocument(
                        PerformancePlanNode.builder()
                                .name("Plan")
                                .type(NodeType.ROOT)
                                .children(List.of(
                                        PerformancePlanNode.builder()
                                                .name("Users")
                                                .type(NodeType.THREAD_GROUP)
                                                .threadGroupData(new ThreadGroupData())
                                                .children(List.of(
                                                        PerformancePlanNode.builder()
                                                                .name("Upload")
                                                                .type(NodeType.REQUEST)
                                                                .httpRequestItem(uploadRequest())
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .build()
                ))
                .efficientMode(false)
                .trendEnabled(true)
                .reportRealtimeEnabled(true)
                .build();
        Environment environment = environment("env-dev", "Dev", new Variable(true, "baseUrl", "https://example.test"));
        Environment globals = environment("globals", "Globals", new Variable(true, "token", "abc123"));

        PerformanceRunPlan runPlan = PerformanceRunPlanFactory.create(
                configuration,
                environment,
                globals,
                "EasyPostman Test"
        );

        assertEquals(runPlan.getGeneratedBy(), "EasyPostman Test");
        assertFalse(runPlan.getSettings().isEfficientMode());
        assertEquals(runPlan.getEnvironment().getVariables().get(0).getKey(), "baseUrl");
        assertEquals(runPlan.getGlobals().getVariables().get(0).getValue(), "abc123");
        assertEquals(runPlan.getAssets().size(), 1);
        PerformanceRunAsset asset = runPlan.getAssets().get(0);
        assertEquals(asset.getType(), PerformanceRunAsset.TYPE_FILE);
        assertEquals(asset.getPath(), "assets/files/avatar.png");

        String json = new PerformanceRunPlanJsonStorage().toJson(runPlan);
        assertTrue(json.contains("\"testPlan\""));
        assertTrue(json.contains("\"requestSnapshot\""));
        assertFalse(json.contains("\"trendEnabled\""));
        assertFalse(json.contains("\"reportRealtimeEnabled\""));
        assertFalse(json.contains("\"requestItem\""));

        PerformanceRequestFormDataPart formData = runPlan.getTestPlan()
                .getRoot()
                .getChildren()
                .get(0)
                .getChildren()
                .get(0)
                .getRequestSnapshot()
                .getFormData()
                .get(0);
        assertTrue(formData.isFile());
        assertEquals(formData.getValue(), "assets/files/avatar.png");
    }

    private static HttpRequestItem uploadRequest() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("upload");
        item.setName("Upload");
        item.setProtocol(RequestItemProtocolEnum.HTTP);
        item.setMethod("POST");
        item.setUrl("https://example.test/upload");
        item.setFormDataList(List.of(new HttpFormData(
                true,
                "avatar",
                HttpFormData.TYPE_FILE,
                "assets/files/avatar.png"
        )));
        return item;
    }

    private static Environment environment(String id, String name, Variable variable) {
        Environment environment = new Environment(name);
        environment.setId(id);
        environment.setVariableList(List.of(variable));
        return environment;
    }
}
