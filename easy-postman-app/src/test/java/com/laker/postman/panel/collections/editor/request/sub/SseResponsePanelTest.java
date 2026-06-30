package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.stream.MessageType;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class SseResponsePanelTest extends AbstractSwingUiTest {

    @Test
    public void lowFrequencyFieldsShouldMoveOutOfDefaultTableColumns() throws Exception {
        SSEResponsePanel panel = createPanel();

        SwingUtilities.invokeAndWait(() -> {
            JTable table = panel.getTable();
            assertEquals(table.getColumnCount(), 4);
            assertEquals(table.getColumnName(0), I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_TYPE));
            assertEquals(table.getColumnName(1), I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_TIME));
            assertEquals(table.getColumnName(2), I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_CONTENT));
            assertEquals(table.getColumnName(3), I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ASSERTION));

            TableColumn typeColumn = table.getColumnModel().getColumn(0);
            TableColumn contentColumn = table.getColumnModel().getColumn(2);
            TableColumn assertionColumn = table.getColumnModel().getColumn(3);

            assertTrue(typeColumn.getPreferredWidth() <= 60);
            assertTrue(contentColumn.getPreferredWidth() >= 650);
            assertHeaderFits(table, assertionColumn, "Assertion");
        });
    }

    @Test
    public void statusRowsShouldNotLeakLifecycleEventIntoDefaultTable() throws Exception {
        SSEResponsePanel panel = createPanel();

        SwingUtilities.invokeAndWait(() -> panel.addMessage(
                MessageType.CONNECTED,
                "10:00:00",
                null,
                "open",
                null,
                I18nUtil.getMessage(MessageKeys.SSE_STREAM_CONNECTED),
                null
        ));
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            JTable table = panel.getTable();

            assertEquals(table.getRowCount(), 1);
            assertEquals(table.getValueAt(0, 2), I18nUtil.getMessage(MessageKeys.SSE_STREAM_CONNECTED));
            for (int column = 0; column < table.getColumnCount(); column++) {
                assertTrue(!"open".equals(table.getValueAt(0, column)));
            }

            Component component = table.prepareRenderer(table.getCellRenderer(0, 2), 0, 2);
            assertEquals(component.getForeground(), ModernColors.getTextSecondary());
        });
    }

    @Test
    public void sseDetailDialogMetadataShouldBeSeparateFromContentText() {
        java.util.List<StreamMessageContentDialog.DetailField> fields = java.util.List.of(
                new StreamMessageContentDialog.DetailField("Event ID", "abc-123"),
                new StreamMessageContentDialog.DetailField("Retry(ms)", "-")
        );

        String copyText = StreamMessageContentDialog.buildDetailCopyText(fields, "{\"ok\":true}");
        JPanel metadataPanel = StreamMessageContentDialog.createMetadataPanel(fields);

        assertTrue(copyText.contains("Event ID: abc-123"));
        assertTrue(copyText.contains("{\"ok\":true}"));
        assertEquals(metadataPanel.getComponentCount(), 4);
    }

    @Test
    public void detailDialogShouldDefaultToFormattedJsonAndUseIconButtons() {
        String rawJson = "{\"ok\":true,\"nested\":{\"value\":1}}";
        String formatted = """
                {
                    "ok": true,
                    "nested": {
                        "value": 1
                    }
                }
                """;

        StreamMessageContentDialog.InitialContent initial = StreamMessageContentDialog.resolveInitialContent(
                rawJson,
                true,
                () -> formatted
        );
        JButton copyButton = StreamMessageContentDialog.createFooterButton(
                "复制",
                false,
                StreamMessageContentDialog.FooterAction.COPY
        );

        assertEquals(initial.text(), formatted);
        assertTrue(initial.showingFormatted());
        assertNotNull(copyButton.getIcon());
    }

    @Test
    public void detailDialogShouldAllocateMoreHeightForContentEditor() {
        Dimension size = StreamMessageContentDialog.preferredDialogSize(
                "{\"id\":\"short\"}",
                new Dimension(1440, 900)
        );

        assertTrue(size.height >= 420);
    }

    private SSEResponsePanel createPanel() throws Exception {
        SSEResponsePanel[] holder = new SSEResponsePanel[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new SSEResponsePanel());
        return holder[0];
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    private void assertHeaderFits(JTable table, TableColumn column, String fallbackHeader) {
        FontMetrics metrics = table.getFontMetrics(table.getTableHeader().getFont());
        int expectedWidth = metrics.stringWidth(fallbackHeader) + 30;
        assertTrue(column.getPreferredWidth() >= expectedWidth);
    }
}
