package com.laker.postman.panel.history;

import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.RoundedToolWindowPanel;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class HistoryPanelToolWindowLayoutTest extends AbstractSwingUiTest {

    @Test
    public void historyMainSplitShouldUseCollectionStyleRoundedCards() {
        JLabel left = new JLabel("history");
        JLabel right = new JLabel("detail");

        JSplitPane splitPane = HistoryPanel.createHistorySplitPane(left, right);

        assertEquals(splitPane.getDividerSize(), AppToolWindowChrome.DIVIDER_SIZE);
        assertEquals(splitPane.getDividerLocation(), AppToolWindowChrome.DEFAULT_SIDE_WIDTH);
        assertTrue(splitPane.getBorder() instanceof EmptyBorder);
        assertWrappedLeftToolWindow(splitPane.getLeftComponent(), left);
        assertWrappedRightToolWindow(splitPane.getRightComponent(), right);
    }

    @Test
    public void historyHeaderShouldNotShowSidebarStatsText() throws Exception {
        JPanel headerPanel = createHeaderPanel();

        assertFalse(allLabelTexts(headerPanel).stream().anyMatch(text -> text != null && text.contains("0/0")));
    }

    private static JPanel createHeaderPanel() throws Exception {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        HistoryPanel historyPanel;
        try {
            historyPanel = new HistoryPanel();
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
        Method method = HistoryPanel.class.getDeclaredMethod("createHeaderPanel");
        method.setAccessible(true);
        return (JPanel) method.invoke(historyPanel);
    }

    private static List<String> allLabelTexts(Component component) {
        List<String> texts = new ArrayList<>();
        collectLabelTexts(component, texts);
        return texts;
    }

    private static void collectLabelTexts(Component component, List<String> texts) {
        if (component instanceof JLabel label) {
            texts.add(label.getText());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectLabelTexts(child, texts);
            }
        }
    }

    private static void assertWrappedLeftToolWindow(Component wrapperComponent, Component content) {
        JComponent wrapper = assertChromeWrapper(wrapperComponent);
        assertEquals(wrapper.getInsets().top, 4);
        assertEquals(wrapper.getInsets().left, 6);
        assertEquals(wrapper.getInsets().bottom, 4);
        assertEquals(wrapper.getInsets().right, 0);

        JComponent roundedContent = assertRoundedContent(wrapper);
        assertTrue(roundedContent.getBorder() instanceof EmptyBorder);
        assertEquals(roundedContent.getInsets().top, 8);
        assertEquals(roundedContent.getInsets().left, 10);
        assertEquals(roundedContent.getInsets().bottom, 8);
        assertEquals(roundedContent.getInsets().right, 10);
        assertSame(roundedContent.getComponent(0), content);
    }

    private static void assertWrappedRightToolWindow(Component wrapperComponent, Component content) {
        JComponent wrapper = assertChromeWrapper(wrapperComponent);
        assertEquals(wrapper.getInsets().top, 4);
        assertEquals(wrapper.getInsets().left, 0);
        assertEquals(wrapper.getInsets().bottom, 4);
        assertEquals(wrapper.getInsets().right, 6);
        assertSame(assertRoundedPanel(wrapper).getComponent(0), content);
    }

    private static JComponent assertChromeWrapper(Component wrapperComponent) {
        assertTrue(wrapperComponent instanceof JComponent);
        JComponent wrapper = (JComponent) wrapperComponent;
        assertTrue(wrapper.getBorder() instanceof EmptyBorder);
        assertTrue(wrapper.getComponent(0) instanceof RoundedToolWindowPanel);
        return wrapper;
    }

    private static JComponent assertRoundedContent(JComponent wrapper) {
        Component content = assertRoundedPanel(wrapper).getComponent(0);
        assertTrue(content instanceof JComponent);
        return (JComponent) content;
    }

    private static RoundedToolWindowPanel assertRoundedPanel(JComponent wrapper) {
        return (RoundedToolWindowPanel) wrapper.getComponent(0);
    }
}
