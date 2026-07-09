package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.JScrollPane;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
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
                ThemeColors.WINDOW_CHROME_BACKGROUND,
                ThemeColors.DIALOG_CHROME_BACKGROUND,
                ThemeColors.TAB_BACKGROUND,
                ThemeColors.TAB_SELECTED_BACKGROUND,
                ThemeColors.TAB_SEPARATOR,
                ThemeColors.SELECTION_BACKGROUND,
                ThemeColors.BORDER_LIGHT,
                "Panel.background",
                "Table.background",
                "Table.gridColor",
                "Table.selectionBackground",
                "Table.selectionForeground",
                "TableHeader.background"
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
        Color tableBackground = new Color(255, 255, 255);
        Color headerBackground = new Color(246, 248, 250);
        Color grid = new Color(230, 233, 238);
        Color selection = new Color(226, 235, 254);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put("Table.background", tableBackground);
        UIManager.put("TableHeader.background", headerBackground);
        UIManager.put("Table.gridColor", grid);
        UIManager.put("Table.selectionBackground", selection);
        JTable table = new JTable(1, 1);
        JScrollPane scrollPane = new JScrollPane(table);

        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, table);

        assertEquals(scrollPane.getBackground(), surface);
        assertTrue(scrollPane.getBorder() instanceof EmptyBorder);
        assertTrue(scrollPane.getViewportBorder() instanceof EmptyBorder);
        assertEquals(scrollPane.getViewport().getBackground(), surface);
        assertEquals(scrollPane.getColumnHeader().getBackground(), headerBackground);
        assertEquals(table.getBackground(), tableBackground);
        assertEquals(table.getGridColor(), grid);
        assertEquals(table.getSelectionBackground(), selection);
        assertEquals(table.getIntercellSpacing(), new Dimension(1, 1));
        assertEquals(table.getRowMargin(), 1);
        assertEquals(table.getTableHeader().getBackground(), headerBackground);
        assertTrue(table.getTableHeader().getPreferredSize().height >= 32);
    }

    @Test
    public void shouldApplyFramedScrollPaneCardWithThinThemeBorder() {
        Color surface = new Color(250, 251, 252);
        Color border = new Color(211, 218, 230);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.BORDER_LIGHT, border);
        JScrollPane scrollPane = new JScrollPane(new JPanel());

        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(scrollPane);

        assertEquals(scrollPane.getBackground(), surface);
        assertEquals(scrollPane.getViewport().getBackground(), surface);
        assertTrue(scrollPane.getBorder() instanceof LineBorder);
        assertEquals(((LineBorder) scrollPane.getBorder()).getLineColor(), border);
        assertTrue(scrollPane.getViewportBorder() instanceof EmptyBorder);
    }

    @Test
    public void shouldPreserveFramedScrollPaneCardWhenApplyingPanelTreeCard() {
        Color surface = new Color(250, 251, 252);
        Color border = new Color(211, 218, 230);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.BORDER_LIGHT, border);
        JPanel parent = new JPanel();
        JScrollPane scrollPane = new JScrollPane(new JPanel());
        parent.add(scrollPane);

        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(scrollPane);
        ToolWindowSurfaceStyle.applyPanelTreeCard(parent);

        assertEquals(scrollPane.getBackground(), surface);
        assertEquals(scrollPane.getViewport().getBackground(), surface);
        assertTrue(scrollPane.getBorder() instanceof LineBorder);
        assertEquals(((LineBorder) scrollPane.getBorder()).getLineColor(), border);
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
    public void shouldKeepExplicitCardSurfaceThroughComponentUiUpdate() throws Exception {
        Color surface = new ColorUIResource(250, 251, 252);
        Color panelBackground = new ColorUIResource(233, 234, 238);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put("Panel.background", panelBackground);
        JPanel panel = new JPanel();

        ToolWindowSurfaceStyle.applyCard(panel);
        assertEquals(panel.getBackground(), surface);

        SwingUtilities.invokeAndWait(panel::updateUI);

        assertEquals(panel.getBackground(), surface);
    }

    @Test
    public void shouldRemoveThemeRefreshListenerWhenComponentIsNoLongerDisplayable() throws Exception {
        JPanel panel = new JPanel();
        int initialListeners = UIManager.getPropertyChangeListeners().length;

        ToolWindowSurfaceStyle.applyCard(panel);

        assertEquals(UIManager.getPropertyChangeListeners().length, initialListeners + 1);

        HierarchyEvent displayabilityChanged = new HierarchyEvent(
                panel,
                HierarchyEvent.HIERARCHY_CHANGED,
                panel,
                null,
                HierarchyEvent.DISPLAYABILITY_CHANGED
        );
        for (HierarchyListener listener : panel.getHierarchyListeners()) {
            listener.hierarchyChanged(displayabilityChanged);
        }
        SwingUtilities.invokeAndWait(() -> {
        });

        assertEquals(UIManager.getPropertyChangeListeners().length, initialListeners);
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
    public void shouldApplyToolWindowToolbarSeparatorWithThemeColorAndPadding() {
        Color surface = new Color(250, 251, 252);
        Color separator = new Color(229, 231, 235);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.TAB_SEPARATOR, separator);
        JPanel panel = new JPanel();

        ToolWindowSurfaceStyle.applyToolWindowToolbarSeparator(panel, 4, 6, 4, 6);

        assertTrue(panel.isOpaque());
        assertEquals(panel.getBackground(), surface);
        assertTrue(panel.getBorder() instanceof CompoundBorder);
        CompoundBorder border = (CompoundBorder) panel.getBorder();
        assertTrue(border.getOutsideBorder() instanceof MatteBorder);
        MatteBorder separatorBorder = (MatteBorder) border.getOutsideBorder();
        assertEquals(separatorBorder.getBorderInsets(panel).bottom, 1);
        assertEquals(separatorBorder.getMatteColor(), separator);
        assertEquals(panel.getInsets().top, 4);
        assertEquals(panel.getInsets().left, 6);
        assertEquals(panel.getInsets().bottom, 5);
        assertEquals(panel.getInsets().right, 6);
    }

    @Test
    public void shouldApplyDialogRightSeparatorWithThemeColor() {
        Color surface = new Color(246, 247, 249);
        Color separator = new Color(218, 221, 228);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, surface);
        UIManager.put(ThemeColors.TAB_SEPARATOR, separator);
        JPanel panel = new JPanel();

        ToolWindowSurfaceStyle.applyDialogRightSeparator(panel);

        assertTrue(panel.isOpaque());
        assertEquals(panel.getBackground(), surface);
        assertTrue(panel.getBorder() instanceof MatteBorder);
        MatteBorder border = (MatteBorder) panel.getBorder();
        assertEquals(border.getBorderInsets(panel).right, 1);
        assertEquals(border.getMatteColor(), separator);
    }

    @Test
    public void shouldApplySectionCardWithThinBorderAndPadding() {
        Color surface = new Color(250, 251, 252);
        Color border = new Color(211, 218, 230);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.BORDER_LIGHT, border);
        JPanel panel = new JPanel();

        ToolWindowSurfaceStyle.applySectionCard(panel);

        assertTrue(panel.isOpaque());
        assertEquals(panel.getBackground(), surface);
        assertTrue(panel.getBorder() instanceof CompoundBorder);
        assertEquals(panel.getInsets().top, 9);
        assertEquals(panel.getInsets().left, 11);
        assertEquals(panel.getInsets().bottom, 11);
        assertEquals(panel.getInsets().right, 11);
    }

    @Test
    public void shouldApplyDialogSurfaceToDialogRoot() {
        Color chrome = new Color(247, 248, 249);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, chrome);
        JPanel panel = new JPanel();

        ToolWindowSurfaceStyle.applyDialogSurface(panel);

        assertTrue(panel.isOpaque());
        assertEquals(panel.getBackground(), chrome);
    }

    @Test
    public void shouldApplyDialogFooterBackgroundAndSeparatorPadding() {
        Color chrome = new Color(247, 248, 249);
        Color separator = new Color(233, 234, 238);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, chrome);
        UIManager.put(ThemeColors.TAB_SEPARATOR, separator);
        JPanel panel = new JPanel();

        ToolWindowSurfaceStyle.applyDialogFooter(panel);

        assertTrue(panel.isOpaque());
        assertEquals(panel.getBackground(), chrome);
        assertTrue(panel.getBorder() instanceof CompoundBorder);
        assertEquals(panel.getInsets().top, 11);
        assertEquals(panel.getInsets().left, 16);
        assertEquals(panel.getInsets().bottom, 10);
        assertEquals(panel.getInsets().right, 16);
    }

    @Test
    public void shouldApplyDialogBackgroundToScrollPaneAndViewport() {
        Color chrome = new Color(247, 248, 249);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, chrome);
        JScrollPane scrollPane = new JScrollPane(new JPanel());

        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);

        assertEquals(scrollPane.getBackground(), chrome);
        assertEquals(scrollPane.getViewport().getBackground(), chrome);
        assertTrue(scrollPane.getBorder() instanceof EmptyBorder);
        assertTrue(scrollPane.getViewportBorder() instanceof EmptyBorder);
    }

    @Test
    public void shouldApplyDialogBackgroundToDialogList() {
        Color chrome = new Color(247, 248, 249);
        Color selected = new Color(226, 235, 254);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, chrome);
        UIManager.put(ThemeColors.TAB_SELECTED_BACKGROUND, selected);
        JList<String> list = new JList<>(new String[]{"one"});
        JScrollPane scrollPane = new JScrollPane(list);

        ToolWindowSurfaceStyle.applyDialogListScrollPane(scrollPane, list);

        assertEquals(scrollPane.getBackground(), chrome);
        assertEquals(scrollPane.getViewport().getBackground(), chrome);
        assertEquals(list.getBackground(), chrome);
        assertEquals(list.getSelectionBackground(), selected);
    }

    @Test
    public void shouldApplyDialogBackgroundToDialogSplitPane() {
        Color chrome = new Color(247, 248, 249);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, chrome);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JPanel(), new JPanel());

        ToolWindowSurfaceStyle.applyDialogSplitPane(splitPane);

        assertEquals(splitPane.getBackground(), chrome);
        assertTrue(splitPane.getBorder() instanceof EmptyBorder);
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        assertEquals(divider.getBackground(), chrome);
        assertTrue(divider.getBorder() instanceof EmptyBorder);
    }

    @Test
    public void shouldApplyDialogWindowChromeToRootPaneAndTitleBar() {
        Color chrome = new Color(247, 248, 249);
        Color windowChrome = new Color(233, 234, 238);
        Color foreground = new Color(12, 13, 14);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, chrome);
        UIManager.put(ThemeColors.WINDOW_CHROME_BACKGROUND, windowChrome);
        UIManager.put("Menu.foreground", foreground);
        JRootPane rootPane = new JRootPane();

        ToolWindowSurfaceStyle.applyDialogWindowChrome(rootPane);

        assertTrue(rootPane.isOpaque());
        assertEquals(rootPane.getBackground(), chrome);
        assertEquals(rootPane.getClientProperty(FlatClientProperties.TITLE_BAR_BACKGROUND), chrome);
        assertEquals(rootPane.getClientProperty(FlatClientProperties.TITLE_BAR_FOREGROUND), foreground);
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
