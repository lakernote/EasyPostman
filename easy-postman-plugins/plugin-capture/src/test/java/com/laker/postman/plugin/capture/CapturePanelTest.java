package com.laker.postman.plugin.capture;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class CapturePanelTest {

    @Test
    public void shouldReturnDraftSummaryForIncompleteFilterExpression() {
        String summary = CapturePanel.summarizeDraftCaptureFilter("(a.com or");

        assertTrue(summary.contains("a.com or"));
    }

    @Test
    public void shouldRetainSelectedFlowWhenItRemainsVisibleAfterTableRefresh() {
        CaptureFlow selected = new CaptureFlow(
                "GET",
                "https://example.com/selected",
                "example.com",
                "/selected",
                Map.of(),
                new byte[0]
        );
        CaptureFlow newer = new CaptureFlow(
                "GET",
                "https://example.com/newer",
                "example.com",
                "/newer",
                Map.of(),
                new byte[0]
        );

        CaptureFlow retained = CapturePanel.findVisibleSelectedFlow(selected, List.of(newer, selected));

        assertSame(retained, selected);
    }

    @Test
    public void shouldDropSelectedFlowWhenItIsNoLongerVisibleAfterTableRefresh() {
        CaptureFlow selected = new CaptureFlow(
                "GET",
                "https://example.com/selected",
                "example.com",
                "/selected",
                Map.of(),
                new byte[0]
        );
        CaptureFlow newer = new CaptureFlow(
                "GET",
                "https://example.com/newer",
                "example.com",
                "/newer",
                Map.of(),
                new byte[0]
        );

        CaptureFlow retained = CapturePanel.findVisibleSelectedFlow(selected, List.of(newer));

        assertNull(retained);
    }

    @Test
    public void shouldFormatRetainedRowCountWithCapacity() {
        String countText = CapturePanel.formatRetainedRowCount(37, 37, 300);

        assertTrue(countText.contains("37"));
        assertTrue(countText.contains("300"));
    }

    @Test
    public void shouldWrapCaptureFilterTooltipInsteadOfShowingOneLongLine() {
        String tooltip = CapturePanel.htmlTooltip("过滤语法：method:POST host:api.example.com\n支持 and/or 和括号", 360);

        assertTrue(tooltip.startsWith("<html>"));
        assertTrue(tooltip.contains("width:360px"));
        assertTrue(tooltip.contains("<br>"));
        assertTrue(tooltip.contains("method:POST"));
    }

    @Test
    public void shouldUseCompactWidthsForCaptureTableColumns() {
        CapturePanel.TableColumnSpec[] specs = CapturePanel.captureTableColumnSpecs();

        assertEquals(specs.length, 10);
        assertEquals(specs[2].columnIndex(), 2);
        assertTrue(specs[2].preferredWidth() <= 160);
        assertTrue(specs[2].maxWidth() > 0);
        assertTrue(specs[2].maxWidth() <= 220);
        assertEquals(specs[3].columnIndex(), 3);
        assertTrue(specs[3].preferredWidth() <= 58);
        assertEquals(specs[4].columnIndex(), 4);
        assertTrue(specs[4].preferredWidth() <= 68);
        assertTrue(specs[5].preferredWidth() <= 70);
        assertTrue(specs[6].minWidth() <= 280);
        assertTrue(specs[6].preferredWidth() <= 420);
        assertTrue(specs[8].minWidth() <= 76);
        assertTrue(specs[9].minWidth() <= 80);
        assertTrue(totalPreferredWidth(specs) <= 1040);
        assertTrue(totalMinimumWidth(specs) <= 780);
    }

    private static int totalPreferredWidth(CapturePanel.TableColumnSpec[] specs) {
        int total = 0;
        for (CapturePanel.TableColumnSpec spec : specs) {
            total += spec.preferredWidth();
        }
        return total;
    }

    private static int totalMinimumWidth(CapturePanel.TableColumnSpec[] specs) {
        int total = 0;
        for (CapturePanel.TableColumnSpec spec : specs) {
            total += spec.minWidth();
        }
        return total;
    }

    @Test
    public void shouldPreserveDetailCaretWhenDetailTextRefreshes() {
        RSyntaxTextArea area = new RSyntaxTextArea();
        area.setText("first line\nsecond line\nthird line");
        area.setCaretPosition("first line\n".length());

        CapturePanel.updateDetailAreaTextPreservingView(area, "first line\nsecond line\nthird line\nfourth line");

        assertEquals(area.getCaretPosition(), "first line\n".length());
    }

    @Test
    public void shouldMatchSourceDisplayFilter() {
        CaptureFlow chrome = new CaptureFlow(
                "GET",
                "https://example.com/chrome",
                "example.com",
                "/chrome",
                Map.of(),
                new byte[0],
                "connection-1",
                CaptureSourceInfo.network("127.0.0.1", 50001, "127.0.0.1", 8888)
                        .withProcess("12345", "Google Chrome Helper", "/Applications/Google Chrome.app"),
                List.of()
        );
        CaptureFlow codex = new CaptureFlow(
                "GET",
                "https://example.com/codex",
                "example.com",
                "/codex",
                Map.of(),
                new byte[0],
                "connection-2",
                CaptureSourceInfo.network("127.0.0.1", 50002, "127.0.0.1", 8888)
                        .withProcess("25562", "codex", "/usr/local/bin/codex"),
                List.of()
        );

        assertTrue(CapturePanel.matchesSourceDisplayFilter(chrome, "Google Chrome Helper", ""));
        assertTrue(CapturePanel.matchesSourceDisplayFilter(codex, "", "Google Chrome Helper"));
        assertFalse(CapturePanel.matchesSourceDisplayFilter(chrome, "codex", ""));
        assertFalse(CapturePanel.matchesSourceDisplayFilter(chrome, "", "Google Chrome Helper"));
    }

    @Test
    public void shouldResetBaseTableCellColorAfterSemanticColumnColor() {
        JLabel label = new JLabel("77");
        label.setForeground(CaptureStatusStyle.tableForegroundFor(502));

        CapturePanel.resetCaptureTableLabel(label, false, SwingConstants.LEFT, null);

        assertEquals(label.getForeground(), com.laker.postman.common.constants.ModernColors.getTextPrimary());
        assertEquals(label.getHorizontalAlignment(), SwingConstants.LEFT);
        assertNull(label.getToolTipText());

        label.setForeground(Color.RED);
        CapturePanel.resetCaptureTableLabel(label, true, SwingConstants.RIGHT, "selected");

        assertEquals(label.getHorizontalAlignment(), SwingConstants.RIGHT);
        assertEquals(label.getToolTipText(), "selected");
    }
}
