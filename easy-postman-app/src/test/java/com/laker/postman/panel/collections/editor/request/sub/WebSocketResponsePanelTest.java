package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.panel.collections.editor.request.StreamMessageUiMetadata;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.stream.MessageType;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class WebSocketResponsePanelTest extends AbstractSwingUiTest {

    @Test
    public void assertionColumnShouldShowInlineDetailsOnSingleClick() throws Exception {
        List<TestResult> testResults = List.of(new TestResult("status", true, ""));
        WebSocketResponsePanel panel = createPanelWithMessage("payload", testResults);

        SwingUtilities.invokeAndWait(() -> {
            JTable table = panel.getTable();
            Rectangle cell = table.getCellRect(0, 3, true);
            MouseEvent click = new MouseEvent(table, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    cell.x + cell.width / 2, cell.y + cell.height / 2, 1, false, MouseEvent.BUTTON1);

            for (MouseListener listener : table.getMouseListeners()) {
                listener.mouseClicked(click);
            }

            assertTrue(panel.getAssertionDetailsPanel().isVisible());
            assertEquals(panel.getAssertionDetailsPanel().getResultRowCount(), 1);
            assertEquals(panel.getAssertionDetailsPanel().getResultNameAt(0), "status");
            assertEquals(table.getSelectedRow(), 0);
            assertTrue(panel.getAssertionDetailsPanel().getTitleText().contains("1/1"));
            assertTrue(panel.getAssertionDetailsPanel().getMetaText()
                    .contains(StreamMessageUiMetadata.display(MessageType.RECEIVED)));
        });
    }

    @Test
    public void assertionDetailsShouldUseResizableSplitPane() throws Exception {
        WebSocketResponsePanel panel = createPanel();

        SwingUtilities.invokeAndWait(() -> {
            JSplitPane splitPane = panel.getAssertionSplitPane();

            assertSame(splitPane.getTopComponent(), panel.getTableScrollPane());
            assertSame(splitPane.getBottomComponent(), panel.getAssertionDetailsPanel());
            assertTrue(splitPane.getResizeWeight() > 0.9);
        });
    }

    @Test
    public void streamTableShouldExposeSemanticTypeAndAssertionSummaryValues() throws Exception {
        List<TestResult> testResults = List.of(
                new TestResult("ok", true, ""),
                new TestResult("failed", false, "boom")
        );
        WebSocketResponsePanel panel = createPanelWithMessage("payload", testResults);

        SwingUtilities.invokeAndWait(() -> {
            JTable table = panel.getTable();
            assertEquals(table.getValueAt(0, 0), MessageType.RECEIVED);
            assertEquals(table.getValueAt(0, 3).toString(), "1/2");
        });
    }

    @Test
    public void visibleRowsShouldMatchFilteredTableRows() throws Exception {
        WebSocketResponsePanel panel = createPanel();
        List<TestResult> hiddenResults = List.of(new TestResult("hidden", true, ""));
        List<TestResult> visibleResults = List.of(new TestResult("visible", true, ""));

        SwingUtilities.invokeAndWait(() -> {
            panel.addMessage(MessageType.RECEIVED, "10:00:00", "drop", hiddenResults);
            panel.addMessage(MessageType.RECEIVED, "10:00:01", "needle", visibleResults);
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() -> panel.getSearchField().setText("needle"));
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(panel.getTable().getRowCount(), 1);
            assertSame(panel.getVisibleRow(0).testResults, visibleResults);
        });
    }

    @Test
    public void messageAddedOffEdtShouldFlushToVisibleTable() throws Exception {
        WebSocketResponsePanel panel = createPanel();
        List<TestResult> testResults = List.of(new TestResult("off-edt", true, ""));

        panel.addMessage(MessageType.RECEIVED, "10:00:00", "off edt payload", testResults);
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(panel.getTable().getRowCount(), 1);
            assertSame(panel.getVisibleRow(0).testResults, testResults);
        });
    }

    @Test
    public void oversizedPendingBatchShouldOnlyShowRetainedRows() throws Exception {
        WebSocketResponsePanel panel = createPanel(3);
        List<TestResult> testResults = List.of(new TestResult("batched", true, ""));

        for (int i = 1; i <= 5; i++) {
            panel.addMessage(MessageType.RECEIVED, "10:00:0" + i, "payload-" + i, testResults);
        }
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            JTable table = panel.getTable();
            assertEquals(table.getRowCount(), 3);
            assertEquals(panel.getVisibleRow(0).content, "payload-3");
            assertEquals(panel.getVisibleRow(2).content, "payload-5");
        });
    }

    @Test
    public void messageContentDialogSizeShouldStayCompactForShortContent() {
        Dimension size = StreamMessageContentDialog.preferredDialogSize("""
                {
                  "type": "context-check",
                  "message": "send-once-after-connect"
                }
                """, new Dimension(1440, 900));

        assertTrue(size.width <= 680, "Short message detail width should not look like a large document viewer");
        assertTrue(size.height <= 360, "Short message detail height should not leave a large empty area");
    }

    @Test
    public void messageContentDialogSizeShouldCapLongContentToViewport() {
        String longContent = "payload ".repeat(500);

        Dimension size = StreamMessageContentDialog.preferredDialogSize(longContent, new Dimension(1000, 700));

        assertTrue(size.width <= 720, "Dialog should not consume more than 72% of viewport width");
        assertTrue(size.height <= 504, "Dialog should not consume more than 72% of viewport height");
        assertTrue(size.width >= 560, "Long content should still have enough readable width");
    }

    private WebSocketResponsePanel createPanelWithMessage(String content, List<TestResult> testResults) throws Exception {
        WebSocketResponsePanel panel = createPanel();
        SwingUtilities.invokeAndWait(() -> panel.addMessage(MessageType.RECEIVED, "10:00:00", content, testResults));
        flushEdt();
        return panel;
    }

    private WebSocketResponsePanel createPanel() throws Exception {
        return createPanel(StreamMessageLogBuffer.DEFAULT_MAX_ROWS);
    }

    private WebSocketResponsePanel createPanel(int maxRows) throws Exception {
        WebSocketResponsePanel[] holder = new WebSocketResponsePanel[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new WebSocketResponsePanel(maxRows));
        return holder[0];
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
        SwingUtilities.invokeAndWait(() -> {
        });
    }
}
