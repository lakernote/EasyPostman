package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import java.awt.Cursor;
import java.awt.Dimension;
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

    private static void moveMouse(TimelinePanel panel, int x, int y) {
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(), 0, x, y, 0, false);
        for (MouseMotionListener listener : panel.getMouseMotionListeners()) {
            listener.mouseMoved(event);
        }
    }
}
