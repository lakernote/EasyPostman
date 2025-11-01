package com.laker.postman.common.component.tab;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代化设计的 PlusPanel，采用流行的设计语言和视觉效果
 * - 现代渐变背景
 * - 柔和阴影效果
 * - 优雅的动画交互
 * - 清晰的视觉层次
 */
public class PlusPanel extends JPanel {
    // 圆角和间距
    private static final int CORNER_RADIUS = 24;
    private static final int CARD_PADDING = 40;
    private static final int ICON_SIZE = 120;


    public PlusPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 创建现代化的卡片式内容面板
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int width = getWidth();
                int height = getHeight();

                // 绘制多层深度阴影
                drawEnhancedShadow(g2, width, height);

                // 绘制卡片背景
                g2.setColor(ModernColors.BG_WHITE);
                g2.fillRoundRect(0, 0, width, height, CORNER_RADIUS, CORNER_RADIUS);

                // 绘制顶部装饰条 - 使用裁剪确保完美贴合
                Shape oldClip = g2.getClip();
                // 只裁剪顶部区域
                g2.setClip(0, 0, width, 4);

                // 绘制渐变装饰（静态，无动画）
                GradientPaint topStrip = new GradientPaint(
                        0, 0, ModernColors.PRIMARY,
                        width, 0, ModernColors.SECONDARY
                );
                g2.setPaint(topStrip);
                g2.fillRoundRect(0, 0, width, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS);

                // 恢复裁剪区域
                g2.setClip(oldClip);

                // 绘制微妙的边框
                g2.setColor(new Color(ModernColors.BORDER_LIGHT.getRed(),
                        ModernColors.BORDER_LIGHT.getGreen(),
                        ModernColors.BORDER_LIGHT.getBlue(), 150));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, width - 2, height - 2, CORNER_RADIUS, CORNER_RADIUS);

                // 添加内发光效果
                g2.setColor(ModernColors.whiteWithAlpha(40));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(2, 2, width - 4, height - 4, CORNER_RADIUS - 2, CORNER_RADIUS - 2);

                g2.dispose();
                super.paintComponent(g);
            }

            private void drawEnhancedShadow(Graphics2D g2, int width, int height) {
                // 创建更深层次的阴影效果
                int shadowLayers = 5;
                for (int i = shadowLayers; i > 0; i--) {
                    int alpha = 25 - i * 3;
                    int offset = i * 2;
                    g2.setColor(new Color(ModernColors.TEXT_PRIMARY.getRed(),
                            ModernColors.TEXT_PRIMARY.getGreen(),
                            ModernColors.TEXT_PRIMARY.getBlue(), alpha));
                    g2.fillRoundRect(offset, offset, width - offset, height - offset,
                            CORNER_RADIUS + i, CORNER_RADIUS + i);
                }
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING));


        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 创建图标容器，简洁的圆形背景（无光晕）
        JPanel iconContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = ICON_SIZE + 20;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // 绘制简洁的圆形背景（浅色）
                g2.setColor(new Color(ModernColors.PRIMARY.getRed(),
                        ModernColors.PRIMARY.getGreen(),
                        ModernColors.PRIMARY.getBlue(), 15)); // 极淡的紫色
                g2.fillOval(x, y, size, size);

                // 绘制细边框（可选，增加层次感）
                g2.setColor(new Color(ModernColors.PRIMARY.getRed(),
                        ModernColors.PRIMARY.getGreen(),
                        ModernColors.PRIMARY.getBlue(), 30));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconContainer.setOpaque(false);
        iconContainer.setLayout(new BorderLayout());
        iconContainer.setPreferredSize(new Dimension(ICON_SIZE + 40, ICON_SIZE + 40));

        JLabel plusIcon = new JLabel();
        ImageIcon logoIcon = new ImageIcon(Icons.LOGO.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
        plusIcon.setIcon(logoIcon);
        plusIcon.setHorizontalAlignment(SwingConstants.CENTER);
        iconContainer.add(plusIcon, BorderLayout.CENTER);

        iconContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(iconContainer);

        // 图标与主标题间距
        contentPanel.add(Box.createVerticalStrut(24));

        // 主标题
        JLabel createRequestLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CREATE_NEW_REQUEST));
        createRequestLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        createRequestLabel.setHorizontalAlignment(SwingConstants.CENTER);
        createRequestLabel.setForeground(ModernColors.TEXT_PRIMARY);
        createRequestLabel.setFont(createRequestLabel.getFont().deriveFont(Font.BOLD, 24f));
        contentPanel.add(createRequestLabel);

        // 主标题与提示间距
        contentPanel.add(Box.createVerticalStrut(12));

        // 提示文本（国际化）- 设计为按钮样式
        class HoverableLabel extends JLabel {
            private boolean isHovered = false;

            public HoverableLabel(String text) {
                super(text);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isHovered) {
                    // 绘制悬停背景
                    GradientPaint gradient = new GradientPaint(
                            0, 0, ModernColors.PRIMARY,
                            getWidth(), 0, ModernColors.SECONDARY
                    );
                    g2.setPaint(gradient);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                    // 添加高光
                    GradientPaint highlight = new GradientPaint(
                            0, 0, ModernColors.whiteWithAlpha(40),
                            0, getHeight() / 2f, ModernColors.whiteWithAlpha(0)
                    );
                    g2.setPaint(highlight);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 20, 20);
                }

                g2.dispose();
                super.paintComponent(g);
            }

            public void setHovered(boolean hovered) {
                this.isHovered = hovered;
                repaint();
            }
        }

        HoverableLabel hintLabel = new HoverableLabel(I18nUtil.getMessage(MessageKeys.PLUS_PANEL_HINT));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hintLabel.setForeground(ModernColors.TEXT_HINT);
        hintLabel.setFont(hintLabel.getFont().deriveFont(15f));
        hintLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        hintLabel.setOpaque(false);

        // 点击触发和悬停效果
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    SingletonFactory.getInstance(RequestEditPanel.class).addNewTab(I18nUtil.getMessage(MessageKeys.NEW_REQUEST));
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hintLabel.setForeground(ModernColors.TEXT_INVERSE);
                hintLabel.setFont(hintLabel.getFont().deriveFont(Font.BOLD));
                hintLabel.setHovered(true);
                contentPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hintLabel.setForeground(ModernColors.TEXT_HINT);
                hintLabel.setFont(hintLabel.getFont().deriveFont(Font.PLAIN));
                hintLabel.setHovered(false);
                contentPanel.setCursor(Cursor.getDefaultCursor());
            }
        };
        hintLabel.addMouseListener(adapter);
        hintLabel.setFocusable(true);
        contentPanel.add(hintLabel);

        // 快捷键信息分组显示，提升可读性
        contentPanel.add(Box.createVerticalStrut(24));
        JPanel shortcutPanel = new JPanel();
        shortcutPanel.setOpaque(false);
        shortcutPanel.setLayout(new BoxLayout(shortcutPanel, BoxLayout.Y_AXIS));

        // 新建请求快捷键描述
        JLabel shortcutDescLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUS_PANEL_SHORTCUT_DESC));
        shortcutDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        shortcutDescLabel.setFont(shortcutDescLabel.getFont().deriveFont(Font.PLAIN, 13f));
        shortcutDescLabel.setForeground(ModernColors.TEXT_HINT);
        shortcutPanel.add(shortcutDescLabel);
        shortcutPanel.add(Box.createVerticalStrut(8));

        // 保存快捷键描述
        JLabel saveShortcutDescLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SAVE_SHORTCUT_DESC));
        saveShortcutDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveShortcutDescLabel.setFont(saveShortcutDescLabel.getFont().deriveFont(Font.PLAIN, 13f));
        saveShortcutDescLabel.setForeground(ModernColors.TEXT_HINT);
        shortcutPanel.add(saveShortcutDescLabel);
        shortcutPanel.add(Box.createVerticalStrut(8));

        // 退出快捷键描述
        JLabel exitShortcutDescLabel = new JLabel(I18nUtil.getMessage(MessageKeys.EXIT_SHORTCUT_DESC));
        exitShortcutDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitShortcutDescLabel.setFont(exitShortcutDescLabel.getFont().deriveFont(Font.PLAIN, 13f));
        exitShortcutDescLabel.setForeground(ModernColors.TEXT_HINT);
        shortcutPanel.add(exitShortcutDescLabel);

        contentPanel.add(shortcutPanel);

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 将内容面板添加到中心位置
        add(contentPanel, BorderLayout.CENTER);
    }
}