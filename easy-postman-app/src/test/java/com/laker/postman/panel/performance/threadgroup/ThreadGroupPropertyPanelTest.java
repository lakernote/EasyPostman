package com.laker.postman.panel.performance.threadgroup;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.Container;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ThreadGroupPropertyPanelTest extends AbstractSwingUiTest {

    @Test
    public void fixedExecutionModeShouldUseExclusiveSegmentButtons() {
        ThreadGroupPropertyPanel panel = new ThreadGroupPropertyPanel();
        ThreadGroupData data = new ThreadGroupData();
        data.threadMode = ThreadGroupData.ThreadMode.FIXED;
        data.useTime = false;
        PerformanceTreeNode node = new PerformanceTreeNode("group", NodeType.THREAD_GROUP, data);

        panel.setThreadGroupData(node);

        JToggleButton loopButton = findToggleButton(
                panel,
                trimFieldLabel(I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_LOOPS))
        );
        JToggleButton timeButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_USE_TIME)
        );
        assertNotNull(loopButton);
        assertNotNull(timeButton);
        assertTrue(loopButton.isSelected());
        assertFalse(timeButton.isSelected());

        timeButton.doClick();
        panel.saveThreadGroupData();
        assertTrue(node.threadGroupData.useTime);
        assertFalse(loopButton.isSelected());
        assertTrue(timeButton.isSelected());

        loopButton.doClick();
        panel.saveThreadGroupData();
        assertFalse(node.threadGroupData.useTime);
        assertTrue(loopButton.isSelected());
        assertFalse(timeButton.isSelected());
    }

    private static JToggleButton findToggleButton(Component component, String text) {
        if (component instanceof JToggleButton button && text.equals(button.getText())) {
            return button;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JToggleButton button = findToggleButton(child, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private static String trimFieldLabel(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[:：]\\s*$", "");
    }
}
