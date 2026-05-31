package com.laker.postman.panel.performance;

import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SseStagePropertyPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldHideEventFilterForFixedDurationMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.READ);
        PerformanceTreeNode node = new PerformanceTreeNode("SSE Read", NodeType.SSE_READ);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.FIXED_DURATION;
        node.ssePerformanceData.eventNameFilter = "done";

        panel.setNode(node);

        assertFalse(eventNameFilterField(panel).isVisible());
    }

    @Test
    public void shouldHideEventFilterForFirstEventMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.READ);
        PerformanceTreeNode node = new PerformanceTreeNode("SSE Read", NodeType.SSE_READ);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.SINGLE_MESSAGE;
        node.ssePerformanceData.eventNameFilter = "done";

        panel.setNode(node);

        assertFalse(eventNameFilterField(panel).isVisible());
    }

    @Test
    public void shouldShowOnlyCloseTimeoutForStreamClosedMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.READ);
        PerformanceTreeNode node = new PerformanceTreeNode("SSE Read", NodeType.SSE_READ);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.STREAM_CLOSED;

        panel.setNode(node);

        assertFalse(eventNameFilterField(panel).isVisible());
        assertFalse(messageFilterField(panel).isVisible());
        assertFalse(readTimeoutSpinner(panel).isVisible());
        assertTrue(holdConnectionSpinner(panel).isVisible());
    }

    @Test
    public void shouldUseOnlyReceiveTimeoutForMessageCountMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.READ);
        PerformanceTreeNode node = new PerformanceTreeNode("SSE Read", NodeType.SSE_READ);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.MESSAGE_COUNT;

        panel.setNode(node);

        assertTrue(readTimeoutSpinner(panel).isVisible());
        assertFalse(holdConnectionSpinner(panel).isVisible());
    }

    private EasyTextField eventNameFilterField(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("eventNameFilterField");
        field.setAccessible(true);
        return (EasyTextField) field.get(panel);
    }

    private EasyTextField messageFilterField(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("messageFilterField");
        field.setAccessible(true);
        return (EasyTextField) field.get(panel);
    }

    private EasyJSpinner readTimeoutSpinner(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("readTimeoutSpinner");
        field.setAccessible(true);
        return (EasyJSpinner) field.get(panel);
    }

    private EasyJSpinner holdConnectionSpinner(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("holdConnectionSpinner");
        field.setAccessible(true);
        return (EasyJSpinner) field.get(panel);
    }
}
