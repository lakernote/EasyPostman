package com.laker.postman.common.component.tab;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.Icons;
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
    // 现代化配色方案
    private static final Color PRIMARY_COLOR = new Color(99, 102, 241); // Indigo-500
    private static final Color SECONDARY_COLOR = new Color(139, 92, 246); // Purple-500
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42); // Slate-900
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139); // Slate-500
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color SHADOW_COLOR = new Color(15, 23, 42, 20); // 柔和阴影

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

                // 绘制多层阴影，创建深度感
                drawLayeredShadow(g2, width, height);

                // 绘制卡片背景
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, width, height, CORNER_RADIUS, CORNER_RADIUS);

                // 绘制顶部渐变装饰条
                GradientPaint gradientStrip = new GradientPaint(
                        0, 0, PRIMARY_COLOR,
                        width, 0, SECONDARY_COLOR
                );
                g2.setPaint(gradientStrip);
                g2.fillRoundRect(0, 0, width, 6, CORNER_RADIUS, CORNER_RADIUS);

                // 绘制微妙的边框
                g2.setColor(new Color(226, 232, 240, 100)); // Slate-200 with opacity
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, width - 2, height - 2, CORNER_RADIUS, CORNER_RADIUS);

                g2.dispose();
                super.paintComponent(g);
            }

            private void drawLayeredShadow(Graphics2D g2, int width, int height) {
                // 外层阴影（较大、较淡）
                g2.setColor(new Color(15, 23, 42, 8));
                g2.fillRoundRect(4, 4, width - 8, height - 8, CORNER_RADIUS + 2, CORNER_RADIUS + 2);

                // 中层阴影
                g2.setColor(new Color(15, 23, 42, 12));
                g2.fillRoundRect(2, 2, width - 4, height - 4, CORNER_RADIUS + 1, CORNER_RADIUS + 1);

                // 内层阴影（较小、较深）
                g2.setColor(SHADOW_COLOR);
                g2.fillRoundRect(1, 1, width - 2, height - 2, CORNER_RADIUS, CORNER_RADIUS);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING));

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 创建图标容器，带有现代化的圆形背景
        JPanel iconContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = ICON_SIZE + 20;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // 绘制渐变圆形背景
                GradientPaint gradient = new GradientPaint(
                        (float) x, (float) y, new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 30),
                        (float) (x + size), (float) (y + size), new Color(SECONDARY_COLOR.getRed(), SECONDARY_COLOR.getGreen(), SECONDARY_COLOR.getBlue(), 30)
                );
                g2.setPaint(gradient);
                g2.fillOval(x, y, size, size);

                // 绘制外圈光晕
                g2.setColor(new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 15));
                g2.fillOval(x - 10, y - 10, size + 20, size + 20);

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
        createRequestLabel.setForeground(TEXT_PRIMARY);
        createRequestLabel.setFont(createRequestLabel.getFont().deriveFont(Font.BOLD, 24f));
        contentPanel.add(createRequestLabel);

        // 主标题与提示间距
        contentPanel.add(Box.createVerticalStrut(12));

        // 提示文本（国际化）
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUS_PANEL_HINT));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hintLabel.setForeground(TEXT_SECONDARY);
        hintLabel.setFont(hintLabel.getFont().deriveFont(15f));
        hintLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
                hintLabel.setForeground(PRIMARY_COLOR);
                hintLabel.setFont(hintLabel.getFont().deriveFont(Font.BOLD));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hintLabel.setForeground(TEXT_SECONDARY);
                hintLabel.setFont(hintLabel.getFont().deriveFont(Font.PLAIN));
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
        shortcutDescLabel.setForeground(TEXT_SECONDARY);
        shortcutPanel.add(shortcutDescLabel);
        shortcutPanel.add(Box.createVerticalStrut(8));

        // 保存快捷键描述
        JLabel saveShortcutDescLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SAVE_SHORTCUT_DESC));
        saveShortcutDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveShortcutDescLabel.setFont(saveShortcutDescLabel.getFont().deriveFont(Font.PLAIN, 13f));
        saveShortcutDescLabel.setForeground(TEXT_SECONDARY);
        shortcutPanel.add(saveShortcutDescLabel);
        shortcutPanel.add(Box.createVerticalStrut(8));

        // 退出快捷键描述
        JLabel exitShortcutDescLabel = new JLabel(I18nUtil.getMessage(MessageKeys.EXIT_SHORTCUT_DESC));
        exitShortcutDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitShortcutDescLabel.setFont(exitShortcutDescLabel.getFont().deriveFont(Font.PLAIN, 13f));
        exitShortcutDescLabel.setForeground(TEXT_SECONDARY);
        shortcutPanel.add(exitShortcutDescLabel);

        contentPanel.add(shortcutPanel);

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 将内容面板添加到中心位置
        add(contentPanel, BorderLayout.CENTER);
    }
}