package com.laker.postman.service;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
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
