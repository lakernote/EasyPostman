package com.laker.postman.service;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.timer.TimerData;
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

        JMeterTreeNode awaitNode = new JMeterTreeNode("WS Await", NodeType.WS_AWAIT);
        awaitNode.webSocketPerformanceData = new WebSocketPerformanceData();
        awaitNode.webSocketPerformanceData.completionMode = WebSocketPerformanceData.CompletionMode.MATCHED_MESSAGE;
        awaitNode.webSocketPerformanceData.firstMessageTimeoutMs = 10000;
        awaitNode.webSocketPerformanceData.messageFilter = "a";
        requestNode.add(new DefaultMutableTreeNode(awaitNode));

        root.add(requestNode);

        service.save(root, false, null);

        DefaultMutableTreeNode loadedRoot = service.load("Loaded Plan");
        DefaultMutableTreeNode loadedRequestNode = (DefaultMutableTreeNode) loadedRoot.getChildAt(0);
        JMeterTreeNode loadedSendNode = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedRequestNode.getChildAt(0)).getUserObject();
        JMeterTreeNode loadedAwaitNode = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedRequestNode.getChildAt(1)).getUserObject();

        assertNotNull(loadedSendNode.webSocketPerformanceData);
        assertEquals(loadedSendNode.webSocketPerformanceData.sendContentSource,
                WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT);
        assertEquals(loadedSendNode.webSocketPerformanceData.customSendBody, "a-{{user-a}}");
        assertEquals(loadedSendNode.webSocketPerformanceData.sendPreScript, "pm.variables.set('a', 'dynamic');");
        assertNotNull(loadedAwaitNode.webSocketPerformanceData);
        assertEquals(loadedAwaitNode.webSocketPerformanceData.completionMode,
                WebSocketPerformanceData.CompletionMode.MATCHED_MESSAGE);
        assertEquals(loadedAwaitNode.webSocketPerformanceData.messageFilter, "a");
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

        JMeterTreeNode sseRequest = new JMeterTreeNode("SSE Request", NodeType.REQUEST);
        sseRequest.ssePerformanceData = new SsePerformanceData();
        sseRequest.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.MESSAGE_COUNT;
        sseRequest.ssePerformanceData.firstMessageTimeoutMs = 1234;
        sseRequest.ssePerformanceData.holdConnectionMs = 5678;
        sseRequest.ssePerformanceData.targetMessageCount = 9;
        sseRequest.ssePerformanceData.eventNameFilter = "orders";
        sseRequest.ssePerformanceData.messageFilter = "done";
        DefaultMutableTreeNode sseRequestNode = new DefaultMutableTreeNode(sseRequest);
        sseRequestNode.add(new DefaultMutableTreeNode(new JMeterTreeNode("SSE Connect", NodeType.SSE_CONNECT)));
        sseRequestNode.add(new DefaultMutableTreeNode(new JMeterTreeNode("SSE Await", NodeType.SSE_AWAIT)));
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
        JMeterTreeNode loadedSseRequest = (JMeterTreeNode) loadedSseRequestNode.getUserObject();
        DefaultMutableTreeNode loadedWsRequestNode = (DefaultMutableTreeNode) loadedThreadGroupNode.getChildAt(1);
        JMeterTreeNode loadedWsRequest = (JMeterTreeNode) loadedWsRequestNode.getUserObject();
        JMeterTreeNode loadedAssertion = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedWsRequestNode.getChildAt(1)).getUserObject();
        JMeterTreeNode loadedTimer = (JMeterTreeNode) ((DefaultMutableTreeNode) loadedWsRequestNode.getChildAt(2)).getUserObject();

        assertEquals(loadedThreadGroup.threadGroupData.numThreads, 7);
        assertEquals(loadedThreadGroup.threadGroupData.loops, 3);
        assertEquals(loadedSseRequest.ssePerformanceData.completionMode, SsePerformanceData.CompletionMode.MESSAGE_COUNT);
        assertEquals(loadedSseRequest.ssePerformanceData.firstMessageTimeoutMs, 1234);
        assertEquals(loadedSseRequest.ssePerformanceData.holdConnectionMs, 5678);
        assertEquals(loadedSseRequest.ssePerformanceData.targetMessageCount, 9);
        assertEquals(loadedSseRequest.ssePerformanceData.eventNameFilter, "orders");
        assertEquals(loadedSseRequest.ssePerformanceData.messageFilter, "done");
        assertEquals(loadedWsRequest.webSocketPerformanceData.connectTimeoutMs, 4321);
        assertEquals(loadedAssertion.assertionData.value, "200");
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
