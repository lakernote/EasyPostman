package com.laker.postman.panel.performance;

import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertFalse;

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

    private EasyTextField eventNameFilterField(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("eventNameFilterField");
        field.setAccessible(true);
        return (EasyTextField) field.get(panel);
    }
}
