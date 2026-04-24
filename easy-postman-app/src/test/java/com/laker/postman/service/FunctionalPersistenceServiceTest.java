package com.laker.postman.service;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.functional.table.RunnerRowData;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

public class FunctionalPersistenceServiceTest {

    @Test(description = "功能测试配置路径应跟随当前工作区，便于 Git 工作区同步")
    public void shouldResolveConfigPathFromCurrentWorkspace() throws IOException {
        Path workspaceDir = Files.createTempDirectory("functional-workspace-path");
        Workspace workspace = new Workspace();
        workspace.setPath(workspaceDir.toString() + File.separator);

        WorkspaceAwareFunctionalPersistenceService service = new WorkspaceAwareFunctionalPersistenceService(workspace);

        assertEquals(service.getConfigFilePath(), workspaceDir.resolve("functional_config.json"));
    }

    @Test(description = "保存功能测试配置时应真实写入当前工作区，便于 Git 同步")
    public void shouldSaveConfigFileIntoCurrentWorkspace() throws IOException {
        Path workspaceDir = Files.createTempDirectory("functional-workspace-save");
        Workspace workspace = new Workspace();
        workspace.setPath(workspaceDir.toString());
        WorkspaceAwareFunctionalPersistenceService service = new WorkspaceAwareFunctionalPersistenceService(workspace);

        HttpRequestItem requestItem = requestItem("req-workspace", "workspace");
        RunnerRowData row = new RunnerRowData(requestItem, new PreparedRequest());
        service.stubRequest(requestItem);

        CsvDataPanel.CsvState csvState = new CsvDataPanel.CsvState(
                "workspace-users.csv",
                List.of("username", "password"),
                List.of(csvRow("username", "alice", "password", "secret"))
        );

        service.save(List.of(row), csvState);

        Path configPath = workspaceDir.resolve("functional_config.json");
        assertTrue(Files.exists(configPath));

        List<RunnerRowData> loadedRows = service.load();
        assertEquals(loadedRows.size(), 1);
        assertEquals(loadedRows.get(0).requestItem.getId(), "req-workspace");
        CsvDataPanel.CsvState loadedCsvState = service.loadCsvState();
        assertNotNull(loadedCsvState);
        assertEquals(loadedCsvState.getSourceName(), "workspace-users.csv");
        assertEquals(loadedCsvState.getHeaders(), List.of("username", "password"));
        assertEquals(loadedCsvState.getRows().get(0).get("username"), "alice");
    }

    @Test(description = "异步保存应固定调度时的工作区，避免切换工作区后写错位置")
    public void shouldSaveAsyncToWorkspaceActiveWhenScheduled() throws Exception {
        Path workspaceA = Files.createTempDirectory("functional-workspace-async-a");
        Path workspaceB = Files.createTempDirectory("functional-workspace-async-b");
        BlockingWorkspaceFunctionalPersistenceService service =
                new BlockingWorkspaceFunctionalPersistenceService(workspace(workspaceA));

        HttpRequestItem requestItem = requestItem("req-async", "async");
        RunnerRowData row = new RunnerRowData(requestItem, new PreparedRequest());

        CsvDataPanel.CsvState csvState = new CsvDataPanel.CsvState(
                "async-users.csv",
                List.of("username"),
                List.of(csvRow("username", "bob"))
        );

        service.saveAsync(List.of(row), csvState);
        if (service.awaitWorkerWorkspaceLookup()) {
            service.setWorkspace(workspace(workspaceB));
            service.releaseWorkerWorkspaceLookup();
        } else {
            service.setWorkspace(workspace(workspaceB));
        }

        Path configInA = workspaceA.resolve("functional_config.json");
        Path configInB = workspaceB.resolve("functional_config.json");
        assertTrue(awaitExists(configInA), "functional_config.json should be saved in the source workspace");
        assertFalse(Files.exists(configInB), "functional_config.json must not be written to the target workspace");
        assertTrue(Files.readString(configInA).contains("async-users.csv"));
    }

    @Test(description = "默认工作区迁移旧功能测试配置后应移除工作区外的旧文件")
    public void shouldMoveLegacyConfigIntoDefaultWorkspace() throws IOException {
        Path tempDir = Files.createTempDirectory("functional-legacy-move");
        Path legacyPath = tempDir.resolve("functional_config.json");
        Path workspaceDir = tempDir.resolve("workspaces").resolve("default");
        Files.createDirectories(workspaceDir);
        Files.writeString(legacyPath, "{\"version\":\"1.0\",\"rows\":[]}");

        Workspace defaultWorkspace = workspace(workspaceDir);
        defaultWorkspace.setId("default-workspace");
        LegacyAwareFunctionalPersistenceService service =
                new LegacyAwareFunctionalPersistenceService(defaultWorkspace, legacyPath);

        Path configPath = service.getConfigFilePath();

        assertEquals(configPath, workspaceDir.resolve("functional_config.json"));
        assertTrue(Files.exists(configPath));
        assertFalse(Files.exists(legacyPath));
    }

    @Test(description = "默认工作区已有功能测试配置时应清理工作区外旧文件")
    public void shouldRemoveLegacyConfigWhenWorkspaceConfigAlreadyExists() throws IOException {
        Path tempDir = Files.createTempDirectory("functional-legacy-clean");
        Path legacyPath = tempDir.resolve("functional_config.json");
        Path workspaceDir = tempDir.resolve("workspaces").resolve("default");
        Path workspaceConfigPath = workspaceDir.resolve("functional_config.json");
        Files.createDirectories(workspaceDir);
        Files.writeString(legacyPath, "{\"version\":\"1.0\",\"rows\":[]}");
        Files.writeString(workspaceConfigPath, "{\"version\":\"1.0\",\"rows\":[{\"selected\":true}]}");

        Workspace defaultWorkspace = workspace(workspaceDir);
        defaultWorkspace.setId("default-workspace");
        LegacyAwareFunctionalPersistenceService service =
                new LegacyAwareFunctionalPersistenceService(defaultWorkspace, legacyPath);

        Path configPath = service.getConfigFilePath();

        assertEquals(configPath, workspaceConfigPath);
        assertTrue(Files.exists(workspaceConfigPath));
        assertFalse(Files.exists(legacyPath));
    }

    @Test(description = "应支持保存并恢复 FunctionalPanel CSV 快照")
    public void shouldPersistAndLoadCsvState() throws IOException {
        Path tempDir = Files.createTempDirectory("functional-persistence-test");
        Path configPath = tempDir.resolve("functional_config.json");
        TestableFunctionalPersistenceService service = new TestableFunctionalPersistenceService(configPath);
        service.init();

        HttpRequestItem requestItem = requestItem("req-1", "login");
        RunnerRowData row = new RunnerRowData(requestItem, new PreparedRequest());
        row.selected = false;
        CsvDataPanel.CsvState csvState = new CsvDataPanel.CsvState(
                "users.csv",
                List.of("username", "password"),
                List.of(csvRow("username", "alice", "password", "secret"))
        );

        service.stubRequest(requestItem);
        service.save(List.of(row), csvState);

        List<RunnerRowData> loadedRows = service.load();
        CsvDataPanel.CsvState loadedCsvState = service.loadCsvState();

        assertEquals(loadedRows.size(), 1);
        assertEquals(loadedRows.get(0).requestItem.getId(), "req-1");
        assertFalse(loadedRows.get(0).selected);
        assertNotNull(loadedCsvState);
        assertEquals(loadedCsvState.getSourceName(), "users.csv");
        assertEquals(loadedCsvState.getHeaders(), List.of("username", "password"));
        assertEquals(loadedCsvState.getRows().get(0).get("username"), "alice");
    }

    private static HttpRequestItem requestItem(String id, String name) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        item.setUrl("https://example.com/" + name);
        item.setMethod("GET");
        return item;
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

    private static Map<String, String> csvRow(String... keyValues) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put(keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    private static final class TestableFunctionalPersistenceService extends FunctionalPersistenceService {
        private final Path configPath;
        private final Map<String, HttpRequestItem> requests = new LinkedHashMap<>();

        private TestableFunctionalPersistenceService(Path configPath) {
            this.configPath = configPath;
        }

        private void stubRequest(HttpRequestItem requestItem) {
            requests.put(requestItem.getId(), requestItem);
        }

        @Override
        public HttpRequestItem findRequestItemById(String requestId) {
            return requests.get(requestId);
        }

        @Override
        protected Path getConfigFilePath() {
            return configPath;
        }
    }

    private static final class WorkspaceAwareFunctionalPersistenceService extends FunctionalPersistenceService {
        private final Workspace workspace;
        private final Map<String, HttpRequestItem> requests = new LinkedHashMap<>();

        private WorkspaceAwareFunctionalPersistenceService(Workspace workspace) {
            this.workspace = workspace;
        }

        private void stubRequest(HttpRequestItem requestItem) {
            requests.put(requestItem.getId(), requestItem);
        }

        @Override
        public HttpRequestItem findRequestItemById(String requestId) {
            return requests.get(requestId);
        }

        @Override
        protected Workspace getCurrentWorkspace() {
            return workspace;
        }
    }

    private static final class LegacyAwareFunctionalPersistenceService extends FunctionalPersistenceService {
        private final Workspace workspace;
        private final Path legacyConfigPath;

        private LegacyAwareFunctionalPersistenceService(Workspace workspace, Path legacyConfigPath) {
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

    private static final class BlockingWorkspaceFunctionalPersistenceService extends FunctionalPersistenceService {
        private final Thread testThread = Thread.currentThread();
        private final CountDownLatch workerWorkspaceLookupStarted = new CountDownLatch(1);
        private final CountDownLatch releaseWorkerWorkspaceLookup = new CountDownLatch(1);
        private volatile Workspace workspace;

        private BlockingWorkspaceFunctionalPersistenceService(Workspace workspace) {
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
