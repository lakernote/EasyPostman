package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import java.awt.Cursor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TimelinePanelLayoutTest extends AbstractSwingUiTest {

    @Test
    public void standardTimelineShouldRemainCompactEnoughForFirstViewport() {
        TimelinePanel panel = new TimelinePanel(List.of(
                new TimelinePanel.Stage("DNS解析", 0, 148, "DNS"),
                new TimelinePanel.Stage("建立连接", 148, 375, "Connect"),
                new TimelinePanel.Stage("SSL握手", 375, 1085, "TLS"),
                new TimelinePanel.Stage("请求发送", 1085, 1102, "Request"),
                new TimelinePanel.Stage("等待响应 (TTFB)", 1102, 1591, "TTFB"),
                new TimelinePanel.Stage("内容下载", 1591, 1592, "Download")
        ), new HttpEventInfo());

        Dimension preferredSize = panel.getPreferredSize();

        assertTrue(preferredSize.height <= 360,
                "Timeline should avoid the tall two-card layout, height was " + preferredSize.height);
    }

    @Test
    public void certificateWarningShouldUpdatePreferredHeightWhenEventInfoChanges() {
        TimelinePanel panel = new TimelinePanel(List.of(
                new TimelinePanel.Stage("DNS解析", 0, 148, "DNS")
        ), new HttpEventInfo());
        int originalHeight = panel.getPreferredSize().height;
        HttpEventInfo info = new HttpEventInfo();
        info.setSslCertWarning("Certificate hostname mismatch");

        panel.setHttpEventInfo(info);

        assertTrue(panel.getPreferredSize().height > originalHeight,
                "Certificate warning should add a visible compact info row");
    }

    @Test
    public void leftWhitespaceShouldNotTriggerBarHover() {
        TimelinePanel panel = new TimelinePanel(List.of(
                new TimelinePanel.Stage("DNS解析", 0, 148, "DNS")
        ), new HttpEventInfo());
        Dimension preferredSize = panel.getPreferredSize();
        panel.setSize(preferredSize);
        int barCenterY = preferredSize.height - 36;

        moveMouse(panel, 4, barCenterY);

        assertEquals(panel.getCursor().getType(), Cursor.DEFAULT_CURSOR,
                "Timeline should not show a bar hover cursor over unrelated left whitespace");
    }

    @Test
    public void zeroDurationStageShouldNotTriggerBarHover() {
        TimelinePanel panel = new TimelinePanel(List.of(
                new TimelinePanel.Stage("DNS解析", 0, 0, "DNS")
        ), new HttpEventInfo());
        Dimension preferredSize = panel.getPreferredSize();
        panel.setSize(preferredSize);
        int barCenterY = preferredSize.height - 36;

        moveMouse(panel, 180, barCenterY);

        assertEquals(panel.getCursor().getType(), Cursor.DEFAULT_CURSOR,
                "Zero-duration stages should not expose hover affordance when no bar is drawn");
    }

    @Test
    public void zeroDurationStageShouldUseSameRowHoverBackground() {
        TimelinePanel panel = new TimelinePanel(List.of(
                new TimelinePanel.Stage("DNS解析", 0, 0, "DNS")
        ), new HttpEventInfo());
        Dimension preferredSize = panel.getPreferredSize();
        panel.setSize(preferredSize);
        int barCenterY = preferredSize.height - 36;

        moveMouse(panel, 180, barCenterY);
        BufferedImage image = paintPanel(panel);

        Color rowBackground = new Color(image.getRGB(30, barCenterY), true);
        assertEquals(rowBackground, TimelineTheme.hoveredBarBackground(),
                "Zero-duration rows should use the same hover highlight as timed rows");
    }

    @Test
    public void durationStageShouldKeepDefaultCursorBecauseRowsAreTooltipOnly() {
        TimelinePanel panel = new TimelinePanel(List.of(
                new TimelinePanel.Stage("DNS解析", 0, 148, "DNS")
        ), new HttpEventInfo());
        Dimension preferredSize = panel.getPreferredSize();
        panel.setSize(preferredSize);
        int barCenterY = preferredSize.height - 36;

        moveMouse(panel, 180, barCenterY);

        assertEquals(panel.getCursor().getType(), Cursor.DEFAULT_CURSOR,
                "Timeline rows expose hover details but are not clickable actions");
    }

    @Test
    public void zeroDurationStageRowShouldExplainMissingObservedTiming() {
        boolean originalChinese = I18nUtil.isChinese();
        try {
            I18nUtil.setLocale("en");
            TimelinePanel panel = new TimelinePanel(List.of(
                    new TimelinePanel.Stage("DNS", 0, 0, "DNS")
            ), new HttpEventInfo());
            Dimension preferredSize = panel.getPreferredSize();
            panel.setSize(preferredSize);

            String tooltip = findStageTooltip(panel, 180, "DNS");

            assertNotNull(tooltip);
            assertTrue(tooltip.contains("Not recorded"),
                    "Unobserved zero-duration stages should explain why no bar is drawn");
        } finally {
            I18nUtil.setLocale(originalChinese ? "zh" : "en");
        }
    }

    @Test
    public void sameMillisecondObservedStageShouldDisplaySubMillisecondDuration() {
        boolean originalChinese = I18nUtil.isChinese();
        try {
            I18nUtil.setLocale("en");
            HttpEventInfo info = new HttpEventInfo();
            info.setDnsStart(1_000);
            info.setDnsEnd(1_000);
            TimelinePanel panel = new TimelinePanel(TimelinePanel.buildStandardStages(info), info);
            Dimension preferredSize = panel.getPreferredSize();
            panel.setSize(preferredSize);

            String tooltip = findStageTooltip(panel, 180, "DNS");

            assertNotNull(tooltip);
            assertTrue(tooltip.contains("&lt;1ms"),
                    "Same-millisecond observed stages should be displayed as sub-millisecond timing");
            assertTrue(tooltip.contains("same millisecond"),
                    "Tooltip should distinguish observed sub-millisecond timing from missing timing");
        } finally {
            I18nUtil.setLocale(originalChinese ? "zh" : "en");
        }
    }

    @Test
    public void subMillisecondMarkerShouldClampToTrackWithoutTextReserve() {
        int trackRightX = 1_000;
        int markerX = TimelinePanel.resolveSubMillisecondMarkerX(996, 4, trackRightX);

        assertEquals(markerX, 996,
                "Sub-millisecond markers should only reserve their own width because the duration is shown in the tooltip");
    }

    @Test
    public void subMillisecondStagesShouldReserveVisualSlotsInSequence() {
        List<TimelinePanel.Stage> stages = List.of(
                new TimelinePanel.Stage("Request Send", 1_000, 1_000, ""),
                new TimelinePanel.Stage("Waiting", 1_000, 1_005, ""),
                new TimelinePanel.Stage("Content Download", 1_005, 1_005, "")
        );

        int[] widths = TimelinePanel.resolveStageVisualWidths(stages, 5, 500);

        assertEquals(widths.length, 3);
        assertTrue(widths[0] > 0 && widths[0] < widths[1],
                "Leading sub-millisecond stages should reserve a small visual slot before the next duration bar");
        assertEquals(widths[0], widths[2],
                "Sub-millisecond stages should use a consistent visual slot");
        assertEquals(widths[1], 500 - widths[0] - widths[2],
                "Duration bars should use the remaining visual space after sub-millisecond slots");
        assertEquals(IntStream.of(widths).sum(), 500,
                "Observed visual stages should fill the available track without overflowing");
    }

    @Test
    public void durationTextShouldOnlyRenderInsideBarsWithEnoughSpace() {
        assertTrue(TimelinePanel.shouldDrawDurationTextInsideBar(60, 40),
                "Duration labels should be shown when the bar has enough room for text and padding");
        assertFalse(TimelinePanel.shouldDrawDurationTextInsideBar(48, 40),
                "Duration labels should be hidden instead of being pushed outside narrow bars");
    }

    @Test
    public void connectionInfoTooltipShouldExposeFullTruncatedValues() {
        HttpEventInfo info = new HttpEventInfo();
        info.setRemoteAddress("very-long-hostname-for-debugging.example.internal/192.168.100.101:443");
        info.setCipherName("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
        TimelinePanel panel = new TimelinePanel(List.of(
                new TimelinePanel.Stage("DNS解析", 0, 148, "DNS")
        ), info);
        panel.setSize(panel.getPreferredSize());

        String tooltip = panel.getToolTipText(new MouseEvent(panel, MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(), 0, 32, 32, 0, false));

        assertNotNull(tooltip);
        assertTrue(tooltip.contains("very-long-hostname-for-debugging.example.internal/192.168.100.101:443"),
                "Info tooltip should expose the full remote address");
        assertTrue(tooltip.contains("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"),
                "Info tooltip should expose the full cipher name");
    }

    @Test
    public void englishConnectionInfoLabelsShouldFitNormalSummaryColumns() {
        boolean originalChinese = I18nUtil.isChinese();
        try {
            I18nUtil.setLocale("en");
            TimelinePanel panel = new TimelinePanel(List.of(
                    new TimelinePanel.Stage("DNS", 0, 148, "DNS")
            ), new HttpEventInfo());
            FontMetrics metrics = panel.getFontMetrics(FontsUtil.getDefaultFont(Font.BOLD));
            List<String> labels = List.of(
                    I18nUtil.getMessage(MessageKeys.WATERFALL_HTTP_VERSION),
                    I18nUtil.getMessage(MessageKeys.WATERFALL_LOCAL_ADDRESS),
                    I18nUtil.getMessage(MessageKeys.WATERFALL_REMOTE_ADDRESS),
                    I18nUtil.getMessage(MessageKeys.WATERFALL_TLS_PROTOCOL),
                    I18nUtil.getMessage(MessageKeys.WATERFALL_CIPHER_NAME),
                    I18nUtil.getMessage(MessageKeys.WATERFALL_CERTIFICATE_CN),
                    I18nUtil.getMessage(MessageKeys.WATERFALL_ISSUER_CN),
                    I18nUtil.getMessage(MessageKeys.WATERFALL_VALID_UNTIL)
            );

            int labelWidth = TimelinePanel.resolveInfoLabelWidth(metrics, labels, 560);

            for (String label : labels) {
                assertTrue(metrics.stringWidth(label) <= labelWidth - TimelinePanel.INFO_LABEL_VALUE_GAP,
                        label + " should not be ellipsized in the English timeline summary");
            }
            assertTrue(560 - labelWidth >= TimelinePanel.INFO_VALUE_MIN_WIDTH,
                    "Timeline summary values should keep enough space after dynamic label width");
        } finally {
            I18nUtil.setLocale(originalChinese ? "zh" : "en");
        }
    }

    private static void moveMouse(TimelinePanel panel, int x, int y) {
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(), 0, x, y, 0, false);
        for (MouseMotionListener listener : panel.getMouseMotionListeners()) {
            listener.mouseMoved(event);
        }
    }

    private static BufferedImage paintPanel(TimelinePanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static String findStageTooltip(TimelinePanel panel, int x, String stageLabel) {
        for (int y = 0; y < panel.getHeight(); y++) {
            String tooltip = panel.getToolTipText(new MouseEvent(panel, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, x, y, 0, false));
            if (tooltip != null && tooltip.contains("<b>" + stageLabel + "</b>")) {
                return tooltip;
            }
        }
        return null;
    }
}
