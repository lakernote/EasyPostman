package com.laker.postman.panel.collections;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.common.component.RoundedToolWindowPanel;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RequestCollectionsToolWindowLayoutTest {

    @Test
    public void requestCollectionsPanelShouldAddOuterGapAroundRoundedToolWindow() {
        JLabel content = new JLabel("tree");

        JComponent wrapper = RequestCollectionsPanel.createCollectionToolWindow(content);

        assertTrue(wrapper.getBorder() instanceof EmptyBorder);
        assertEquals(wrapper.getInsets().top, 4);
        assertEquals(wrapper.getInsets().left, 6);
        assertEquals(wrapper.getInsets().bottom, 4);
        assertEquals(wrapper.getInsets().right, 1);
        assertTrue(wrapper.getComponent(0) instanceof RoundedToolWindowPanel);
        RoundedToolWindowPanel roundedPanel = (RoundedToolWindowPanel) wrapper.getComponent(0);
        assertTrue(roundedPanel.getComponent(0) instanceof JComponent);
        JComponent insetPanel = (JComponent) roundedPanel.getComponent(0);
        assertTrue(insetPanel.getBorder() instanceof EmptyBorder);
        assertEquals(insetPanel.getInsets().top, 8);
        assertEquals(insetPanel.getInsets().left, 10);
        assertEquals(insetPanel.getInsets().bottom, 8);
        assertEquals(insetPanel.getInsets().right, 10);
        assertSame(insetPanel.getComponent(0), content);
    }

    @Test
    public void requestEditorPanelShouldUseMatchingRoundedToolWindowChrome() {
        JLabel content = new JLabel("editor");

        JComponent wrapper = RequestCollectionsPanel.createRequestEditorToolWindow(content);

        assertTrue(wrapper.getBorder() instanceof EmptyBorder);
        assertEquals(wrapper.getInsets().top, 4);
        assertEquals(wrapper.getInsets().left, 1);
        assertEquals(wrapper.getInsets().bottom, 4);
        assertEquals(wrapper.getInsets().right, 6);
        assertTrue(wrapper.getComponent(0) instanceof RoundedToolWindowPanel);
        RoundedToolWindowPanel roundedPanel = (RoundedToolWindowPanel) wrapper.getComponent(0);
        assertSame(roundedPanel.getComponent(0), content);
    }

    @Test
    public void requestCollectionsSplitShouldUseBorderlessBackgroundGapInsteadOfDividerLine() {
        JLabel left = new JLabel("collections");
        JLabel right = new JLabel("editor");

        JSplitPane splitPane = RequestCollectionsPanel.createCollectionsSplitPane(left, right);

        assertEquals(splitPane.getDividerSize(), 4);
        assertEquals(splitPane.getDividerLocation(), 310);
        assertTrue(splitPane.getBorder() instanceof EmptyBorder);
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        assertTrue(divider.getBorder() instanceof EmptyBorder);
    }

    @Test
    public void splitDividerShouldNotPaintTransientDragLine() {
        JLabel left = new JLabel("collections");
        JLabel right = new JLabel("editor");
        JSplitPane splitPane = RequestCollectionsPanel.createCollectionsSplitPane(left, right);
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        divider.setSize(4, 60);

        BufferedImage image = new BufferedImage(4, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        divider.paint(g2);
        g2.dispose();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(new Color(image.getRGB(x, y), true).getAlpha(), 0);
            }
        }
    }

    @Test
    public void splitPaneShouldPaintDividerGapWithMainBackground() {
        Object previousBackground = UIManager.get(ThemeColors.BACKGROUND);
        Color background = new Color(238, 242, 247);
        UIManager.put(ThemeColors.BACKGROUND, background);

        try {
            JLabel left = new JLabel("collections");
            JLabel right = new JLabel("editor");
            JSplitPane splitPane = RequestCollectionsPanel.createCollectionsSplitPane(left, right);
            splitPane.setSize(120, 40);
            splitPane.setDividerLocation(50);
            splitPane.doLayout();

            BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            splitPane.paint(g2);
            g2.dispose();

            assertEquals(new Color(image.getRGB(51, 20), true), background);
        } finally {
            UIManager.put(ThemeColors.BACKGROUND, previousBackground);
        }
    }

    @Test
    public void roundedToolWindowGapsShouldPaintAConsistentMainBackground() {
        Object previousBackground = UIManager.get(ThemeColors.BACKGROUND);
        Color background = new Color(238, 242, 247);
        UIManager.put(ThemeColors.BACKGROUND, background);

        try {
            JSplitPane splitPane = RequestCollectionsPanel.createCollectionsSplitPane(
                    new JLabel("collections"),
                    new JLabel("editor")
            );
            JComponent left = (JComponent) splitPane.getLeftComponent();
            JComponent right = (JComponent) splitPane.getRightComponent();

            assertTrue(left.isOpaque());
            assertTrue(right.isOpaque());
            assertTrue(splitPane.isOpaque());
            assertEquals(left.getBackground(), background);
            assertEquals(right.getBackground(), background);
            assertEquals(splitPane.getBackground(), background);
        } finally {
            UIManager.put(ThemeColors.BACKGROUND, previousBackground);
        }
    }
}
