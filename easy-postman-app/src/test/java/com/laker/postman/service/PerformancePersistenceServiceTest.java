package com.laker.postman.service;

import com.laker.postman.common.component.CsvDataPanel;
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

import static org.testng.Assert.*;

public class PerformancePersistenceServiceTest {

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
}
