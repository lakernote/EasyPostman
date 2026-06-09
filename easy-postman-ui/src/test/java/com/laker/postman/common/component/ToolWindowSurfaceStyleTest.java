package com.laker.postman.common.component;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.JScrollPane;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ToolWindowSurfaceStyleTest {
    private Map<String, Object> previousTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousTokens = new HashMap<>();
        for (String key : new String[]{
                ThemeColors.SURFACE,
                ThemeColors.INPUT_BACKGROUND,
                ThemeColors.BACKGROUND,
                ThemeColors.TAB_BACKGROUND
        }) {
            previousTokens.put(key, UIManager.get(key));
        }
    }

    @AfterMethod
    public void restoreThemeTokens() {
        previousTokens.forEach(UIManager::put);
    }

    @Test
    public void shouldApplyCardSurfaceToTabbedPane() {
        Color tabBackground = new Color(255, 255, 255);
        UIManager.put(ThemeColors.TAB_BACKGROUND, tabBackground);
        JTabbedPane tabs = new JTabbedPane();

        ToolWindowSurfaceStyle.applyTabbedPaneCard(tabs);

        assertTrue(tabs.isOpaque());
        assertEquals(tabs.getBackground(), tabBackground);
    }

    @Test
    public void shouldApplyCardSurfaceToTableScrollPane() {
        Color surface = new Color(250, 251, 252);
        Color input = new Color(246, 248, 250);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.INPUT_BACKGROUND, input);
        JTable table = new JTable(1, 1);
        JScrollPane scrollPane = new JScrollPane(table);

        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, table);

        assertEquals(scrollPane.getBackground(), surface);
        assertTrue(scrollPane.getBorder() instanceof EmptyBorder);
        assertTrue(scrollPane.getViewportBorder() instanceof EmptyBorder);
        assertEquals(scrollPane.getViewport().getBackground(), surface);
        assertEquals(table.getBackground(), surface);
        assertEquals(table.getTableHeader().getBackground(), input);
    }

    @Test
    public void shouldRefreshCardSurfaceWhenUiUpdates() throws Exception {
        Color initialSurface = new Color(250, 251, 252);
        Color nextSurface = new Color(24, 26, 28);
        UIManager.put(ThemeColors.SURFACE, initialSurface);
        JPanel panel = new JPanel();

        ToolWindowSurfaceStyle.applyCard(panel);
        assertEquals(panel.getBackground(), initialSurface);

        UIManager.put(ThemeColors.SURFACE, nextSurface);
        LookAndFeel previousLookAndFeel = UIManager.getLookAndFeel();
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            SwingUtilities.invokeAndWait(() -> {
            });
        } finally {
            if (previousLookAndFeel != null) {
                UIManager.setLookAndFeel(previousLookAndFeel);
            }
        }

        assertEquals(panel.getBackground(), nextSurface);
    }

    @Test
    public void shouldApplySectionHeaderSurfaceAndPadding() {
        Color surface = new Color(250, 251, 252);
        UIManager.put(ThemeColors.SURFACE, surface);
        JPanel panel = new JPanel();

        ToolWindowSurfaceStyle.applySectionHeader(panel, 1, 2, 3, 4);

        assertTrue(panel.isOpaque());
        assertEquals(panel.getBackground(), surface);
        assertTrue(panel.getBorder() instanceof EmptyBorder);
        assertEquals(panel.getInsets().top, 1);
        assertEquals(panel.getInsets().left, 2);
        assertEquals(panel.getInsets().bottom, 3);
        assertEquals(panel.getInsets().right, 4);
    }

    @Test
    public void shouldRefreshTextComponentCardWhenUiUpdates() throws Exception {
        Color initialSurface = new Color(250, 251, 252);
        Color nextSurface = new Color(24, 26, 28);
        UIManager.put(ThemeColors.SURFACE, initialSurface);
        JTextPane pane = new JTextPane();

        ToolWindowSurfaceStyle.applyTextComponentCard(pane);
        assertEquals(pane.getBackground(), initialSurface);

        UIManager.put(ThemeColors.SURFACE, nextSurface);
        LookAndFeel previousLookAndFeel = UIManager.getLookAndFeel();
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            SwingUtilities.invokeAndWait(() -> {
            });
        } finally {
            if (previousLookAndFeel != null) {
                UIManager.setLookAndFeel(previousLookAndFeel);
            }
        }

        assertEquals(pane.getBackground(), nextSurface);
    }

    @Test
    public void shouldRefreshTextComponentInputWhenUiUpdates() throws Exception {
        Color initialInput = new Color(246, 248, 250);
        Color nextInput = new Color(31, 34, 37);
        UIManager.put(ThemeColors.INPUT_BACKGROUND, initialInput);
        JTextPane pane = new JTextPane();

        ToolWindowSurfaceStyle.applyTextComponentInput(pane);
        assertEquals(pane.getBackground(), initialInput);

        UIManager.put(ThemeColors.INPUT_BACKGROUND, nextInput);
        LookAndFeel previousLookAndFeel = UIManager.getLookAndFeel();
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            SwingUtilities.invokeAndWait(() -> {
            });
        } finally {
            if (previousLookAndFeel != null) {
                UIManager.setLookAndFeel(previousLookAndFeel);
            }
        }

        assertEquals(pane.getBackground(), nextInput);
    }

    @Test
    public void shouldApplyCardSurfaceToListAndTree() {
        Color surface = new Color(250, 251, 252);
        Color selection = new Color(219, 234, 254);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.SELECTION_BACKGROUND, selection);
        JList<String> list = new JList<>(new String[]{"one"});
        JTree tree = new JTree();

        ToolWindowSurfaceStyle.applyListCard(list);
        ToolWindowSurfaceStyle.applyTreeCard(tree);

        assertEquals(list.getBackground(), surface);
        assertEquals(list.getSelectionBackground(), selection);
        assertEquals(tree.getBackground(), surface);
    }

    @Test
    public void shouldApplyCardSurfaceToListAndTreeScrollPanes() {
        Color surface = new Color(250, 251, 252);
        Color selection = new Color(219, 234, 254);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.SELECTION_BACKGROUND, selection);
        JList<String> list = new JList<>(new String[]{"one"});
        JScrollPane listScroll = new JScrollPane(list);
        JTree tree = new JTree();
        JScrollPane treeScroll = new JScrollPane(tree);

        ToolWindowSurfaceStyle.applyListScrollPaneCard(listScroll, list);
        ToolWindowSurfaceStyle.applyTreeScrollPaneCard(treeScroll, tree);

        assertEquals(listScroll.getBackground(), surface);
        assertEquals(listScroll.getViewport().getBackground(), surface);
        assertEquals(list.getBackground(), surface);
        assertEquals(list.getSelectionBackground(), selection);
        assertEquals(treeScroll.getBackground(), surface);
        assertEquals(treeScroll.getViewport().getBackground(), surface);
        assertEquals(tree.getBackground(), surface);
    }

    @Test
    public void shouldApplyCardSurfaceToPopupMenu() {
        Color surface = new Color(250, 251, 252);
        UIManager.put(ThemeColors.SURFACE, surface);
        JPopupMenu popupMenu = new JPopupMenu();

        ToolWindowSurfaceStyle.applyPopupMenuCard(popupMenu);

        assertTrue(popupMenu.isOpaque());
        assertEquals(popupMenu.getBackground(), surface);
    }

    @Test
    public void shouldApplyPanelTreeCardWithoutForcingTransparentPanelsOpaque() {
        Color surface = new Color(250, 251, 252);
        UIManager.put(ThemeColors.SURFACE, surface);
        JPanel root = new JPanel();
        JPanel transparentChild = new JPanel();
        transparentChild.setOpaque(false);
        root.add(transparentChild);

        ToolWindowSurfaceStyle.applyPanelTreeCard(root);

        assertTrue(root.isOpaque());
        assertEquals(root.getBackground(), surface);
        assertTrue(!transparentChild.isOpaque());
        assertEquals(transparentChild.getBackground(), surface);
    }

    @Test
    public void shouldPreserveRoundedToolWindowChromeBackgroundDuringPanelTreeStyling() {
        Color background = new Color(238, 242, 247);
        Color surface = new Color(250, 251, 252);
        UIManager.put(ThemeColors.BACKGROUND, background);
        UIManager.put(ThemeColors.SURFACE, surface);
        JPanel content = new JPanel();
        JComponent wrapper = ToolWindowChrome.wrapLeftToolWindow(content);
        RoundedToolWindowPanel roundedPanel = (RoundedToolWindowPanel) wrapper.getComponent(0);

        ToolWindowSurfaceStyle.applyPanelTreeCard(wrapper);

        assertEquals(wrapper.getBackground(), background);
        assertEquals(roundedPanel.getBackground(), background);
        assertEquals(content.getBackground(), surface);
    }

    @Test
    public void shouldPreserveToolWindowSplitGapBackgroundDuringPanelTreeStyling() {
        Color background = new Color(238, 242, 247);
        Color surface = new Color(250, 251, 252);
        UIManager.put(ThemeColors.BACKGROUND, background);
        UIManager.put(ThemeColors.SURFACE, surface);
        JPanel left = new JPanel();
        JPanel right = new JPanel();
        JSplitPane splitPane = ToolWindowChrome.createHorizontalCardSplitPane(left, right, 100);

        ToolWindowSurfaceStyle.applyPanelTreeCard(splitPane);

        assertEquals(splitPane.getBackground(), background);
        assertEquals(left.getBackground(), surface);
        assertEquals(right.getBackground(), surface);
    }
}
