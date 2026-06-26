package com.laker.postman.plugin.capture;

import com.laker.postman.plugin.api.PluginStorage;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
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
    public void shouldOmitSourceAndHostFromDetailSummaryChips() {
        CapturePanel panel = new CapturePanel(null, PluginStorage.noop());

        List<String> labels = collectLabelTexts(panel);

        assertFalse(labels.contains(CaptureI18n.t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_SOURCE) + ": -"));
        assertFalse(labels.contains(CaptureI18n.t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_HOST) + ": -"));
    }

    @Test
    public void shouldUseCompactWidthsForCaptureTableColumns() {
        CapturePanel.TableColumnSpec[] specs = CapturePanel.captureTableColumnSpecs();

        assertEquals(specs.length, 10);
        assertEquals(specs[1].columnIndex(), 1);
        assertTrue(specs[1].preferredWidth() >= 64);
        assertEquals(specs[2].columnIndex(), 2);
        assertTrue(specs[2].preferredWidth() <= 120);
        assertTrue(specs[2].maxWidth() > 0);
        assertTrue(specs[2].maxWidth() <= 160);
        assertEquals(specs[3].columnIndex(), 3);
        assertTrue(specs[3].preferredWidth() <= 58);
        assertEquals(specs[4].columnIndex(), 4);
        assertTrue(specs[4].preferredWidth() <= 68);
        assertTrue(specs[5].preferredWidth() <= 70);
        assertTrue(specs[6].minWidth() <= 240);
        assertTrue(specs[6].preferredWidth() <= 320);
        assertEquals(specs[6].maxWidth(), 0);
        assertTrue(specs[7].maxWidth() <= 80);
        assertTrue(specs[8].maxWidth() <= 90);
        assertTrue(specs[9].maxWidth() <= 90);
        assertTrue(totalPreferredWidth(specs) <= 860);
        assertTrue(totalMinimumWidth(specs) <= 640);
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

    private static List<String> collectLabelTexts(Component component) {
        List<String> labels = new ArrayList<>();
        collectLabelTexts(component, labels);
        return labels;
    }

    private static void collectLabelTexts(Component component, List<String> labels) {
        if (component instanceof JLabel label) {
            labels.add(label.getText());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectLabelTexts(child, labels);
            }
        }
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
