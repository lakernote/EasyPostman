package com.laker.postman.common.component.placeholder;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.service.setting.SettingManager;

import javax.swing.*;
import java.awt.*;

/**
 * 主窗口启动壳骨架屏。
 * 只复用主界面的外层分栏关系，不创建真实树、编辑器和响应组件，
 * 这样既能和真实界面位置对齐，又不会把启动链路重新变重。
 */
public class StartupShellPlaceholderPanel extends AbstractPlaceholderPanel {

    private static final int COLLECTIONS_DIVIDER_LOCATION = 250;
    private static final int REQUEST_TABS_HEIGHT = 38;
    private static final int REQUEST_LINE_HEIGHT = 44;
    private static final int REQUEST_META_HEIGHT = 34;
    private static final int RESPONSE_TABS_HEIGHT = 34;
    private static final int STATUS_BAR_HEIGHT = 30;

    public StartupShellPlaceholderPanel() {
        super(new BorderLayout());
        configureRoot(ModernColors.getBackgroundColor(), new Insets(0, 0, 0, 0));

        add(createMainShell(), BorderLayout.CENTER);
        add(createBottomBarShell(), BorderLayout.SOUTH);
    }

    private JComponent createMainShell() {
        JPanel panel = createTransparentPanel(new BorderLayout());
        panel.add(createSidebarRailShell(), BorderLayout.WEST);
        panel.add(createCollectionsShell(), BorderLayout.CENTER);
        return panel;
    }

    private JComponent createSidebarRailShell() {
        boolean expanded = SettingManager.isSidebarExpanded();
        SidebarRailShell rail = new SidebarRailShell(expanded);
        rail.setPreferredSize(new Dimension(expanded ? 104 : 58, 0));
        return rail;
    }

    private JComponent createCollectionsShell() {
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createTreeShell(),
                createRequestWorkspaceShell()
        );
        splitPane.setDividerSize(3);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setResizeWeight(0);
        splitPane.setDividerLocation(COLLECTIONS_DIVIDER_LOCATION);
        splitPane.getLeftComponent().setMinimumSize(new Dimension(220, 0));
        splitPane.getRightComponent().setMinimumSize(new Dimension(0, 0));
        return splitPane;
    }

    private JComponent createTreeShell() {
        CollectionsTreeShell treeShell = new CollectionsTreeShell();
        treeShell.setPreferredSize(new Dimension(COLLECTIONS_DIVIDER_LOCATION, 0));
        return treeShell;
    }

    private JComponent createRequestWorkspaceShell() {
        JPanel panel = createTransparentPanel(new BorderLayout());
        panel.add(createRequestTabsShell(), BorderLayout.NORTH);
        panel.add(createRequestEditorShell(), BorderLayout.CENTER);
        return panel;
    }

    private JComponent createRequestTabsShell() {
        TabsStripShell shell = new TabsStripShell(true, false, REQUEST_TABS_HEIGHT);
        shell.setPreferredSize(new Dimension(0, REQUEST_TABS_HEIGHT));
        return shell;
    }

    private JComponent createRequestEditorShell() {
        JPanel panel = createTransparentPanel(new BorderLayout());

        RequestLineShell requestLineShell = new RequestLineShell();
        requestLineShell.setPreferredSize(new Dimension(0, REQUEST_LINE_HEIGHT));
        panel.add(requestLineShell, BorderLayout.NORTH);

        boolean vertical = SettingManager.isLayoutVertical();
        JSplitPane splitPane = new JSplitPane(
                vertical ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT,
                createRequestBodyShell(),
                createResponseShell()
        );
        splitPane.setDividerSize(4);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setResizeWeight(0.5);
        if (vertical) {
            splitPane.setDividerLocation(0.58d);
        } else {
            splitPane.setDividerLocation(0.52d);
        }
        splitPane.getTopComponent().setMinimumSize(new Dimension(0, 0));
        splitPane.getBottomComponent().setMinimumSize(new Dimension(0, 0));
        splitPane.getLeftComponent().setMinimumSize(new Dimension(0, 0));
        splitPane.getRightComponent().setMinimumSize(new Dimension(0, 0));
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JComponent createRequestBodyShell() {
        JPanel panel = createTransparentPanel(new BorderLayout());
        TabsStripShell metaTabsShell = new TabsStripShell(false, true, REQUEST_META_HEIGHT);
        metaTabsShell.setPreferredSize(new Dimension(0, REQUEST_META_HEIGHT));
        panel.add(metaTabsShell, BorderLayout.NORTH);
        panel.add(new EditorCanvasShell(), BorderLayout.CENTER);
        return panel;
    }

    private JComponent createResponseShell() {
        JPanel panel = createTransparentPanel(new BorderLayout());
        TabsStripShell responseTabsShell = new TabsStripShell(false, false, RESPONSE_TABS_HEIGHT);
        responseTabsShell.setPreferredSize(new Dimension(0, RESPONSE_TABS_HEIGHT));
        panel.add(responseTabsShell, BorderLayout.NORTH);
        panel.add(new ResponseCanvasShell(), BorderLayout.CENTER);
        return panel;
    }

    private JComponent createBottomBarShell() {
        BottomBarShell shell = new BottomBarShell();
        shell.setPreferredSize(new Dimension(0, STATUS_BAR_HEIGHT));
        return shell;
    }

    private Color blockColor() {
        return isDarkTheme() ? new Color(77, 81, 86) : new Color(234, 239, 245);
    }

    private Color softBlockColor() {
        return isDarkTheme() ? new Color(69, 73, 78) : new Color(241, 245, 249);
    }

    private Color accentColor() {
        return isDarkTheme() ? new Color(100, 181, 246, 80) : new Color(59, 130, 246, 42);
    }

    private Color accentLineColor() {
        return isDarkTheme() ? new Color(100, 181, 246, 110) : new Color(59, 130, 246, 86);
    }

    private class SidebarRailShell extends JPanel {
        private final boolean expanded;

        private SidebarRailShell(boolean expanded) {
            this.expanded = expanded;
            setOpaque(true);
            setBackground(ModernColors.getBackgroundColor());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                int width = getWidth();
                int currentY = 8;
                for (int i = 0; i < 7; i++) {
                    int height = expanded ? 40 : (i == 0 ? 56 : 42);
                    int x = expanded ? 8 : 6;
                    int w = width - (expanded ? 14 : 12);
                    fillRoundedBlock(g2, x, currentY, w, height, i == 0 ? accentColor() : blockColor(), 12);
                    if (expanded) {
                        fillRoundedBlock(g2, x + 10, currentY + 10, 18, 18, softBlockColor(), 8);
                        fillRoundedBlock(g2, x + 34, currentY + 11, Math.max(28, w - 48), 14, i == 0 ? softBlockColor() : accentColor(), 7);
                    }
                    currentY += height + 10;
                }
                g2.setColor(ModernColors.getDividerBorderColor());
                g2.drawLine(width - 1, 0, width - 1, getHeight());
            } finally {
                g2.dispose();
            }
        }
    }

    private class CollectionsTreeShell extends JPanel {
        private CollectionsTreeShell() {
            setOpaque(true);
            setBackground(ModernColors.getBackgroundColor());
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ModernColors.getDividerBorderColor()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                int width = getWidth();
                int currentY = 12;
                fillRoundedBlock(g2, 14, currentY, width - 28, 32, softBlockColor(), 12);
                currentY += 48;

                int[] indents = {0, 18, 18, 18, 0, 18, 18, 0, 18, 36, 36, 18, 0, 18, 18, 18, 0, 18};
                for (int i = 0; i < indents.length; i++) {
                    int rowH = indents[i] == 0 ? 26 : 18;
                    int rowWidth = Math.max(120, width - 28 - indents[i] - ((i % 3) == 0 ? 0 : 20));
                    Color color = indents[i] == 0 ? accentColor() : blockColor();
                    fillRoundedBlock(g2, 14 + indents[i], currentY, rowWidth, rowH, color, 9);
                    currentY += rowH + 8;
                    if (currentY > getHeight() - 110) {
                        break;
                    }
                }

                int footerY = getHeight() - 96;
                fillRoundedBlock(g2, 14, footerY, width - 28, 18, softBlockColor(), 8);
                fillRoundedBlock(g2, 14, footerY + 28, width - 88, 16, softBlockColor(), 8);
                fillRoundedBlock(g2, 14, footerY + 54, width - 62, 16, softBlockColor(), 8);
            } finally {
                g2.dispose();
            }
        }
    }

    private class TabsStripShell extends JPanel {
        private final boolean requestTabs;
        private final boolean requestMetaTabs;
        private final int preferredHeight;

        private TabsStripShell(boolean requestTabs, boolean requestMetaTabs, int preferredHeight) {
            this.requestTabs = requestTabs;
            this.requestMetaTabs = requestMetaTabs;
            this.preferredHeight = preferredHeight;
            setOpaque(true);
            setBackground(ModernColors.getBackgroundColor());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                int currentX = requestTabs ? 8 : 10;
                int baseY = requestTabs ? 5 : 8;
                int height = requestTabs ? preferredHeight - 10 : 16;
                int[] widths = requestTabs ? new int[]{178, 188, 200, 186} :
                        (requestMetaTabs ? new int[]{46, 50, 50, 56, 44, 40, 38} : new int[]{60, 58, 66, 70, 62});

                for (int i = 0; i < widths.length; i++) {
                    Color fill = (requestTabs && i == 3) || (requestMetaTabs && i == 3) || (!requestTabs && !requestMetaTabs && i == 0)
                            ? accentColor() : blockColor();
                    fillRoundedBlock(g2, currentX, baseY, widths[i], height, fill, 8);
                    if ((requestTabs && i == 3) || (requestMetaTabs && i == 3)) {
                        g2.setColor(accentLineColor());
                        g2.fillRoundRect(currentX, baseY + height + 4, widths[i], 3, 3, 3);
                    }
                    currentX += widths[i] + (requestTabs ? 8 : 14);
                }

                if (requestTabs) {
                    fillRoundedBlock(g2, getWidth() - 34, 7, 22, 22, blockColor(), 8);
                    g2.setColor(ModernColors.getDividerBorderColor());
                    g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private class RequestLineShell extends JPanel {
        private RequestLineShell() {
            setOpaque(true);
            setBackground(ModernColors.getBackgroundColor());
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                int y = (getHeight() - 30) / 2;
                fillRoundedBlock(g2, 10, y, 104, 30, blockColor(), 10);
                fillRoundedBlock(g2, 124, y, Math.max(240, getWidth() - 300), 30, softBlockColor(), 10);
                fillRoundedBlock(g2, getWidth() - 170, y, 86, 30, accentColor(), 10);
                fillRoundedBlock(g2, getWidth() - 74, y, 62, 30, blockColor(), 10);
            } finally {
                g2.dispose();
            }
        }
    }

    private class EditorCanvasShell extends JPanel {
        private EditorCanvasShell() {
            setOpaque(true);
            setBackground(ModernColors.getCardBackgroundColor());
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, ModernColors.getDividerBorderColor()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                int toolbarY = 12;
                fillRoundedBlock(g2, 12, toolbarY, 54, 24, blockColor(), 8);
                fillRoundedBlock(g2, 76, toolbarY, 66, 24, accentColor(), 8);
                fillRoundedBlock(g2, 152, toolbarY, 24, 24, blockColor(), 8);
                fillRoundedBlock(g2, 184, toolbarY, 24, 24, blockColor(), 8);

                int codeTop = 48;
                for (int i = 0; i < 12; i++) {
                    fillRoundedBlock(g2, 22, codeTop + 12 + i * 24, 18, 14, blockColor(), 6);
                }
                int currentY = codeTop + 12;
                fillRoundedBlock(g2, 58, currentY, 170, 16, accentColor(), 8);
                currentY += 28;
                for (int i = 0; i < 9; i++) {
                    fillRoundedBlock(g2, 58, currentY, Math.max(220, getWidth() - 104 - (i % 3) * 90), 15, softBlockColor(), 8);
                    currentY += 22;
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private class ResponseCanvasShell extends JPanel {
        private ResponseCanvasShell() {
            setOpaque(true);
            setBackground(ModernColors.getCardBackgroundColor());
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, ModernColors.getDividerBorderColor()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                fillRoundedBlock(g2, 12, 14, 60, 26, blockColor(), 8);
                fillRoundedBlock(g2, getWidth() - 182, 15, 24, 24, blockColor(), 8);
                fillRoundedBlock(g2, getWidth() - 148, 15, 24, 24, blockColor(), 8);
                fillRoundedBlock(g2, getWidth() - 114, 15, 24, 24, blockColor(), 8);
                fillRoundedBlock(g2, 16, 54, Math.max(180, getWidth() - 32), Math.max(80, getHeight() - 72), softBlockColor(), 10);
            } finally {
                g2.dispose();
            }
        }
    }

    private class BottomBarShell extends JPanel {
        private BottomBarShell() {
            setOpaque(true);
            setBackground(ModernColors.getCardBackgroundColor());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                g2.setColor(ModernColors.getDividerBorderColor());
                g2.drawLine(0, 0, getWidth(), 0);
                fillRoundedBlock(g2, 12, 5, 24, 20, blockColor(), 8);
                fillRoundedBlock(g2, 44, 5, 34, 20, accentColor(), 8);
                fillRoundedBlock(g2, getWidth() - 160, 5, 84, 20, blockColor(), 8);
                fillRoundedBlock(g2, getWidth() - 66, 5, 44, 20, blockColor(), 8);
            } finally {
                g2.dispose();
            }
        }
    }
}
