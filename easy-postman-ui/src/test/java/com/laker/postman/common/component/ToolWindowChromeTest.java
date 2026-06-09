package com.laker.postman.common.component;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ToolWindowChromeTest {
    private Map<String, Object> previousTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousTokens = new HashMap<>();
        previousTokens.put(ThemeColors.BACKGROUND, UIManager.get(ThemeColors.BACKGROUND));
    }

    @AfterMethod
    public void restoreThemeTokens() {
        previousTokens.forEach(UIManager::put);
    }

    @Test
    public void shouldWrapLeftToolWindowWithRoundedCardAndOuterGap() {
        JLabel content = new JLabel("content");

        JComponent wrapper = ToolWindowChrome.wrapLeftToolWindow(content);

        assertTrue(wrapper.getBorder() instanceof EmptyBorder);
        assertEquals(wrapper.getInsets().top, 4);
        assertEquals(wrapper.getInsets().left, 6);
        assertEquals(wrapper.getInsets().bottom, 4);
        assertEquals(wrapper.getInsets().right, 1);
        assertTrue(wrapper.getComponent(0) instanceof RoundedToolWindowPanel);
        RoundedToolWindowPanel roundedPanel = (RoundedToolWindowPanel) wrapper.getComponent(0);
        assertSame(roundedPanel.getComponent(0), content);
    }

    @Test
    public void shouldCreateBorderlessHorizontalSplitPane() {
        JSplitPane splitPane = ToolWindowChrome.createHorizontalSplitPane(
                new JLabel("left"),
                new JLabel("right"),
                ToolWindowChrome.DEFAULT_SIDE_WIDTH
        );

        assertEquals(splitPane.getDividerLocation(), ToolWindowChrome.DEFAULT_SIDE_WIDTH);
        assertEquals(splitPane.getDividerSize(), ToolWindowChrome.DIVIDER_SIZE);
        assertTrue(splitPane.getBorder() instanceof EmptyBorder);
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        assertTrue(divider.getBorder() instanceof EmptyBorder);
    }

    @Test
    public void shouldCreateHorizontalCardSplitPaneWithMatchingToolWindowGaps() {
        JLabel left = new JLabel("left");
        JLabel right = new JLabel("right");

        JSplitPane splitPane = ToolWindowChrome.createHorizontalCardSplitPane(
                left,
                right,
                ToolWindowChrome.DEFAULT_SIDE_WIDTH
        );

        assertTrue(splitPane.getLeftComponent() instanceof JComponent);
        assertTrue(splitPane.getRightComponent() instanceof JComponent);
        JComponent leftWrapper = (JComponent) splitPane.getLeftComponent();
        JComponent rightWrapper = (JComponent) splitPane.getRightComponent();
        assertEquals(leftWrapper.getInsets().left, 6);
        assertEquals(leftWrapper.getInsets().right, 1);
        assertEquals(rightWrapper.getInsets().left, 1);
        assertEquals(rightWrapper.getInsets().right, 6);
        RoundedToolWindowPanel leftCard = (RoundedToolWindowPanel) leftWrapper.getComponent(0);
        RoundedToolWindowPanel rightCard = (RoundedToolWindowPanel) rightWrapper.getComponent(0);
        assertSame(leftCard.getComponent(0), left);
        assertSame(rightCard.getComponent(0), right);
    }

    @Test
    public void shouldCreateVerticalCardSplitPaneWithMatchingToolWindowGaps() {
        JLabel top = new JLabel("top");
        JLabel bottom = new JLabel("bottom");

        JSplitPane splitPane = ToolWindowChrome.createVerticalCardSplitPane(top, bottom, 260);

        JComponent topWrapper = (JComponent) splitPane.getTopComponent();
        JComponent bottomWrapper = (JComponent) splitPane.getBottomComponent();
        assertEquals(topWrapper.getInsets().top, 4);
        assertEquals(topWrapper.getInsets().bottom, 1);
        assertEquals(bottomWrapper.getInsets().top, 1);
        assertEquals(bottomWrapper.getInsets().bottom, 4);
        RoundedToolWindowPanel topCard = (RoundedToolWindowPanel) topWrapper.getComponent(0);
        RoundedToolWindowPanel bottomCard = (RoundedToolWindowPanel) bottomWrapper.getComponent(0);
        assertSame(topCard.getComponent(0), top);
        assertSame(bottomCard.getComponent(0), bottom);
    }

    @Test
    public void shouldCreateStackedVerticalSplitWithoutDoubleWrappingTopToolWindow() {
        JComponent topToolWindow = ToolWindowChrome.createHorizontalCardSplitPane(
                new JLabel("left"),
                new JLabel("right"),
                ToolWindowChrome.DEFAULT_SIDE_WIDTH
        );
        JLabel bottom = new JLabel("bottom");

        JSplitPane splitPane = ToolWindowChrome.createVerticalStackedCardSplitPane(topToolWindow, bottom, 260);

        assertSame(splitPane.getTopComponent(), topToolWindow);
        assertTrue(splitPane.getBottomComponent() instanceof JComponent);
        JComponent bottomWrapper = (JComponent) splitPane.getBottomComponent();
        assertEquals(bottomWrapper.getInsets().top, 1);
        assertEquals(bottomWrapper.getInsets().bottom, 4);
        RoundedToolWindowPanel bottomCard = (RoundedToolWindowPanel) bottomWrapper.getComponent(0);
        assertSame(bottomCard.getComponent(0), bottom);
    }

    @Test
    public void shouldWrapDialogContentWithBackgroundAndRoundedToolWindow() {
        JLabel content = new JLabel("dialog");

        JComponent dialogShell = ToolWindowChrome.wrapDialogToolWindow(content);

        assertTrue(dialogShell.getComponent(0) instanceof JComponent);
        JComponent toolWindowWrapper = (JComponent) dialogShell.getComponent(0);
        assertEquals(toolWindowWrapper.getInsets().top, 4);
        assertEquals(toolWindowWrapper.getInsets().left, 6);
        assertEquals(toolWindowWrapper.getInsets().bottom, 4);
        assertEquals(toolWindowWrapper.getInsets().right, 6);
        assertTrue(toolWindowWrapper.getComponent(0) instanceof RoundedToolWindowPanel);
        RoundedToolWindowPanel roundedPanel = (RoundedToolWindowPanel) toolWindowWrapper.getComponent(0);
        assertSame(roundedPanel.getComponent(0), content);
    }

    @Test
    public void splitDividerShouldNotPaintItsOwnLine() {
        JSplitPane splitPane = ToolWindowChrome.createHorizontalSplitPane(
                new JLabel("left"),
                new JLabel("right"),
                ToolWindowChrome.DEFAULT_SIDE_WIDTH
        );
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        divider.setSize(4, 40);

        BufferedImage image = new BufferedImage(4, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        divider.paint(graphics);
        graphics.dispose();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(new Color(image.getRGB(x, y), true).getAlpha(), 0);
            }
        }
    }

    @Test
    public void splitPaneShouldPaintGapWithMainBackground() {
        Color background = new Color(238, 242, 247);
        UIManager.put(ThemeColors.BACKGROUND, background);

        JSplitPane splitPane = ToolWindowChrome.createHorizontalSplitPane(new JLabel("left"), new JLabel("right"), 50);
        splitPane.setSize(120, 40);
        splitPane.setDividerLocation(50);
        splitPane.doLayout();

        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        splitPane.paint(graphics);
        graphics.dispose();

        assertEquals(new Color(image.getRGB(51, 20), true), background);
    }
}
