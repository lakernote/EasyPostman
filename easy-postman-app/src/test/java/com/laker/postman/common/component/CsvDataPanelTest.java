package com.laker.postman.common.component;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.*;

public class CsvDataPanelTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                "Table.background",
                "Table.alternateRowColor",
                "Table.selectionBackground",
                ThemeColors.SURFACE,
                ThemeColors.HOVER_BACKGROUND
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

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

    @Test(description = "CSV 斑马纹默认背景应使用主题 hover token")
    public void stripeBackgroundShouldUseSemanticHoverColorWhenLafAlternateColorMissing() throws Exception {
        Color hover = new Color(31, 32, 33);
        UIManager.put("Table.alternateRowColor", null);
        UIManager.put(ThemeColors.HOVER_BACKGROUND, hover);

        assertEquals(invokeStripeBackground(Color.WHITE), hover);
    }

    @Test(description = "CSV 表格选中背景应优先使用 FlatLaf Table.selectionBackground")
    public void csvTableSelectionShouldUseFlatLafTableSelectionBackground() throws Exception {
        Color selectionBackground = new Color(41, 42, 43);
        UIManager.put("Table.selectionBackground", selectionBackground);

        JTable table = new JTable();
        CsvDataPanel panel = new CsvDataPanel();
        invokeConfigureCsvTable(panel, table);

        assertEquals(table.getSelectionBackground(), selectionBackground);
    }

    @Test(description = "CSV 表格基础背景应使用 FlatLaf table token")
    public void csvTableBackgroundShouldUseTableBackgroundToken() throws Exception {
        Color tableBackground = new Color(20, 21, 22);
        Color cardBackground = new Color(31, 32, 33);
        UIManager.put("Table.background", tableBackground);
        UIManager.put(ThemeColors.SURFACE, cardBackground);

        JTable table = new JTable();
        CsvDataPanel panel = new CsvDataPanel();
        invokeConfigureCsvTable(panel, table);

        assertEquals(table.getBackground(), tableBackground);
    }

    @Test(description = "CSV 文件选择器应优先使用当前 CSV 所在目录，其次使用上一次成功导入目录")
    public void csvFileChooserShouldResolveInitialDirectoryFromCurrentFileOrRememberedDirectory() {
        File rememberedDir = new File(System.getProperty("java.io.tmpdir"));
        File currentFile = new File(rememberedDir, "users.csv");

        assertEquals(CsvFileChooserDirectoryMemory.resolveInitialDirectory(currentFile, null), rememberedDir);
        assertEquals(CsvFileChooserDirectoryMemory.resolveInitialDirectory(null, rememberedDir.getAbsolutePath()), rememberedDir);
        assertNull(CsvFileChooserDirectoryMemory.resolveInitialDirectory(null, "/path/not-exist/easy-postman-csv"));
    }

    @Test(description = "成功选择 CSV 文件后应记住父目录")
    public void csvFileChooserShouldRememberSelectedFileParentDirectory() {
        File selectedFile = new File(System.getProperty("java.io.tmpdir"), "users.csv");

        assertEquals(CsvFileChooserDirectoryMemory.resolveDirectoryToRemember(selectedFile),
                new File(System.getProperty("java.io.tmpdir")));
        assertNull(CsvFileChooserDirectoryMemory.resolveDirectoryToRemember(null));
    }

    private static Map<String, String> row(String... keyValues) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put(keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    private static Color invokeStripeBackground(Color base) throws Exception {
        Method method = CsvDataPanel.class.getDeclaredMethod("getStripeBackground", Color.class);
        method.setAccessible(true);
        return (Color) method.invoke(null, base);
    }

    private static void invokeConfigureCsvTable(CsvDataPanel panel, JTable table) throws Exception {
        Method method = CsvDataPanel.class.getDeclaredMethod("configureCsvTable", JTable.class);
        method.setAccessible(true);
        method.invoke(panel, table);
    }
}
