package com.laker.postman.common.component.placeholder;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.service.setting.SettingManager;

import javax.swing.*;
import java.awt.*;

/**
 * 请求编辑器延迟加载骨架屏。
 * 复用真实请求编辑区的结构关系：请求行、子标签、请求区/响应区分栏，
 * 让 deferred editor 出现时更接近真实界面，而不是一张泛化卡片。
 */
public class RequestEditorPlaceholderPanel extends AbstractPlaceholderPanel {

    private static final int REQUEST_LINE_HEIGHT = 44;
    private static final int META_TABS_HEIGHT = 34;
    private static final int RESPONSE_TABS_HEIGHT = 34;

    public RequestEditorPlaceholderPanel() {
        super(new BorderLayout());
        configureRoot(ModernColors.getBackgroundColor(), new Insets(0, 0, 0, 0));

        add(createRequestLineShell(), BorderLayout.NORTH);
        add(createSplitShell(), BorderLayout.CENTER);
    }

    private JComponent createRequestLineShell() {
        JPanel shell = createTransparentPanel(new BorderLayout());
        shell.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()));
        shell.setPreferredSize(new Dimension(0, REQUEST_LINE_HEIGHT));
        return shell;
    }

    private JComponent createSplitShell() {
        boolean vertical = SettingManager.isLayoutVertical();
        JSplitPane splitPane = new JSplitPane(
                vertical ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT,
                createRequestShell(),
                createResponseShell()
        );
        splitPane.setDividerSize(4);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setResizeWeight(vertical ? 0.58 : 0.5);
        if (vertical) {
            splitPane.setDividerLocation(0.58d);
        } else {
            splitPane.setDividerLocation(0.5d);
        }
        splitPane.getTopComponent().setMinimumSize(new Dimension(0, 0));
        splitPane.getBottomComponent().setMinimumSize(new Dimension(0, 0));
        splitPane.getLeftComponent().setMinimumSize(new Dimension(0, 0));
        splitPane.getRightComponent().setMinimumSize(new Dimension(0, 0));
        return splitPane;
    }

    private JComponent createRequestShell() {
        JPanel shell = createTransparentPanel(new BorderLayout());
        shell.add(new MetaTabsShell(), BorderLayout.NORTH);
        shell.add(new EditorBodyShell(), BorderLayout.CENTER);
        return shell;
    }

    private JComponent createResponseShell() {
        JPanel shell = createTransparentPanel(new BorderLayout());
        shell.add(new ResponseTabsShell(), BorderLayout.NORTH);
        shell.add(new ResponseBodyShell(), BorderLayout.CENTER);
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

    private class MetaTabsShell extends JPanel {
        private MetaTabsShell() {
            setOpaque(true);
            setBackground(ModernColors.getBackgroundColor());
            setPreferredSize(new Dimension(0, META_TABS_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                int currentX = 8;
                int[] widths = {46, 50, 50, 56, 44, 40, 38};
                for (int i = 0; i < widths.length; i++) {
                    Color fill = i == 3 ? accentColor() : blockColor();
                    fillRoundedBlock(g2, currentX, 8, widths[i], 16, fill, 8);
                    if (i == 3) {
                        g2.setColor(accentLineColor());
                        g2.fillRoundRect(currentX, 28, widths[i], 3, 3, 3);
                    }
                    currentX += widths[i] + 14;
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private class EditorBodyShell extends JPanel {
        private EditorBodyShell() {
            setOpaque(true);
            setBackground(ModernColors.getCardBackgroundColor());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                int topY = 12;
                fillRoundedBlock(g2, 12, topY, 54, 24, blockColor(), 8);
                fillRoundedBlock(g2, 76, topY, 66, 24, accentColor(), 8);
                fillRoundedBlock(g2, 152, topY, 24, 24, blockColor(), 8);
                fillRoundedBlock(g2, 184, topY, 24, 24, blockColor(), 8);

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

    private class ResponseTabsShell extends JPanel {
        private ResponseTabsShell() {
            setOpaque(true);
            setBackground(ModernColors.getBackgroundColor());
            setPreferredSize(new Dimension(0, RESPONSE_TABS_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                enableAntialias(g2);
                int currentX = 12;
                int[] widths = {60, 58, 66, 70, 62};
                for (int i = 0; i < widths.length; i++) {
                    fillRoundedBlock(g2, currentX, 12, widths[i], 18, i == 0 ? accentColor() : blockColor(), 8);
                    currentX += widths[i] + 12;
                }
                g2.setColor(ModernColors.getDividerBorderColor());
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            } finally {
                g2.dispose();
            }
        }
    }

    private class ResponseBodyShell extends JPanel {
        private ResponseBodyShell() {
            setOpaque(true);
            setBackground(ModernColors.getCardBackgroundColor());
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            enableAntialias(g2);
            int y = (REQUEST_LINE_HEIGHT - 30) / 2;
            fillRoundedBlock(g2, 10, y, 104, 30, blockColor(), 10);
            fillRoundedBlock(g2, 124, y, Math.max(240, getWidth() - 300), 30, softBlockColor(), 10);
            fillRoundedBlock(g2, getWidth() - 170, y, 86, 30, accentColor(), 10);
            fillRoundedBlock(g2, getWidth() - 74, y, 62, 30, blockColor(), 10);
        } finally {
            g2.dispose();
        }
    }
}
