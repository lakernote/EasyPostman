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
                .build());
        String savedJson = Files.readString(configPath, StandardCharsets.UTF_8);

        PerformancePlanConfiguration loaded = storage.loadConfiguration(configPath);

        assertNotNull(loaded);
        assertFalse(loaded.isEfficientMode());
        assertFalse(loaded.isTrendEnabled());
        assertTrue(loaded.isReportRealtimeEnabled());
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
    public void shouldHydrateLegacyRequestsFromWorkspaceCollectionsWhenLoading() throws Exception {
        Path workspaceDir = Files.createTempDirectory("performance-plan-storage-legacy");
        Path configPath = workspaceDir.resolve("performance_config.json");
        Files.writeString(workspaceDir.resolve("collections.json"), """
                [
                  {
                    "type": "group",
                    "name": "Default Group",
                    "children": [
                      {
                        "type": "request",
                        "data": {
                          "id": "legacy-id",
                          "name": "Legacy By Id",
                          "url": "https://example.test/by-id",
                          "method": "POST",
                          "protocol": "HTTP"
                        }
                      },
                      {
                        "type": "request",
                        "data": {
                          "id": "legacy-name-id",
                          "name": "Legacy Unique Name",
                          "url": "https://example.test/by-name",
                          "method": "GET",
                          "protocol": "HTTP"
                        }
                      }
                    ]
                  }
                ]
                """, StandardCharsets.UTF_8);
        Files.writeString(configPath, """
                {
                  "version": "1.0",
                  "efficientMode": true,
                  "trendEnabled": true,
                  "tree": {
                    "name": "Plan",
                    "type": "ROOT",
                    "enabled": true,
                    "children": [
                      {
                        "name": "Users",
                        "type": "THREAD_GROUP",
                        "enabled": true,
                        "children": [
                          {
                            "name": "Legacy By Id",
                            "type": "REQUEST",
                            "enabled": true,
                            "requestItemId": "legacy-id",
                            "requestInheritanceSnapshot": false
                          },
                          {
                            "name": "Legacy Unique Name",
                            "type": "REQUEST",
                            "enabled": true,
                            "requestInheritanceSnapshot": false
                          }
                        ]
                      }
                    ]
                  }
                }
                """, StandardCharsets.UTF_8);

        PerformancePlanConfiguration loaded = new PerformancePlanStorage().loadConfiguration(configPath);

        List<PerformancePlanNode> requests = loaded.getPlanDocument().getRoot().getChildren().get(0).getChildren();
        assertEquals(requests.get(0).getHttpRequestItem().getId(), "legacy-id");
        assertEquals(requests.get(0).getHttpRequestItem().getUrl(), "https://example.test/by-id");
        assertEquals(requests.get(1).getHttpRequestItem().getId(), "legacy-name-id");
        assertEquals(requests.get(1).getHttpRequestItem().getUrl(), "https://example.test/by-name");
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
