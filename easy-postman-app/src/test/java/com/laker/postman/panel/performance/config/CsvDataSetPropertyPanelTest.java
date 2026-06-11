package com.laker.postman.panel.performance.config;

import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class CsvDataSetPropertyPanelTest extends AbstractSwingUiTest {

    @Test
    public void previewTableShouldFillWideViewportWhenColumnsFit() {
        CsvDataSetPropertyPanel panel = new CsvDataSetPropertyPanel();
        JScrollPane previewScrollPane = findFirst(panel, JScrollPane.class);
        assertNotNull(previewScrollPane);
        previewScrollPane.setSize(900, 220);
        previewScrollPane.getViewport().setExtentSize(new Dimension(900, 180));

        panel.setNode(csvNode());

        JTable previewTable = findFirst(panel, JTable.class);
        assertNotNull(previewTable);
        assertEquals(previewTable.getAutoResizeMode(), JTable.AUTO_RESIZE_ALL_COLUMNS);
        assertTrue(totalPreferredColumnWidth(previewTable) >= 840,
                "CSV preview should use the available viewport width instead of leaving a large blank area");
    }

    @Test
    public void emptyStateShouldStayVisibleWhenNoCsvDataLoaded() {
        CsvDataSetPropertyPanel panel = new CsvDataSetPropertyPanel();
        panel.setNode(new PerformanceTreeNode("CSV Data Set", NodeType.CSV_DATA_SET));

        JLabel emptyStateLabel = findLabel(panel, I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
        assertNotNull(emptyStateLabel);
        assertTrue(emptyStateLabel.isShowing() || emptyStateLabel.isVisible());
    }

    @Test
    public void previewTableShouldContainAllCsvRows() {
        CsvDataSetPropertyPanel panel = new CsvDataSetPropertyPanel();
        panel.setNode(csvNodeWithRows(12));

        JTable previewTable = findFirst(panel, JTable.class);
        assertNotNull(previewTable);
        assertEquals(previewTable.getRowCount(), 12);
    }

    @Test
    public void previewTableShouldUseCsvManagementTableVisualStyle() {
        CsvDataSetPropertyPanel panel = new CsvDataSetPropertyPanel();
        panel.setNode(csvNode());

        JTable previewTable = findFirst(panel, JTable.class);
        assertNotNull(previewTable);
        assertEquals(previewTable.getRowHeight(), 30);
        assertTrue(previewTable.getShowHorizontalLines());
        assertTrue(previewTable.getShowVerticalLines());
        assertEquals(previewTable.getIntercellSpacing(), new Dimension(1, 1));
        assertEquals(previewTable.getRowMargin(), 1);
        assertFalse(previewTable.getTableHeader().getReorderingAllowed());
    }

    @Test
    public void headerShouldNotRenderScopeOrSourceNote() {
        CsvDataSetPropertyPanel panel = new CsvDataSetPropertyPanel();
        panel.setNode(new PerformanceTreeNode("CSV Data Set", NodeType.CSV_DATA_SET));
        panel.setBounds(0, 0, 1200, 520);
        layoutRecursively(panel);

        assertFalse(hasTextComponentAbove(panel, I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_DATA_SET_SCOPE_NOTE), 120));

        panel.setNode(csvNode());
        layoutRecursively(panel);
        assertFalse(hasTextComponentAbove(panel, "html", 120));
        assertFalse(hasTextComponentAbove(panel, I18nUtil.getMessage(MessageKeys.CSV_MANUAL_CREATED), 120));
    }

    @Test
    public void actionButtonsShouldUseShortLabels() {
        CsvDataSetPropertyPanel panel = new CsvDataSetPropertyPanel();
        panel.setNode(csvNode());

        assertTrue(hasButton(panel, I18nUtil.getMessage(MessageKeys.CSV_ACTION_IMPORT)));
        assertTrue(hasButton(panel, I18nUtil.getMessage(MessageKeys.CSV_ACTION_CREATE)));
        assertTrue(hasButton(panel, I18nUtil.getMessage(MessageKeys.CSV_ACTION_MANAGE)));
        assertTrue(hasButton(panel, I18nUtil.getMessage(MessageKeys.CSV_ACTION_CLEAR)));
        assertFalse(hasButton(panel, I18nUtil.getMessage(MessageKeys.CSV_MENU_IMPORT_FILE)));
        assertFalse(hasButton(panel, I18nUtil.getMessage(MessageKeys.CSV_MENU_CREATE_MANUAL)));
        assertFalse(hasButton(panel, I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA)));
        assertFalse(hasButton(panel, I18nUtil.getMessage(MessageKeys.CSV_MENU_CLEAR_DATA)));
    }

    private static int totalPreferredColumnWidth(JTable table) {
        int width = 0;
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            width += column.getPreferredWidth();
        }
        return width;
    }

    private static PerformanceTreeNode csvNode() {
        CsvDataSetData data = new CsvDataSetData(
                "users.csv",
                List.of("username", "password", "email"),
                List.of(
                        row("username", "alice", "password", "secret", "email", "alice@example.test"),
                        row("username", "bob", "password", "secret", "email", "bob@example.test")
                )
        );
        return new PerformanceTreeNode("CSV Data Set", NodeType.CSV_DATA_SET, data);
    }

    private static PerformanceTreeNode csvNodeWithRows(int rowCount) {
        List<Map<String, String>> rows = new java.util.ArrayList<>();
        for (int i = 1; i <= rowCount; i++) {
            rows.add(row("username", String.valueOf(i), "password", "", "email", ""));
        }
        CsvDataSetData data = new CsvDataSetData(
                "users.csv",
                List.of("username", "password", "email"),
                rows
        );
        return new PerformanceTreeNode("CSV Data Set", NodeType.CSV_DATA_SET, data);
    }

    private static Map<String, String> row(String... keyValues) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put(keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    private static JLabel findLabel(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel result = findLabel(child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static boolean hasTextComponentAbove(Component component, String text, int maxY) {
        List<Component> matches = new ArrayList<>();
        collectTextComponents(component, text, matches);
        for (Component match : matches) {
            if (toPanelBounds(component, match).y < maxY) {
                return true;
            }
        }
        return false;
    }

    private static void collectTextComponents(Component component, String text, List<Component> matches) {
        if (component instanceof JLabel label && label.getText() != null && label.getText().contains(text)) {
            matches.add(label);
        } else if (component instanceof JTextArea textArea && textArea.getText() != null && textArea.getText().contains(text)) {
            matches.add(textArea);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectTextComponents(child, text, matches);
            }
        }
    }

    private static boolean hasButton(Component root, String buttonText) {
        List<AbstractButton> buttons = new ArrayList<>();
        collectButtons(root, List.of(buttonText), buttons);
        return !buttons.isEmpty();
    }

    private static Rectangle unionButtonBoundsAbove(Component root, List<String> buttonTexts, int maxY) {
        List<AbstractButton> buttons = new ArrayList<>();
        collectButtons(root, buttonTexts, buttons);
        Rectangle union = null;
        for (AbstractButton button : buttons) {
            Rectangle bounds = toPanelBounds(root, button);
            if (bounds.y < maxY) {
                union = union == null ? bounds : union.union(bounds);
            }
        }
        return union;
    }

    private static void collectButtons(Component component, List<String> buttonTexts, List<AbstractButton> buttons) {
        if (component instanceof AbstractButton button && buttonTexts.contains(button.getText())) {
            buttons.add(button);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectButtons(child, buttonTexts, buttons);
            }
        }
    }

    private static Rectangle toPanelBounds(Component root, Component component) {
        Rectangle bounds = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), root);
        return new Rectangle(bounds);
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container childContainer) {
                layoutRecursively(childContainer);
            }
        }
    }

    private static <T> T findFirst(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T result = findFirst(child, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
