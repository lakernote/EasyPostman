package com.laker.postman.common.component.tab;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.service.setting.ShortcutManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代化设计的 PlusPanel，采用流行的设计语言和视觉效果
 * - 简洁优雅的卡片设计
 * - 柔和阴影效果
 * - 流畅的交互反馈
 * - 清晰的视觉层次
 * - 优化的性能表现
 * - 完全支持亮色和暗色主题自适应
 */
public class PlusPanel extends JPanel {
    // 圆角和间距
    private static final int CORNER_RADIUS = 16;
    private static final int CARD_PADDING = 48;
    private static final int ICON_SIZE = 96;

    /**
     * 获取主题适配的阴影颜色
     */
    private Color getShadowColor(int alpha) {
        return ModernColors.getShadowColor(alpha);
    }

    /**
     * 获取主题适配的卡片背景色
     */
    private Color getCardBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    /**
     * 获取主题适配的边框颜色
     */
    private Color getBorderColor() {
        return ModernColors.getBorderLightColor();
    }

    /**
     * 获取主题适配的图标背景颜色（带透明度）
     */
    private Color getIconBackgroundColor(int alpha) {
        return ModernColors.primaryWithAlpha(alpha);
    }

    /**
     * 获取主题适配的非悬停按钮背景色
     */
    private Color getButtonBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    /**
     * 获取主题适配的悬停文字颜色
     */
    private Color getHoverTextColor() {
        // 悬停时统一使用白色文字
        return Color.WHITE;
    }


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

                // 绘制优化的阴影效果（减少层数提升性能）
                drawOptimizedShadow(g2, width, height);

                // 绘制卡片背景（主题适配）
                g2.setColor(getCardBackground());
                g2.fillRoundRect(0, 0, width, height, CORNER_RADIUS, CORNER_RADIUS);

                // 绘制顶部装饰条 - 更细更优雅
                Shape oldClip = g2.getClip();
                g2.setClip(0, 0, width, 3);

                // 使用现代渐变色
                GradientPaint topStrip = new GradientPaint(
                        0, 0, ModernColors.PRIMARY,
                        width, 0, ModernColors.ACCENT
                );
                g2.setPaint(topStrip);
                g2.fillRoundRect(0, 0, width, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS);

                g2.setClip(oldClip);

                // 绘制精致的边框（主题适配）
                g2.setColor(getBorderColor());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, width - 1, height - 1, CORNER_RADIUS, CORNER_RADIUS);

                g2.dispose();
                super.paintComponent(g);
            }

            private void drawOptimizedShadow(Graphics2D g2, int width, int height) {
                // 优化的三层阴影（使用主题适配的颜色）
                int[] offsets = {6, 4, 2};
                int[] alphas = {8, 12, 18};

                for (int i = 0; i < offsets.length; i++) {
                    int offset = offsets[i];
                    g2.setColor(getShadowColor(alphas[i]));
                    g2.fillRoundRect(offset, offset, width - offset, height - offset,
                            CORNER_RADIUS + offset / 2, CORNER_RADIUS + offset / 2);
                }
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING));


        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 创建图标容器，简洁优雅的圆形背景
        JPanel iconContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = ICON_SIZE + 32;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // 多层发光效果
                // 外层大光晕
                g2.setColor(getIconBackgroundColor(12));
                g2.fillOval(x - 8, y - 8, size + 16, size + 16);

                // 中层光晕
                g2.setColor(getIconBackgroundColor(20));
                g2.fillOval(x - 4, y - 4, size + 8, size + 8);

                // 内层背景
                g2.setColor(getIconBackgroundColor(30));
                g2.fillOval(x, y, size, size);

                // 精致边框
                g2.setColor(getIconBackgroundColor(60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconContainer.setOpaque(false);
        iconContainer.setLayout(new BorderLayout());
        iconContainer.setPreferredSize(new Dimension(ICON_SIZE + 48, ICON_SIZE + 48));

        JLabel plusIcon = new JLabel();
        Image scaledImage = Icons.LOGO.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        ImageIcon logoIcon = new ImageIcon(scaledImage);
        plusIcon.setIcon(logoIcon);
        plusIcon.setHorizontalAlignment(SwingConstants.CENTER);
        iconContainer.add(plusIcon, BorderLayout.CENTER);

        iconContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(iconContainer);

        // 图标与主标题间距
        contentPanel.add(Box.createVerticalStrut(32));

        // 主标题 - 更大更醒目（主题适配文字颜色）
        JLabel createRequestLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CREATE_NEW_REQUEST));
        createRequestLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        createRequestLabel.setHorizontalAlignment(SwingConstants.CENTER);
        createRequestLabel.setForeground(ModernColors.getTextPrimary());
        createRequestLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 13));
        contentPanel.add(createRequestLabel);

        // 主标题与提示间距
        contentPanel.add(Box.createVerticalStrut(16));

        // 提示文本（国际化）- 现代按钮样式
        class HoverableLabel extends JLabel {
            @java.io.Serial
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;
            private transient GradientPaint cachedGradient;
            private transient GradientPaint cachedHighlight;
            private int lastWidth = -1;

            public HoverableLabel(String text) {
                super(text);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                if (isHovered) {
                    // 缓存渐变对象，仅在宽度变化时重新创建
                    if (cachedGradient == null || lastWidth != width) {
                        cachedGradient = new GradientPaint(
                                0, 0, ModernColors.PRIMARY,
                                width, 0, ModernColors.ACCENT
                        );
                        cachedHighlight = new GradientPaint(
                                0, 0, ModernColors.whiteWithAlpha(30),
                                0, height / 2.5f, ModernColors.whiteWithAlpha(0)
                        );
                        lastWidth = width;
                    }

                    // 绘制现代渐变背景
                    g2.setPaint(cachedGradient);
                    g2.fillRoundRect(0, 0, width, height, 24, 24);

                    // 添加微妙高光
                    g2.setPaint(cachedHighlight);
                    g2.fillRoundRect(0, 0, width, height / 2, 24, 24);
                } else {
                    // 非悬停状态：浅色背景（主题适配）
                    g2.setColor(getButtonBackground());
                    g2.fillRoundRect(0, 0, width, height, 24, 24);

                    // 添加边框（主题适配）
                    g2.setColor(getBorderColor());
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, width - 1, height - 1, 24, 24);
                }

                g2.dispose();
                super.paintComponent(g);
            }

            public void setHovered(boolean hovered) {
                if (this.isHovered != hovered) {
                    this.isHovered = hovered;
                    repaint();
                }
            }
        }

        HoverableLabel hintLabel = new HoverableLabel(I18nUtil.getMessage(MessageKeys.PLUS_PANEL_HINT));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hintLabel.setForeground(ModernColors.PRIMARY);
        hintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 2));
        hintLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(12, 32, 12, 32));
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
                hintLabel.setForeground(getHoverTextColor());
                hintLabel.setHovered(true);
                contentPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hintLabel.setForeground(ModernColors.PRIMARY);
                hintLabel.setHovered(false);
                contentPanel.setCursor(Cursor.getDefaultCursor());
            }
        };
        hintLabel.addMouseListener(adapter);
        hintLabel.setFocusable(true);
        contentPanel.add(hintLabel);

        // 快捷键信息分组显示，提升可读性
        contentPanel.add(Box.createVerticalStrut(32));
        JPanel shortcutPanel = new JPanel();
        shortcutPanel.setOpaque(false);
        shortcutPanel.setLayout(new BoxLayout(shortcutPanel, BoxLayout.Y_AXIS));

        // 动态显示快捷键，使用 ShortcutManager
        addShortcutLabel(shortcutPanel, I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_REQUEST),
                ShortcutManager.NEW_REQUEST);
        addShortcutLabel(shortcutPanel, I18nUtil.getMessage(MessageKeys.SAVE_REQUEST),
                ShortcutManager.SAVE_REQUEST);
        addShortcutLabel(shortcutPanel, I18nUtil.getMessage(MessageKeys.TAB_CLOSE_CURRENT),
                ShortcutManager.CLOSE_CURRENT_TAB);
        addShortcutLabel(shortcutPanel, I18nUtil.getMessage(MessageKeys.TAB_CLOSE_OTHERS),
                ShortcutManager.CLOSE_OTHER_TABS);
        addShortcutLabel(shortcutPanel, I18nUtil.getMessage(MessageKeys.TAB_CLOSE_ALL),
                ShortcutManager.CLOSE_ALL_TABS);
        addShortcutLabel(shortcutPanel, I18nUtil.getMessage(MessageKeys.EXIT_APP),
                ShortcutManager.EXIT_APP);

        contentPanel.add(shortcutPanel);

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 将内容面板添加到中心位置
        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * 添加快捷键标签的辅助方法
     */
    private void addShortcutLabel(JPanel panel, String actionName, String shortcutId) {
        String shortcutText = ShortcutManager.getShortcutText(shortcutId);

        // 使用国际化格式化标签文本
        String labelText = I18nUtil.getMessage(MessageKeys.SHORTCUT_LABEL_FORMAT, actionName, shortcutText);

        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        label.setForeground(ModernColors.getTextHint());
        panel.add(label);
        panel.add(Box.createVerticalStrut(8));
    }
}