package com.laker.postman.common.component.placeholder;

import com.laker.postman.common.constants.ModernColors;

import java.awt.*;

/**
 * 主窗口启动壳骨架屏。
 * 在真实主内容尚未加载完成时，用单面板自绘方式模拟左侧导航、顶部工具区、
 * 中间编辑区和下方结果区，避免先出现整块空白背景、再逐个显示子组件。
 */
public class StartupShellPlaceholderPanel extends AbstractPlaceholderPanel {

    public StartupShellPlaceholderPanel() {
        super(new BorderLayout());
        configureRoot(ModernColors.getBackgroundColor(), new Insets(18, 18, 18, 18));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            enableAntialias(g2);

            int width = getWidth();
            int height = getHeight();
            if (width <= 240 || height <= 180) {
                return;
            }

            boolean dark = isDarkTheme();
            Color cardBg = ModernColors.getCardBackgroundColor();
            Color borderColor = ModernColors.getDividerBorderColor();
            Color blockColor = dark ? new Color(77, 81, 86) : new Color(234, 239, 245);
            Color softBlockColor = dark ? new Color(69, 73, 78) : new Color(241, 245, 249);
            Color accentColor = dark ? new Color(100, 181, 246, 80) : new Color(59, 130, 246, 42);
            Color accentLineColor = dark ? new Color(100, 181, 246, 110) : new Color(59, 130, 246, 86);

            int outerGap = 14;
            int railX = 0;
            int railW = 58;
            int sidebarX = railX + railW + 10;
            int sidebarW = Math.min(300, Math.max(250, (width - 80) / 6));
            int mainX = sidebarX + sidebarW + 14;
            int mainW = width - mainX - 14;
            int topY = 0;
            int totalH = height;

            int statusH = 34;
            int tabsH = 40;
            int requestLineH = 56;
            int requestMetaH = 44;
            int responseH = Math.max(220, totalH / 3);
            int contentTopY = topY + tabsH + requestLineH + requestMetaH + outerGap;
            int editorH = totalH - contentTopY - responseH - statusH - outerGap * 2;

            paintToolRail(g2, railX, topY + 10, railW, totalH - statusH - 20, blockColor, accentColor);
            paintSidebar(g2, sidebarX, topY + tabsH + 4, sidebarW, totalH - statusH - tabsH - 18,
                    cardBg, borderColor, blockColor, softBlockColor, accentColor, accentLineColor);
            paintTabsBar(g2, mainX, topY + 6, mainW, tabsH - 8, blockColor, accentColor, accentLineColor);
            paintRequestLine(g2, mainX, topY + tabsH + 8, mainW, requestLineH - 10, cardBg, borderColor, blockColor, accentColor);
            paintMetaTabs(g2, mainX, topY + tabsH + requestLineH + 4, mainW, requestMetaH - 8, blockColor, accentColor, accentLineColor);
            paintEditorArea(g2, mainX, contentTopY, mainW, editorH, cardBg, borderColor, blockColor, softBlockColor, accentColor);
            paintResponseArea(g2, mainX, contentTopY + editorH + outerGap, mainW, responseH, cardBg, borderColor, blockColor, softBlockColor, accentColor);
            paintStatusBar(g2, 0, totalH - statusH, width, statusH, blockColor, accentColor);
        } finally {
            g2.dispose();
        }
    }

    private void paintToolRail(Graphics2D g2, int x, int y, int width, int height, Color blockColor, Color accentColor) {
        fillRoundedBlock(g2, x, y, width, height, ModernColors.getCardBackgroundColor(), 18);
        int currentY = y + 12;
        for (int i = 0; i < 7; i++) {
            int iconH = (i == 0) ? 56 : 44;
            int iconW = width - 14;
            Color fill = (i == 0) ? accentColor : blockColor;
            fillRoundedBlock(g2, x + 7, currentY, iconW, iconH, fill, 14);
            currentY += iconH + 12;
        }
    }

    private void paintSidebar(Graphics2D g2, int x, int y, int width, int height,
                              Color cardBg, Color borderColor, Color blockColor, Color softBlockColor,
                              Color accentColor, Color accentLineColor) {
        fillRoundedBlock(g2, x, y, width, height, cardBg, 20);
        g2.setColor(borderColor);
        g2.drawRoundRect(x, y, width, height, 20, 20);

        int innerX = x + 14;
        int currentY = y + 12;
        fillRoundedBlock(g2, innerX, currentY, width - 28, 30, softBlockColor, 12);
        currentY += 42;
        fillRoundedBlock(g2, innerX, currentY, width - 28, 24, accentColor, 12);
        currentY += 40;

        for (int i = 0; i < 15; i++) {
            int rowH = (i == 0) ? 28 : 20;
            Color rowColor = (i == 0) ? accentColor : blockColor;
            int indent = (i % 4 == 0) ? 0 : ((i % 4) * 16);
            fillRoundedBlock(g2, innerX + indent, currentY, width - 28 - indent, rowH, rowColor, 10);
            if (i == 0) {
                g2.setColor(accentLineColor);
                g2.fillRoundRect(innerX, currentY + rowH + 6, width - 28, 2, 2, 2);
            }
            currentY += rowH + 10;
            if (currentY > y + height - 120) {
                break;
            }
        }

        int footerY = y + height - 98;
        fillRoundedBlock(g2, innerX, footerY, width - 28, 18, softBlockColor, 10);
        fillRoundedBlock(g2, innerX, footerY + 28, width - 82, 16, softBlockColor, 10);
        fillRoundedBlock(g2, innerX, footerY + 54, width - 56, 16, softBlockColor, 10);
    }

    private void paintTabsBar(Graphics2D g2, int x, int y, int width, int height, Color blockColor, Color accentColor, Color accentLineColor) {
        int currentX = x + 10;
        int[] tabWidths = {180, 190, 200, 188};
        for (int i = 0; i < tabWidths.length; i++) {
            int tabW = Math.min(tabWidths[i], width / 5);
            Color fill = (i == 3) ? accentColor : blockColor;
            fillRoundedBlock(g2, currentX, y + 2, tabW, height - 10, fill, 10);
            if (i == 3) {
                g2.setColor(accentLineColor);
                g2.fillRoundRect(currentX, y + height - 8, tabW, 3, 3, 3);
            }
            currentX += tabW + 10;
        }
        fillRoundedBlock(g2, x + width - 42, y + 4, 24, 24, blockColor, 8);
    }

    private void paintRequestLine(Graphics2D g2, int x, int y, int width, int height,
                                  Color cardBg, Color borderColor, Color blockColor, Color accentColor) {
        fillRoundedBlock(g2, x, y, width, height, cardBg, 18);
        g2.setColor(borderColor);
        g2.drawRoundRect(x, y, width, height, 18, 18);

        int innerY = y + (height - 32) / 2;
        fillRoundedBlock(g2, x + 14, innerY, 110, 32, accentColor, 12);
        fillRoundedBlock(g2, x + 136, innerY, width - 320, 32, blockColor, 12);
        fillRoundedBlock(g2, x + width - 158, innerY, 66, 32, accentColor, 12);
        fillRoundedBlock(g2, x + width - 82, innerY, 66, 32, blockColor, 12);
    }

    private void paintMetaTabs(Graphics2D g2, int x, int y, int width, int height,
                               Color blockColor, Color accentColor, Color accentLineColor) {
        int currentX = x + 6;
        int[] widths = {52, 54, 50, 62, 46, 42};
        for (int i = 0; i < widths.length; i++) {
            Color fill = (i == 3) ? accentColor : blockColor;
            fillRoundedBlock(g2, currentX, y + 6, widths[i], 18, fill, 8);
            if (i == 3) {
                g2.setColor(accentLineColor);
                g2.fillRoundRect(currentX, y + 30, widths[i], 3, 3, 3);
            }
            currentX += widths[i] + 16;
        }
    }

    private void paintEditorArea(Graphics2D g2, int x, int y, int width, int height,
                                 Color cardBg, Color borderColor, Color blockColor, Color softBlockColor, Color accentColor) {
        fillRoundedBlock(g2, x, y, width, height, cardBg, 16);
        g2.setColor(borderColor);
        g2.drawRoundRect(x, y, width, height, 16, 16);

        fillRoundedBlock(g2, x, y, 40, height, accentColor, 0);
        int gutterX = x + 52;
        for (int i = 0; i < 12; i++) {
            fillRoundedBlock(g2, gutterX, y + 18 + i * 24, 18, 14, blockColor, 6);
        }

        int textX = x + 92;
        int currentY = y + 18;
        fillRoundedBlock(g2, textX, currentY, 180, 18, accentColor, 8);
        currentY += 34;
        for (int i = 0; i < 8; i++) {
            fillRoundedBlock(g2, textX, currentY, Math.max(220, width - 140 - (i % 3) * 90), 16, softBlockColor, 8);
            currentY += 24;
        }
    }

    private void paintResponseArea(Graphics2D g2, int x, int y, int width, int height,
                                   Color cardBg, Color borderColor, Color blockColor, Color softBlockColor, Color accentColor) {
        fillRoundedBlock(g2, x, y, width, height, cardBg, 16);
        g2.setColor(borderColor);
        g2.drawRoundRect(x, y, width, height, 16, 16);

        int tabX = x + 12;
        int[] tabWidths = {66, 62, 70, 74, 68};
        for (int i = 0; i < tabWidths.length; i++) {
            Color fill = (i == 0) ? accentColor : blockColor;
            fillRoundedBlock(g2, tabX, y + 12, tabWidths[i], 18, fill, 8);
            tabX += tabWidths[i] + 14;
        }

        g2.setColor(borderColor);
        g2.drawLine(x, y + 42, x + width, y + 42);

        int contentY = y + 56;
        fillRoundedBlock(g2, x + 12, contentY, 64, 28, blockColor, 10);
        fillRoundedBlock(g2, x + width - 188, contentY + 2, 24, 24, blockColor, 8);
        fillRoundedBlock(g2, x + width - 154, contentY + 2, 24, 24, blockColor, 8);
        fillRoundedBlock(g2, x + width - 120, contentY + 2, 24, 24, blockColor, 8);

        int bodyY = contentY + 40;
        fillRoundedBlock(g2, x + 16, bodyY, width - 32, height - 56 - 18, softBlockColor, 10);
    }

    private void paintStatusBar(Graphics2D g2, int x, int y, int width, int height, Color blockColor, Color accentColor) {
        g2.setColor(ModernColors.getCardBackgroundColor());
        g2.fillRect(x, y, width, height);
        g2.setColor(ModernColors.getDividerBorderColor());
        g2.drawLine(x, y, x + width, y);

        fillRoundedBlock(g2, x + 12, y + 6, 24, 22, blockColor, 8);
        fillRoundedBlock(g2, x + 44, y + 6, 34, 22, accentColor, 8);
        fillRoundedBlock(g2, x + width - 160, y + 7, 84, 20, blockColor, 8);
        fillRoundedBlock(g2, x + width - 66, y + 7, 44, 20, blockColor, 8);
    }
}
