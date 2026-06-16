package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.List;

import static org.testng.Assert.assertEquals;
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
}
