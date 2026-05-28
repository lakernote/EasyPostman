package com.laker.postman.service;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.Workspace;
import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.plan.PerformanceCsvState;
import com.laker.postman.panel.performance.plan.PerformancePlanDocument;
import com.laker.postman.panel.performance.plan.PerformancePlanConfiguration;
import com.laker.postman.panel.performance.plan.PerformancePlanNode;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.timer.TimerData;
import com.laker.postman.service.collections.CollectionTreeRootRegistry;
import com.laker.postman.service.variable.RequestExecutionScope;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.laker.postman.service.collections.CollectionTreeNodeTypes.REQUEST;
import static org.testng.Assert.*;

public class PerformancePersistenceServiceTest {

    @Test(description = "默认配置路径应跟随当前工作区，便于 Git 工作区同步性能测试方案")
    public void shouldResolveConfigPathFromCurrentWorkspace() throws IOException {
        Path workspaceDir = Files.createTempDirectory("performance-workspace-path");
        Workspace workspace = new Workspace();
        workspace.setPath(workspaceDir.toString() + java.io.File.separator);

        WorkspaceAwarePerformancePersistenceService service = new WorkspaceAwarePerformancePersistenceService(workspace);

        assertEquals(service.getConfigFilePath(), workspaceDir.resolve("performance_config.json"));
    }

    @Test(description = "保存性能测试配置时应真实写入当前工作区，便于 Git 同步")
    public void shouldSaveConfigFileIntoCurrentWorkspace() throws IOException {
        Path workspaceDir = Files.createTempDirectory("performance-workspace-save");
        Workspace workspace = new Workspace();
        workspace.setPath(workspaceDir.toString());
        WorkspaceAwarePerformancePersistenceService service = new WorkspaceAwarePerformancePersistenceService(workspace);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));

        CsvDataPanel.CsvState csvState = new CsvDataPanel.CsvState(
                "workspace-users.csv",
                List.of("username", "password"),
                List.of(row("username", "alice", "password", "secret"))
        );

        service.save(root, false, csvState);

        Path configPath = workspaceDir.resolve("performance_config.json");
        assertTrue(Files.exists(configPath));
        assertFalse(Files.readString(configPath).contains("responseBodyPreviewLimitKb"));

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        assertNotNull(loadedRoot);
        assertEquals(((JMeterTreeNode) loadedRoot.getUserObject()).name, "Loaded Plan");
        assertFalse(service.loadEfficientMode());
        CsvDataPanel.CsvState loadedCsvState = service.loadCsvState();
        assertNotNull(loadedCsvState);
        assertEquals(loadedCsvState.getSourceName(), "workspace-users.csv");
        assertEquals(loadedCsvState.getHeaders(), List.of("username", "password"));
        assertEquals(loadedCsvState.getRows().get(0).get("username"), "alice");
    }

    @Test(description = "异步保存应固定调度时的工作区，避免切换工作区后写错位置")
    public void shouldSaveAsyncToWorkspaceActiveWhenScheduled() throws Exception {
        Path workspaceA = Files.createTempDirectory("performance-workspace-async-a");
        Path workspaceB = Files.createTempDirectory("performance-workspace-async-b");
        BlockingWorkspacePerformancePersistenceService service =
                new BlockingWorkspacePerformancePersistenceService(workspace(workspaceA));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));

        CsvDataPanel.CsvState csvState = new CsvDataPanel.CsvState(
                "async-users.csv",
                List.of("username"),
                List.of(row("username", "bob"))
        );

        service.saveAsync(root, false, csvState);
        if (service.awaitWorkerWorkspaceLookup()) {
            service.setWorkspace(workspace(workspaceB));
            service.releaseWorkerWorkspaceLookup();
        } else {
            service.setWorkspace(workspace(workspaceB));
        }

        Path configInA = workspaceA.resolve("performance_config.json");
        Path configInB = workspaceB.resolve("performance_config.json");
        assertTrue(awaitExists(configInA), "performance_config.json should be saved in the source workspace");
        assertFalse(Files.exists(configInB), "performance_config.json must not be written to the target workspace");
        assertTrue(Files.readString(configInA).contains("async-users.csv"));
    }

    @Test(description = "默认工作区迁移旧性能测试配置后应移除工作区外的旧文件")
    public void shouldMoveLegacyConfigIntoDefaultWorkspace() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-legacy-move");
        Path legacyPath = tempDir.resolve("performance_config.json");
        Path workspaceDir = tempDir.resolve("workspaces").resolve("default");
        Files.createDirectories(workspaceDir);
        Files.writeString(legacyPath, "{\"version\":\"1.0\",\"tree\":{\"name\":\"Plan\",\"type\":\"ROOT\",\"enabled\":true}}");

        Workspace defaultWorkspace = workspace(workspaceDir);
        defaultWorkspace.setId("default-workspace");
        LegacyAwarePerformancePersistenceService service =
                new LegacyAwarePerformancePersistenceService(defaultWorkspace, legacyPath);

        Path configPath = service.getConfigFilePath();

        assertEquals(configPath, workspaceDir.resolve("performance_config.json"));
        assertTrue(Files.exists(configPath));
        assertFalse(Files.exists(legacyPath));
    }

    @Test(description = "默认工作区已有性能测试配置时应清理工作区外旧文件")
    public void shouldRemoveLegacyConfigWhenWorkspaceConfigAlreadyExists() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-legacy-clean");
        Path legacyPath = tempDir.resolve("performance_config.json");
        Path workspaceDir = tempDir.resolve("workspaces").resolve("default");
        Path workspaceConfigPath = workspaceDir.resolve("performance_config.json");
        Files.createDirectories(workspaceDir);
        Files.writeString(legacyPath, "{\"version\":\"1.0\",\"tree\":{\"name\":\"Legacy\",\"type\":\"ROOT\",\"enabled\":true}}");
        Files.writeString(workspaceConfigPath, "{\"version\":\"1.0\",\"tree\":{\"name\":\"Workspace\",\"type\":\"ROOT\",\"enabled\":true}}");

        Workspace defaultWorkspace = workspace(workspaceDir);
        defaultWorkspace.setId("default-workspace");
        LegacyAwarePerformancePersistenceService service =
                new LegacyAwarePerformancePersistenceService(defaultWorkspace, legacyPath);

        Path configPath = service.getConfigFilePath();

        assertEquals(configPath, workspaceConfigPath);
        assertTrue(Files.exists(workspaceConfigPath));
        assertFalse(Files.exists(legacyPath));
    }

    @Test(description = "应支持保存并恢复 PerformancePanel CSV 快照")
    public void shouldPersistAndLoadCsvState() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-test");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        CsvDataPanel.CsvState csvState = new CsvDataPanel.CsvState(
                "users.csv",
                List.of("username", "password"),
                List.of(row("username", "alice", "password", "secret"))
        );

        service.save(root, false, csvState);

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        CsvDataPanel.CsvState loadedCsvState = service.loadCsvState();

        assertNotNull(loadedRoot);
        assertEquals(((JMeterTreeNode) loadedRoot.getUserObject()).name, "Loaded Plan");
        assertNotNull(loadedCsvState);
        assertEquals(loadedCsvState.getSourceName(), "users.csv");
        assertEquals(loadedCsvState.getHeaders(), List.of("username", "password"));
        assertEquals(loadedCsvState.getRows().size(), 1);
        assertEquals(loadedCsvState.getRows().get(0).get("username"), "alice");
        assertEquals(loadedCsvState.getRows().get(0).get("password"), "secret");
        assertFalse(service.loadEfficientMode());
    }

    @Test(description = "应保存并恢复 Thread Group 下的 CSV Data Set 节点")
    public void shouldPersistCsvDataSetNodeUnderThreadGroup() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-csv-data-set-node");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(new JMeterTreeNode("Thread Group", NodeType.THREAD_GROUP, new ThreadGroupData()));
        JMeterTreeNode csvNodeData = new JMeterTreeNode("CSV Data Set", NodeType.CSV_DATA_SET);
        csvNodeData.csvDataSetData = new CsvDataSetData(
                "users-1-300.csv",
                List.of("userId", "roomId"),
                List.of(row("userId", "u1", "roomId", "1728"))
        );
        group.add(new DefaultMutableTreeNode(csvNodeData));
        root.add(group);

        service.save(root, true, true, false);

        String json = Files.readString(configPath, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"type\": \"CSV_DATA_SET\""));
        assertTrue(json.contains("\"csvDataSetData\""));
        assertFalse(json.contains("\"csvState\""));

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        DefaultMutableTreeNode loadedGroup = (DefaultMutableTreeNode) loadedRoot.getChildAt(0);
        DefaultMutableTreeNode loadedCsvNode = (DefaultMutableTreeNode) loadedGroup.getChildAt(0);
        JMeterTreeNode loadedCsvData = (JMeterTreeNode) loadedCsvNode.getUserObject();

        assertEquals(loadedCsvData.type, NodeType.CSV_DATA_SET);
        assertEquals(loadedCsvData.csvDataSetData.getSourceName(), "users-1-300.csv");
        assertEquals(loadedCsvData.csvDataSetData.getHeaders(), List.of("userId", "roomId"));
        assertEquals(loadedCsvData.csvDataSetData.getRows().get(0).get("roomId"), "1728");
    }

    @Test(description = "应保存并恢复趋势采样开关")
    public void shouldPersistAndLoadTrendEnabled() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-trend-enabled");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        service.save(root, true, false, null);

        assertFalse(service.loadTrendEnabled());
        assertTrue(Files.readString(configPath, StandardCharsets.UTF_8).contains("\"trendEnabled\": false"));
    }

    @Test(description = "应保存并恢复报表实时刷新开关，默认结束后生成")
    public void shouldPersistAndLoadRealtimeReportRefresh() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-report-refresh");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        service.save(root, true, true, true, null);

        assertTrue(service.loadReportRealtimeEnabled());
        assertTrue(Files.readString(configPath, StandardCharsets.UTF_8).contains("\"reportRealtimeEnabled\": true"));
    }

    @Test(description = "应通过已注册的 Collection 树根节点按 ID 查找请求并返回深拷贝")
    public void shouldFindRequestItemFromRegisteredCollectionTreeRoot() {
        HttpRequestItem requestItem = new HttpRequestItem();
        requestItem.setId("req-registered");
        requestItem.setName("registered");
        requestItem.setUrl("https://example.com/registered");
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        rootNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, requestItem}));

        try {
            CollectionTreeRootRegistry.registerRootSupplier(() -> rootNode);

            HttpRequestItem result = new PerformancePersistenceService().findRequestItemById("req-registered");

            assertNotSame(result, requestItem);
            assertEquals(result.getId(), requestItem.getId());
            assertEquals(result.getUrl(), requestItem.getUrl());
        } finally {
            CollectionTreeRootRegistry.clear();
        }
    }

    @Test(description = "应支持通过纯计划文档保存请求快照，便于无集合树的压测运行器恢复配置")
    public void shouldSaveAndLoadPlanDocumentWithRequestSnapshotWithoutCollectionTree() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-document");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        HttpRequestItem requestItem = new HttpRequestItem();
        requestItem.setId("snapshot-request");
        requestItem.setName("Snapshot WebSocket");
        requestItem.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
        requestItem.setUrl("wss://example.test/ws");

        WebSocketPerformanceData webSocketData = new WebSocketPerformanceData();
        webSocketData.connectTimeoutMs = 4321;

        PerformancePlanDocument document = new PerformancePlanDocument(
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
                                                        .name("Snapshot WebSocket")
                                                        .type(NodeType.REQUEST)
                                                        .httpRequestItem(requestItem)
                                                        .webSocketPerformanceData(webSocketData)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );

        service.saveDocument(document, true, true, false, null);
        CollectionTreeRootRegistry.clear();

        PerformancePlanDocument loadedDocument = service.loadDocument();

        assertNotNull(loadedDocument);
        PerformancePlanNode loadedRequest = loadedDocument.getRoot().getChildren().get(0).getChildren().get(0);
        assertEquals(loadedRequest.getHttpRequestItem().getId(), "snapshot-request");
        assertEquals(loadedRequest.getHttpRequestItem().getUrl(), "wss://example.test/ws");
        assertEquals(loadedRequest.getHttpRequestItem().getProtocol(), RequestItemProtocolEnum.WEBSOCKET);
        assertEquals(loadedRequest.getWebSocketPerformanceData().connectTimeoutMs, 4321);

        String json = Files.readString(configPath, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"requestSnapshot\""));
        assertFalse(json.contains("\"requestItem\""));
        assertFalse(json.contains("\"requestItemId\""));
        assertTrue(json.contains("wss://example.test/ws"));
    }

    @Test(description = "应支持一次性保存并加载无界面压测运行所需的完整配置包")
    public void shouldSaveAndLoadPerformanceConfigurationBundle() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-bundle");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.numThreads = 3;
        threadGroupData.loops = 2;

        HttpRequestItem requestItem = new HttpRequestItem();
        requestItem.setId("bundle-request");
        requestItem.setName("Bundle HTTP");
        requestItem.setProtocol(RequestItemProtocolEnum.HTTP);
        requestItem.setUrl("https://example.test/bundle");

        PerformancePlanDocument document = new PerformancePlanDocument(
                PerformancePlanNode.builder()
                        .name("Bundle Plan")
                        .type(NodeType.ROOT)
                        .children(List.of(
                                PerformancePlanNode.builder()
                                        .name("Bundle Users")
                                        .type(NodeType.THREAD_GROUP)
                                        .threadGroupData(threadGroupData)
                                        .children(List.of(
                                                PerformancePlanNode.builder()
                                                        .name("Bundle HTTP")
                                                        .type(NodeType.REQUEST)
                                                        .httpRequestItem(requestItem)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );
        PerformanceCsvState csvState = new PerformanceCsvState(
                "bundle-users.csv",
                List.of("userId", "roomId"),
                List.of(row("userId", "user-1", "roomId", "room-9"))
        );

        service.saveConfiguration(PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(false)
                .trendEnabled(false)
                .reportRealtimeEnabled(true)
                .csvState(csvState)
                .build());

        PerformancePlanConfiguration loadedConfiguration = service.loadConfiguration();

        assertNotNull(loadedConfiguration);
        assertFalse(loadedConfiguration.isEfficientMode());
        assertFalse(loadedConfiguration.isTrendEnabled());
        assertTrue(loadedConfiguration.isReportRealtimeEnabled());
        assertNotNull(loadedConfiguration.getCsvState());
        assertEquals(loadedConfiguration.getCsvState().getSourceName(), "bundle-users.csv");
        assertEquals(loadedConfiguration.getCsvState().getRows().get(0).get("roomId"), "room-9");

        PerformancePlanNode loadedThreadGroup = loadedConfiguration.getPlanDocument().getRoot().getChildren().get(0);
        PerformancePlanNode loadedRequest = loadedThreadGroup.getChildren().get(0);
        assertEquals(loadedConfiguration.getPlanDocument().getRoot().getName(), "Bundle Plan");
        assertEquals(loadedThreadGroup.getThreadGroupData().numThreads, 3);
        assertEquals(loadedThreadGroup.getThreadGroupData().loops, 2);
        assertEquals(loadedRequest.getHttpRequestItem().getId(), "bundle-request");
        assertEquals(loadedRequest.getHttpRequestItem().getUrl(), "https://example.test/bundle");

        assertFalse(service.loadEfficientMode());
        assertFalse(service.loadTrendEnabled());
        assertTrue(service.loadReportRealtimeEnabled());
        CsvDataPanel.CsvState loadedLegacyCsvState = service.loadCsvState();
        assertNotNull(loadedLegacyCsvState);
        assertEquals(loadedLegacyCsvState.getSourceName(), "bundle-users.csv");
        assertEquals(loadedLegacyCsvState.getRows().get(0).get("userId"), "user-1");

        DefaultMutableTreeNode loadedTree = service.load("Loaded Bundle");
        assertNotNull(loadedTree);
        assertEquals(((JMeterTreeNode) loadedTree.getUserObject()).name, "Loaded Bundle");
    }

    @Test(description = "应保存并恢复请求执行作用域，确保无界面运行器不依赖集合树也能解析分组变量")
    public void shouldSaveAndLoadRequestExecutionScopeInPlanDocument() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-scope");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        HttpRequestItem requestItem = new HttpRequestItem();
        requestItem.setId("scoped-request");
        requestItem.setName("Scoped HTTP");
        requestItem.setProtocol(RequestItemProtocolEnum.HTTP);
        requestItem.setUrl("https://example.test/scope");

        PerformancePlanDocument document = new PerformancePlanDocument(
                PerformancePlanNode.builder()
                        .name("Scoped Plan")
                        .type(NodeType.ROOT)
                        .children(List.of(
                                PerformancePlanNode.builder()
                                        .name("Scoped Users")
                                        .type(NodeType.THREAD_GROUP)
                                        .threadGroupData(new ThreadGroupData())
                                        .children(List.of(
                                                PerformancePlanNode.builder()
                                                        .name("Scoped HTTP")
                                                        .type(NodeType.REQUEST)
                                                        .httpRequestItem(requestItem)
                                                        .requestExecutionScope(RequestExecutionScope.fromGroupVariables(
                                                                Map.of("tenantId", "persisted-tenant")
                                                        ))
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );

        service.saveDocument(document, true, true, false, null);

        PerformancePlanDocument loadedDocument = service.loadDocument();

        PerformancePlanNode loadedRequest = loadedDocument.getRoot().getChildren().get(0).getChildren().get(0);
        assertEquals(loadedRequest.getRequestExecutionScope().getGroupVariable("tenantId"), "persisted-tenant");
    }

    @Test(description = "旧版不含 csvState 的配置仍应兼容加载")
    public void shouldLoadLegacyConfigWithoutCsvState() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-legacy");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        Files.writeString(configPath, """
                {
                  "version": "1.0",
                  "efficientMode": true,
                  "tree": {
                    "name": "Plan",
                    "type": "ROOT",
                    "enabled": true
                  }
                }
                """, StandardCharsets.UTF_8);

        DefaultMutableTreeNode loadedRoot = service.load("Legacy Plan");

        assertNotNull(loadedRoot);
        assertNull(service.loadCsvState());
        assertTrue(service.loadEfficientMode());
        assertTrue(service.loadTrendEnabled());
        assertFalse(service.loadReportRealtimeEnabled());
        assertEquals(((JMeterTreeNode) loadedRoot.getUserObject()).name, "Legacy Plan");
    }

    @Test(description = "空 CSV 状态不应写出脏数据")
    public void shouldKeepCsvStateEmptyWhenNotProvided() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-empty");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        service.save(root, true, null);

        String json = Files.readString(configPath, StandardCharsets.UTF_8);
        assertFalse(json.contains("\"csvState\""));
        assertNull(service.loadCsvState());
    }

    @Test(description = "应保存并恢复 WebSocket 发送和等待步骤的独立配置")
    public void shouldPersistWebSocketStepConfigs() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-ws-step");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new JMeterTreeNode("WebSocket Example", NodeType.REQUEST));

        JMeterTreeNode sendNode = new JMeterTreeNode("WS Send", NodeType.WS_SEND);
        sendNode.webSocketPerformanceData = new WebSocketPerformanceData();
        sendNode.webSocketPerformanceData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_ON_CONNECT;
        sendNode.webSocketPerformanceData.sendContentSource = WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT;
        sendNode.webSocketPerformanceData.customSendBody = "a-{{user-a}}";
        sendNode.webSocketPerformanceData.sendPreScript = "pm.variables.set('a', 'dynamic');";
        requestNode.add(new DefaultMutableTreeNode(sendNode));

        JMeterTreeNode readNode = new JMeterTreeNode("WS Read", NodeType.WS_READ);
        readNode.webSocketPerformanceData = new WebSocketPerformanceData();
        readNode.webSocketPerformanceData.completionMode = WebSocketPerformanceData.CompletionMode.UNTIL_MATCH;
        readNode.webSocketPerformanceData.firstMessageTimeoutMs = 10000;
        readNode.webSocketPerformanceData.messageFilter = "a";
        requestNode.add(new DefaultMutableTreeNode(readNode));

        root.add(requestNode);

        service.save(root, false, null);

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        DefaultMutableTreeNode loadedRequestNode = (DefaultMutableTreeNode) loadedRoot.getChildAt(0);
        JMeterTreeNode loadedSendNode = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedRequestNode.getChildAt(0)).getUserObject();
        JMeterTreeNode loadedReadNode = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedRequestNode.getChildAt(1)).getUserObject();

        assertNotNull(loadedSendNode.webSocketPerformanceData);
        assertEquals(loadedSendNode.webSocketPerformanceData.sendContentSource,
                WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT);
        assertEquals(loadedSendNode.webSocketPerformanceData.customSendBody, "a-{{user-a}}");
        assertEquals(loadedSendNode.webSocketPerformanceData.sendPreScript, "pm.variables.set('a', 'dynamic');");
        assertNotNull(loadedReadNode.webSocketPerformanceData);
        assertEquals(loadedReadNode.webSocketPerformanceData.completionMode,
                WebSocketPerformanceData.CompletionMode.UNTIL_MATCH);
        assertEquals(loadedReadNode.webSocketPerformanceData.messageFilter, "a");
    }

    @Test(description = "应保存并恢复 SSE 阶段节点的独立配置")
    public void shouldPersistSseStageConfigs() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-sse-stage");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new JMeterTreeNode("SSE Example", NodeType.REQUEST));

        JMeterTreeNode connectNode = new JMeterTreeNode("SSE Connect", NodeType.SSE_CONNECT);
        connectNode.ssePerformanceData = new SsePerformanceData();
        connectNode.ssePerformanceData.connectTimeoutMs = 3456;
        requestNode.add(new DefaultMutableTreeNode(connectNode));

        JMeterTreeNode readNode = new JMeterTreeNode("SSE Read", NodeType.SSE_READ);
        readNode.ssePerformanceData = new SsePerformanceData();
        readNode.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.STREAM_CLOSED;
        readNode.ssePerformanceData.holdConnectionMs = 30000;
        requestNode.add(new DefaultMutableTreeNode(readNode));

        root.add(requestNode);

        service.save(root, false, null);

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        DefaultMutableTreeNode loadedRequestNode = (DefaultMutableTreeNode) loadedRoot.getChildAt(0);
        JMeterTreeNode loadedConnectNode = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedRequestNode.getChildAt(0)).getUserObject();
        JMeterTreeNode loadedReadNode = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedRequestNode.getChildAt(1)).getUserObject();

        assertNotNull(loadedConnectNode.ssePerformanceData);
        assertEquals(loadedConnectNode.ssePerformanceData.connectTimeoutMs, 3456);
        assertNotNull(loadedReadNode.ssePerformanceData);
        assertEquals(loadedReadNode.ssePerformanceData.completionMode,
                SsePerformanceData.CompletionMode.STREAM_CLOSED);
        assertEquals(loadedReadNode.ssePerformanceData.holdConnectionMs, 30000);
    }

    @Test(description = "WebSocket 读取配置遇到未知枚举值时应使用默认值，并继续读取其它字段")
    public void shouldDefaultUnknownWebSocketReadEnumsWhenLoading() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-ws-defaults");
        Path configPath = tempDir.resolve("performance_config.json");
        Files.writeString(configPath, """
                {
                  "version": "1.0",
                  "tree": {
                    "name": "Plan",
                    "type": "ROOT",
                    "enabled": true,
                    "children": [
                      {
                        "name": "WebSocket Example",
                        "type": "REQUEST",
                        "enabled": true,
                        "webSocketPerformanceData": {
                          "sendMode": "LEGACY_SEND",
                          "sendContentSource": "LEGACY_BODY",
                          "completionMode": "MATCHED_MESSAGE",
                          "firstMessageTimeoutMs": 2222,
                          "targetMessageCount": 3,
                          "messageFilter": "ack"
                        }
                      }
                    ]
                  }
                }
                """, StandardCharsets.UTF_8);
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        DefaultMutableTreeNode loadedRequestNode = (DefaultMutableTreeNode) loadedRoot.getChildAt(0);
        JMeterTreeNode loadedRequest = (JMeterTreeNode) loadedRequestNode.getUserObject();

        assertEquals(loadedRequest.webSocketPerformanceData.sendMode,
                WebSocketPerformanceData.SendMode.REQUEST_BODY_ON_CONNECT);
        assertEquals(loadedRequest.webSocketPerformanceData.sendContentSource,
                WebSocketPerformanceData.SendContentSource.REQUEST_BODY);
        assertEquals(loadedRequest.webSocketPerformanceData.completionMode,
                WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE);
        assertEquals(loadedRequest.webSocketPerformanceData.firstMessageTimeoutMs, 2222);
        assertEquals(loadedRequest.webSocketPerformanceData.targetMessageCount, 3);
        assertEquals(loadedRequest.webSocketPerformanceData.messageFilter, "ack");
    }

    @Test(description = "SSE Read 阶段配置遇到旧枚举值时应使用默认值，并继续读取其它字段")
    public void shouldDefaultLegacySseReadEnumsWhenLoading() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-sse-defaults");
        Path configPath = tempDir.resolve("performance_config.json");
        Files.writeString(configPath, """
                {
                  "version": "1.0",
                  "tree": {
                    "name": "Plan",
                    "type": "ROOT",
                    "enabled": true,
                    "children": [
                      {
                        "name": "SSE Example",
                        "type": "REQUEST",
                        "enabled": true,
                        "children": [
                          {
                            "name": "SSE Read",
                            "type": "SSE_READ",
                            "enabled": true,
                            "ssePerformanceData": {
                              "completionMode": "FIRST_MESSAGE",
                              "firstMessageTimeoutMs": 2222,
                              "targetMessageCount": 3,
                              "eventNameFilter": "orders",
                              "messageFilter": "ready"
                            }
                          }
                        ]
                      }
                    ]
                  }
                }
                """, StandardCharsets.UTF_8);
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        DefaultMutableTreeNode loadedRequestNode = (DefaultMutableTreeNode) loadedRoot.getChildAt(0);
        JMeterTreeNode loadedRead = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedRequestNode.getChildAt(0)).getUserObject();

        assertEquals(loadedRead.ssePerformanceData.completionMode,
                SsePerformanceData.CompletionMode.SINGLE_MESSAGE);
        assertEquals(loadedRead.ssePerformanceData.firstMessageTimeoutMs, 2222);
        assertEquals(loadedRead.ssePerformanceData.targetMessageCount, 3);
        assertEquals(loadedRead.ssePerformanceData.eventNameFilter, "orders");
        assertEquals(loadedRead.ssePerformanceData.messageFilter, "ready");
    }

    @Test(description = "应保存并恢复 WebSocket Connect 步骤的独立配置")
    public void shouldPersistWebSocketConnectStepConfig() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-ws-connect-step");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new JMeterTreeNode("WebSocket Example", NodeType.REQUEST));

        JMeterTreeNode connectNode = new JMeterTreeNode("WS Connect", NodeType.WS_CONNECT);
        connectNode.webSocketPerformanceData = new WebSocketPerformanceData();
        connectNode.webSocketPerformanceData.connectTimeoutMs = 12345;
        requestNode.add(new DefaultMutableTreeNode(connectNode));
        root.add(requestNode);

        service.save(root, false, null);

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        DefaultMutableTreeNode loadedRequestNode = (DefaultMutableTreeNode) loadedRoot.getChildAt(0);
        JMeterTreeNode loadedConnectNode = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedRequestNode.getChildAt(0)).getUserObject();

        assertNotNull(loadedConnectNode.webSocketPerformanceData);
        assertEquals(loadedConnectNode.webSocketPerformanceData.connectTimeoutMs, 12345);
    }

    @Test(description = "应保存并恢复通用 Loop 控制器配置")
    public void shouldPersistLoopControllerConfig() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-loop");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new JMeterTreeNode("Users", NodeType.THREAD_GROUP));
        JMeterTreeNode loop = new JMeterTreeNode("Loop [4x]", NodeType.LOOP);
        loop.loopData = new LoopData();
        loop.loopData.iterations = 4;
        DefaultMutableTreeNode loopNode = new DefaultMutableTreeNode(loop);
        groupNode.add(loopNode);
        root.add(groupNode);

        service.save(root, false, null);

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        DefaultMutableTreeNode loadedGroupNode = (DefaultMutableTreeNode) loadedRoot.getChildAt(0);
        JMeterTreeNode loadedLoop = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedGroupNode.getChildAt(0)).getUserObject();

        assertNotNull(loadedLoop.loopData);
        assertEquals(loadedLoop.loopData.iterations, 4);
    }

    @Test(description = "应保存并恢复所有性能节点配置对象")
    public void shouldPersistAllPerformanceNodeConfigs() throws IOException {
        Path tempDir = Files.createTempDirectory("performance-persistence-all-nodes");
        Path configPath = tempDir.resolve("performance_config.json");
        TestablePerformancePersistenceService service = new TestablePerformancePersistenceService(configPath);
        service.init();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));

        JMeterTreeNode threadGroup = new JMeterTreeNode("Users", NodeType.THREAD_GROUP);
        threadGroup.threadGroupData = new ThreadGroupData();
        threadGroup.threadGroupData.numThreads = 7;
        threadGroup.threadGroupData.loops = 3;
        DefaultMutableTreeNode threadGroupNode = new DefaultMutableTreeNode(threadGroup);
        root.add(threadGroupNode);

        DefaultMutableTreeNode sseRequestNode = new DefaultMutableTreeNode(new JMeterTreeNode("SSE Request", NodeType.REQUEST));
        JMeterTreeNode sseConnect = new JMeterTreeNode("SSE Connect", NodeType.SSE_CONNECT);
        sseConnect.ssePerformanceData = new SsePerformanceData();
        sseConnect.ssePerformanceData.connectTimeoutMs = 4321;
        sseRequestNode.add(new DefaultMutableTreeNode(sseConnect));
        JMeterTreeNode sseRead = new JMeterTreeNode("SSE Read", NodeType.SSE_READ);
        sseRead.ssePerformanceData = new SsePerformanceData();
        sseRead.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.MESSAGE_COUNT;
        sseRead.ssePerformanceData.firstMessageTimeoutMs = 1234;
        sseRead.ssePerformanceData.holdConnectionMs = 5678;
        sseRead.ssePerformanceData.targetMessageCount = 9;
        sseRead.ssePerformanceData.eventNameFilter = "orders";
        sseRead.ssePerformanceData.messageFilter = "done";
        sseRequestNode.add(new DefaultMutableTreeNode(sseRead));
        threadGroupNode.add(sseRequestNode);

        JMeterTreeNode wsRequest = new JMeterTreeNode("WS Request", NodeType.REQUEST);
        wsRequest.webSocketPerformanceData = new WebSocketPerformanceData();
        wsRequest.webSocketPerformanceData.connectTimeoutMs = 4321;
        DefaultMutableTreeNode wsRequestNode = new DefaultMutableTreeNode(wsRequest);
        wsRequestNode.add(new DefaultMutableTreeNode(new JMeterTreeNode("WS Connect", NodeType.WS_CONNECT)));

        JMeterTreeNode assertion = new JMeterTreeNode("Status Assertion", NodeType.ASSERTION);
        assertion.assertionData = new AssertionData();
        assertion.assertionData.type = "Response Code";
        assertion.assertionData.operator = "=";
        assertion.assertionData.value = "200";
        wsRequestNode.add(new DefaultMutableTreeNode(assertion));

        JMeterTreeNode extractor = new JMeterTreeNode("Token Extractor", NodeType.EXTRACTOR);
        extractor.extractorData = new ExtractorData();
        extractor.extractorData.type = "JSONPath";
        extractor.extractorData.expression = "$.token";
        extractor.extractorData.variableName = "token";
        extractor.extractorData.defaultValue = "missing";
        wsRequestNode.add(new DefaultMutableTreeNode(extractor));

        JMeterTreeNode timer = new JMeterTreeNode("Think Time", NodeType.TIMER);
        timer.timerData = new TimerData();
        timer.timerData.delayMs = 250;
        wsRequestNode.add(new DefaultMutableTreeNode(timer));
        threadGroupNode.add(wsRequestNode);

        service.save(root, false, null);

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        DefaultMutableTreeNode loadedThreadGroupNode = (DefaultMutableTreeNode) loadedRoot.getChildAt(0);
        JMeterTreeNode loadedThreadGroup = (JMeterTreeNode) loadedThreadGroupNode.getUserObject();
        DefaultMutableTreeNode loadedSseRequestNode = (DefaultMutableTreeNode) loadedThreadGroupNode.getChildAt(0);
        JMeterTreeNode loadedSseConnect = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedSseRequestNode.getChildAt(0)).getUserObject();
        JMeterTreeNode loadedSseRead = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedSseRequestNode.getChildAt(1)).getUserObject();
        DefaultMutableTreeNode loadedWsRequestNode = (DefaultMutableTreeNode) loadedThreadGroupNode.getChildAt(1);
        JMeterTreeNode loadedWsRequest = (JMeterTreeNode) loadedWsRequestNode.getUserObject();
        JMeterTreeNode loadedAssertion = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedWsRequestNode.getChildAt(1)).getUserObject();
        JMeterTreeNode loadedExtractor = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedWsRequestNode.getChildAt(2)).getUserObject();
        JMeterTreeNode loadedTimer = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedWsRequestNode.getChildAt(3)).getUserObject();

        assertEquals(loadedThreadGroup.threadGroupData.numThreads, 7);
        assertEquals(loadedThreadGroup.threadGroupData.loops, 3);
        assertEquals(loadedSseConnect.ssePerformanceData.connectTimeoutMs, 4321);
        assertEquals(loadedSseRead.ssePerformanceData.completionMode, SsePerformanceData.CompletionMode.MESSAGE_COUNT);
        assertEquals(loadedSseRead.ssePerformanceData.firstMessageTimeoutMs, 1234);
        assertEquals(loadedSseRead.ssePerformanceData.holdConnectionMs, 5678);
        assertEquals(loadedSseRead.ssePerformanceData.targetMessageCount, 9);
        assertEquals(loadedSseRead.ssePerformanceData.eventNameFilter, "orders");
        assertEquals(loadedSseRead.ssePerformanceData.messageFilter, "done");
        assertEquals(loadedWsRequest.webSocketPerformanceData.connectTimeoutMs, 4321);
        assertEquals(loadedAssertion.assertionData.value, "200");
        assertEquals(loadedExtractor.extractorData.variableName, "token");
        assertEquals(loadedExtractor.extractorData.defaultValue, "missing");
        assertEquals(loadedTimer.timerData.delayMs, 250);
    }

    private static Map<String, String> row(String... keyValues) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put(keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    private static Workspace workspace(Path workspaceDir) {
        Workspace workspace = new Workspace();
        workspace.setPath(workspaceDir.toString());
        return workspace;
    }

    private static boolean awaitExists(Path path) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (Files.exists(path)) {
                return true;
            }
            Thread.sleep(20);
        }
        return Files.exists(path);
    }

    private static final class TestablePerformancePersistenceService extends PerformancePersistenceService {
        private final Path configPath;

        private TestablePerformancePersistenceService(Path configPath) {
            this.configPath = configPath;
        }

        @Override
        protected Path getConfigFilePath() {
            return configPath;
        }
    }

    private static final class WorkspaceAwarePerformancePersistenceService extends PerformancePersistenceService {
        private final Workspace workspace;

        private WorkspaceAwarePerformancePersistenceService(Workspace workspace) {
            this.workspace = workspace;
        }

        @Override
        protected Workspace getCurrentWorkspace() {
            return workspace;
        }
    }

    private static final class LegacyAwarePerformancePersistenceService extends PerformancePersistenceService {
        private final Workspace workspace;
        private final Path legacyConfigPath;

        private LegacyAwarePerformancePersistenceService(Workspace workspace, Path legacyConfigPath) {
            this.workspace = workspace;
            this.legacyConfigPath = legacyConfigPath;
        }

        @Override
        protected Workspace getCurrentWorkspace() {
            return workspace;
        }

        @Override
        protected Path getLegacyConfigFilePath() {
            return legacyConfigPath;
        }
    }

    private static final class BlockingWorkspacePerformancePersistenceService extends PerformancePersistenceService {
        private final Thread testThread = Thread.currentThread();
        private final CountDownLatch workerWorkspaceLookupStarted = new CountDownLatch(1);
        private final CountDownLatch releaseWorkerWorkspaceLookup = new CountDownLatch(1);
        private volatile Workspace workspace;

        private BlockingWorkspacePerformancePersistenceService(Workspace workspace) {
            this.workspace = workspace;
        }

        private void setWorkspace(Workspace workspace) {
            this.workspace = workspace;
        }

        private boolean awaitWorkerWorkspaceLookup() throws InterruptedException {
            return workerWorkspaceLookupStarted.await(200, TimeUnit.MILLISECONDS);
        }

        private void releaseWorkerWorkspaceLookup() {
            releaseWorkerWorkspaceLookup.countDown();
        }

        @Override
        protected Workspace getCurrentWorkspace() {
            if (Thread.currentThread() != testThread) {
                workerWorkspaceLookupStarted.countDown();
                try {
                    releaseWorkerWorkspaceLookup.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return workspace;
        }
    }
}
