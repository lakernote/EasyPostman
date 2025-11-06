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
 * - 简洁优雅的卡片设计
 * - 柔和阴影效果
 * - 流畅的交互反馈
 * - 清晰的视觉层次
 * - 优化的性能表现
 */
public class PlusPanel extends JPanel {
    // 圆角和间距
    private static final int CORNER_RADIUS = 16;
    private static final int CARD_PADDING = 48;
    private static final int ICON_SIZE = 96;

    // 缓存颜色对象，避免重复创建（性能优化）
    private static final Color SHADOW_COLOR_1 = new Color(15, 23, 42, 8);
    private static final Color SHADOW_COLOR_2 = new Color(15, 23, 42, 12);
    private static final Color SHADOW_COLOR_3 = new Color(15, 23, 42, 18);
    private static final Color ICON_BG_GLOW = new Color(
            ModernColors.PRIMARY.getRed(),
            ModernColors.PRIMARY.getGreen(),
            ModernColors.PRIMARY.getBlue(), 8);
    private static final Color ICON_BG_MAIN = new Color(
            ModernColors.PRIMARY.getRed(),
            ModernColors.PRIMARY.getGreen(),
            ModernColors.PRIMARY.getBlue(), 20);
    private static final Color ICON_BG_BORDER = new Color(
            ModernColors.PRIMARY.getRed(),
            ModernColors.PRIMARY.getGreen(),
            ModernColors.PRIMARY.getBlue(), 60);


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

                // 绘制卡片背景
                g2.setColor(ModernColors.BG_WHITE);
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

                // 绘制精致的边框
                g2.setColor(ModernColors.BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, width - 1, height - 1, CORNER_RADIUS, CORNER_RADIUS);

                g2.dispose();
                super.paintComponent(g);
            }

            private void drawOptimizedShadow(Graphics2D g2, int width, int height) {
                // 优化的三层阴影（使用缓存的颜色对象）
                int[] offsets = {6, 4, 2};
                Color[] colors = {SHADOW_COLOR_1, SHADOW_COLOR_2, SHADOW_COLOR_3};

                for (int i = 0; i < offsets.length; i++) {
                    int offset = offsets[i];
                    g2.setColor(colors[i]);
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

                // 绘制外层微妙光晕（使用缓存颜色）
                g2.setColor(ICON_BG_GLOW);
                g2.fillOval(x - 4, y - 4, size + 8, size + 8);

                // 绘制主圆形背景（使用缓存颜色）
                g2.setColor(ICON_BG_MAIN);
                g2.fillOval(x, y, size, size);

                // 绘制精致边框（使用缓存颜色）
                g2.setColor(ICON_BG_BORDER);
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
        // 使用 SCALE_FAST 提升性能
        Image scaledImage = Icons.LOGO.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_FAST);
        ImageIcon logoIcon = new ImageIcon(scaledImage);
        plusIcon.setIcon(logoIcon);
        plusIcon.setHorizontalAlignment(SwingConstants.CENTER);
        iconContainer.add(plusIcon, BorderLayout.CENTER);

        iconContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(iconContainer);

        // 图标与主标题间距
        contentPanel.add(Box.createVerticalStrut(32));

        // 主标题 - 更大更醒目
        JLabel createRequestLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CREATE_NEW_REQUEST));
        createRequestLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        createRequestLabel.setHorizontalAlignment(SwingConstants.CENTER);
        createRequestLabel.setForeground(ModernColors.TEXT_PRIMARY);
        createRequestLabel.setFont(createRequestLabel.getFont().deriveFont(Font.BOLD, 26f));
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
                    // 非悬停状态：浅色背景
                    g2.setColor(ModernColors.HOVER_BG);
                    g2.fillRoundRect(0, 0, width, height, 24, 24);

                    // 添加边框
                    g2.setColor(ModernColors.BORDER_LIGHT);
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
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.BOLD, 15f));
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
                hintLabel.setForeground(Color.WHITE);
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