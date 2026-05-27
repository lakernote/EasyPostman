package com.laker.postman.panel.performance;

import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SseStagePropertyPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldHideEventFilterForFixedDurationMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.AWAIT);
        JMeterTreeNode node = new JMeterTreeNode("SSE Await", NodeType.SSE_AWAIT);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.FIXED_DURATION;
        node.ssePerformanceData.eventNameFilter = "done";

        panel.setRequestNode(node);

        assertFalse(eventNameFilterField(panel).isVisible());
    }

    @Test
    public void shouldHideEventFilterForFirstEventMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.AWAIT);
        JMeterTreeNode node = new JMeterTreeNode("SSE Await", NodeType.SSE_AWAIT);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.FIRST_MESSAGE;
        node.ssePerformanceData.eventNameFilter = "done";

        panel.setRequestNode(node);

        assertFalse(eventNameFilterField(panel).isVisible());
    }

    @Test
    public void shouldShowOnlyCloseTimeoutForStreamClosedMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.AWAIT);
        JMeterTreeNode node = new JMeterTreeNode("SSE Await", NodeType.SSE_AWAIT);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.STREAM_CLOSED;

        panel.setRequestNode(node);

        assertFalse(eventNameFilterField(panel).isVisible());
        assertFalse(messageFilterField(panel).isVisible());
        assertFalse(awaitTimeoutSpinner(panel).isVisible());
        assertTrue(holdConnectionSpinner(panel).isVisible());
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

    private EasyJSpinner awaitTimeoutSpinner(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("awaitTimeoutSpinner");
        field.setAccessible(true);
        return (EasyJSpinner) field.get(panel);
    }

    private EasyJSpinner holdConnectionSpinner(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("holdConnectionSpinner");
        field.setAccessible(true);
        return (EasyJSpinner) field.get(panel);
    }
}
