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

            int gap = 18;
            int leftX = 18;
            int topY = 18;
            int contentW = width - 36;
            int contentH = height - 36;

            int sidebarW = Math.min(250, Math.max(200, contentW / 5));
            int rightX = leftX + sidebarW + gap;
            int rightW = width - rightX - 18;

            int topCardH = Math.min(86, Math.max(72, contentH / 10));
            int mainCardH = Math.min(320, Math.max(240, contentH / 2));
            int bottomCardH = contentH - topCardH - mainCardH - gap * 2;
            if (bottomCardH < 140) {
                bottomCardH = 140;
                mainCardH = contentH - topCardH - bottomCardH - gap * 2;
            }

            paintSidebar(g2, leftX, topY, sidebarW, contentH, cardBg, borderColor, blockColor, accentColor);
            paintTopBar(g2, rightX, topY, rightW, topCardH, cardBg, borderColor, blockColor, accentColor);
            paintMainEditor(g2, rightX, topY + topCardH + gap, rightW, mainCardH, cardBg, borderColor, blockColor, softBlockColor, accentColor);
            paintBottomArea(g2, rightX, topY + topCardH + gap + mainCardH + gap, rightW, bottomCardH,
                    cardBg, borderColor, blockColor, softBlockColor, accentColor);
        } finally {
            g2.dispose();
        }
    }

    private void paintSidebar(Graphics2D g2, int x, int y, int width, int height,
                              Color cardBg, Color borderColor, Color blockColor, Color accentColor) {
        fillRoundedBlock(g2, x, y, width, height, cardBg, 20);
        g2.setColor(borderColor);
        g2.drawRoundRect(x, y, width, height, 20, 20);

        int innerX = x + 18;
        int currentY = y + 18;
        fillRoundedBlock(g2, innerX, currentY, width - 36, 28, accentColor, 14);
        currentY += 46;

        int[] navHeights = {42, 34, 34, 34, 34, 34};
        for (int i = 0; i < navHeights.length; i++) {
            int h = navHeights[i];
            Color fill = (i == 0) ? accentColor : blockColor;
            fillRoundedBlock(g2, innerX, currentY, width - 36, h, fill, 14);
            currentY += h + 12;
        }

        currentY = Math.max(currentY + 8, y + height - 118);
        fillRoundedBlock(g2, innerX, currentY, width - 36, 16, blockColor, 10);
        fillRoundedBlock(g2, innerX, currentY + 28, width - 76, 14, blockColor, 10);
        fillRoundedBlock(g2, innerX, currentY + 54, width - 56, 14, blockColor, 10);
    }

    private void paintTopBar(Graphics2D g2, int x, int y, int width, int height,
                             Color cardBg, Color borderColor, Color blockColor, Color accentColor) {
        fillRoundedBlock(g2, x, y, width, height, cardBg, 20);
        g2.setColor(borderColor);
        g2.drawRoundRect(x, y, width, height, 20, 20);

        int innerX = x + 18;
        int centerY = y + (height - 32) / 2;
        fillRoundedBlock(g2, innerX, centerY, 110, 32, accentColor, 14);
        fillRoundedBlock(g2, innerX + 126, centerY, 144, 32, blockColor, 14);

        int rightGroupW = 70;
        int rightX = x + width - 18 - rightGroupW;
        for (int i = 0; i < 3; i++) {
            fillRoundedBlock(g2, rightX - i * 84, centerY, rightGroupW, 32, blockColor, 14);
        }
    }

    private void paintMainEditor(Graphics2D g2, int x, int y, int width, int height,
                                 Color cardBg, Color borderColor, Color blockColor, Color softBlockColor, Color accentColor) {
        fillRoundedBlock(g2, x, y, width, height, cardBg, 20);
        g2.setColor(borderColor);
        g2.drawRoundRect(x, y, width, height, 20, 20);

        int innerX = x + 18;
        int innerY = y + 18;
        fillRoundedBlock(g2, innerX, innerY, 180, 16, accentColor, 10);
        fillRoundedBlock(g2, innerX + 194, innerY, 110, 16, blockColor, 10);

        int toolbarY = innerY + 30;
        int[] toolbarWidths = {86, 96, 72, 108};
        int toolbarX = innerX;
        for (int toolbarWidth : toolbarWidths) {
            fillRoundedBlock(g2, toolbarX, toolbarY, toolbarWidth, 28, blockColor, 12);
            toolbarX += toolbarWidth + 10;
        }

        int bodyY = toolbarY + 44;
        int leftColW = Math.max(220, (width - 54) / 2);
        int rightColX = innerX + leftColW + 18;
        int lineW = leftColW - 10;
        for (int i = 0; i < 7; i++) {
            fillRoundedBlock(g2, innerX, bodyY + i * 22, lineW - (i % 3) * 34, 14, softBlockColor, 10);
        }

        int previewW = width - (rightColX - x) - 18;
        fillRoundedBlock(g2, rightColX, bodyY, previewW, Math.max(120, height - 106), softBlockColor, 16);
        fillRoundedBlock(g2, rightColX + 16, bodyY + 18, Math.max(160, previewW - 120), 16, blockColor, 10);
        fillRoundedBlock(g2, rightColX + 16, bodyY + 48, Math.max(220, previewW - 64), 14, blockColor, 10);
        fillRoundedBlock(g2, rightColX + 16, bodyY + 74, Math.max(180, previewW - 92), 14, blockColor, 10);
    }

    private void paintBottomArea(Graphics2D g2, int x, int y, int width, int height,
                                 Color cardBg, Color borderColor, Color blockColor, Color softBlockColor, Color accentColor) {
        int gap = 16;
        int cardW = (width - gap) / 2;
        paintBottomCard(g2, x, y, cardW, height, cardBg, borderColor, blockColor, softBlockColor, accentColor);
        paintBottomCard(g2, x + cardW + gap, y, width - cardW - gap, height, cardBg, borderColor, blockColor, softBlockColor, accentColor);
    }

    private void paintBottomCard(Graphics2D g2, int x, int y, int width, int height,
                                 Color cardBg, Color borderColor, Color blockColor, Color softBlockColor, Color accentColor) {
        fillRoundedBlock(g2, x, y, width, height, cardBg, 18);
        g2.setColor(borderColor);
        g2.drawRoundRect(x, y, width, height, 18, 18);

        int innerX = x + 18;
        int currentY = y + 18;
        fillRoundedBlock(g2, innerX, currentY, Math.min(160, width - 36), 16, accentColor, 10);
        currentY += 32;

        for (int i = 0; i < 4; i++) {
            fillRoundedBlock(g2, innerX, currentY, Math.max(160, width - 36 - i * 24), 14, softBlockColor, 10);
            currentY += 22;
        }

        int chipY = y + height - 46;
        int chipX = innerX;
        int[] chipWidths = {86, 74, 102};
        for (int chipWidth : chipWidths) {
            if (chipX + chipWidth > x + width - 18) {
                break;
            }
            fillRoundedBlock(g2, chipX, chipY, chipWidth, 28, blockColor, 12);
            chipX += chipWidth + 10;
        }
    }
}
