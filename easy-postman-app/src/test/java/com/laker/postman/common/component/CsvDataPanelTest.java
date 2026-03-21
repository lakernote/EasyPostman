package com.laker.postman.common.component;

import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class CsvDataPanelTest {

    @Test(description = "CsvDataPanel 应支持状态导出和恢复")
    public void shouldRoundTripCsvState() {
        CsvDataPanel.CsvState state = new CsvDataPanel.CsvState(
                "users.csv",
                List.of("username", "password"),
                List.of(row("username", "alice", "password", "secret"))
        );

        CsvDataPanel panel = new CsvDataPanel();
        panel.restoreState(state);

        assertTrue(panel.hasData());
        assertEquals(panel.getRowCount(), 1);
        assertEquals(panel.getRowData(0).get("username"), "alice");
        assertEquals(panel.getRowData(0).get("password"), "secret");

        CsvDataPanel.CsvState exported = panel.exportState();
        assertNotNull(exported);
        assertEquals(exported.getSourceName(), "users.csv");
        assertEquals(exported.getHeaders(), List.of("username", "password"));
        assertEquals(exported.getRows().size(), 1);
        assertEquals(exported.getRows().get(0).get("username"), "alice");
        assertEquals(exported.getRows().get(0).get("password"), "secret");
    }

    @Test(description = "恢复后的状态应保持列顺序")
    public void shouldPreserveHeaderOrderAfterRestore() {
        CsvDataPanel panel = new CsvDataPanel();
        panel.restoreState(new CsvDataPanel.CsvState(
                null,
                List.of("cookie", "username", "token"),
                List.of(row("cookie", "c1", "username", "alice", "token", "t1"))
        ));

        CsvDataPanel.CsvState exported = panel.exportState();
        assertNotNull(exported);
        assertEquals(exported.getHeaders(), List.of("cookie", "username", "token"));
    }

    private static Map<String, String> row(String... keyValues) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put(keyValues[i], keyValues[i + 1]);
        }
        return row;
    }
}
