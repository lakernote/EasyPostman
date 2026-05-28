package com.laker.postman.panel.performance.plan;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PerformancePlanStorageTest {

    @Test
    public void storageApiShouldNotExposeSwingOrCsvPanelTypes() {
        assertFalse(exposesType(PerformancePlanStorage.class, DefaultMutableTreeNode.class));
        assertFalse(exposesType(PerformancePlanStorage.class, CsvDataPanel.CsvState.class));
    }

    @Test
    public void shouldSaveAndLoadPureConfiguration() throws Exception {
        Path configPath = Files.createTempDirectory("performance-plan-storage").resolve("performance_config.json");
        PerformancePlanStorage storage = new PerformancePlanStorage();

        HttpRequestItem requestItem = request("snapshot-request", "https://example.test/snapshot");
        WebSocketPerformanceData webSocketData = new WebSocketPerformanceData();
        webSocketData.connectTimeoutMs = 2468;
        PerformanceCsvState csvState = new PerformanceCsvState(
                "storage-users.csv",
                List.of("userId"),
                List.of(java.util.Map.of("userId", "u-1"))
        );

        PerformancePlanDocument document = new PerformancePlanDocument(
                PerformancePlanNode.builder()
                        .name("Storage Plan")
                        .type(NodeType.ROOT)
                        .children(List.of(
                                PerformancePlanNode.builder()
                                        .name("Users")
                                        .type(NodeType.THREAD_GROUP)
                                        .threadGroupData(new ThreadGroupData())
                                        .children(List.of(
                                                PerformancePlanNode.builder()
                                                        .name("Snapshot")
                                                        .type(NodeType.REQUEST)
                                                        .httpRequestItem(requestItem)
                                                        .webSocketPerformanceData(webSocketData)
                                                        .requestInheritanceSnapshot(true)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );

        storage.saveConfiguration(configPath, PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(false)
                .trendEnabled(false)
                .reportRealtimeEnabled(true)
                .csvState(csvState)
                .build());
        String savedJson = Files.readString(configPath, StandardCharsets.UTF_8);

        PerformancePlanConfiguration loaded = storage.loadConfiguration(configPath, id -> null);

        assertNotNull(loaded);
        assertFalse(loaded.isEfficientMode());
        assertFalse(loaded.isTrendEnabled());
        assertTrue(loaded.isReportRealtimeEnabled());
        assertEquals(loaded.getCsvState().getSourceName(), "storage-users.csv");
        assertTrue(savedJson.contains("\"requestSnapshot\""));
        assertFalse(savedJson.contains("\"requestItem\""));
        assertFalse(savedJson.contains("\"requestItemId\""));
        PerformancePlanNode loadedRequest = loaded.getPlanDocument().getRoot().getChildren().get(0).getChildren().get(0);
        assertEquals(loadedRequest.getHttpRequestItem().getUrl(), "https://example.test/snapshot");
        assertEquals(loadedRequest.getRequestSnapshot().getUrl(), "https://example.test/snapshot");
        assertEquals(loadedRequest.getRequestSnapshot().getId(), "snapshot-request");
        assertEquals(loadedRequest.getWebSocketPerformanceData().connectTimeoutMs, 2468);
        assertTrue(loadedRequest.isRequestInheritanceSnapshot());
    }

    @Test
    public void shouldResolveLegacyRequestIdThroughInjectedResolver() throws Exception {
        Path configPath = Files.createTempDirectory("performance-plan-storage-legacy").resolve("performance_config.json");
        Files.writeString(configPath, """
                {
                  "version": "1.0",
                  "tree": {
                    "name": "Plan",
                    "type": "ROOT",
                    "enabled": true,
                    "children": [
                      {
                        "name": "Legacy Request",
                        "type": "REQUEST",
                        "enabled": true,
                        "requestItemId": "legacy-id"
                      }
                    ]
                  }
                }
                """, StandardCharsets.UTF_8);

        PerformancePlanConfiguration loaded = new PerformancePlanStorage().loadConfiguration(
                configPath,
                id -> "legacy-id".equals(id) ? request(id, "https://example.test/legacy") : null
        );

        PerformancePlanNode loadedRequest = loaded.getPlanDocument().getRoot().getChildren().get(0);
        assertEquals(loadedRequest.getHttpRequestItem().getId(), "legacy-id");
        assertEquals(loadedRequest.getHttpRequestItem().getUrl(), "https://example.test/legacy");
    }

    private static boolean exposesType(Class<?> owner, Class<?> forbiddenType) {
        for (Executable constructor : owner.getConstructors()) {
            if (java.util.Arrays.asList(constructor.getParameterTypes()).contains(forbiddenType)) {
                return true;
            }
        }
        for (Method method : owner.getMethods()) {
            if (method.getReturnType().equals(forbiddenType)
                    || java.util.Arrays.asList(method.getParameterTypes()).contains(forbiddenType)) {
                return true;
            }
        }
        return false;
    }

    private static HttpRequestItem request(String id, String url) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(id);
        item.setProtocol(RequestItemProtocolEnum.HTTP);
        item.setMethod("GET");
        item.setUrl(url);
        return item;
    }
}
