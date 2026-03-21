package com.laker.postman.service;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.functional.table.RunnerRowData;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class FunctionalPersistenceServiceTest {

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
}
