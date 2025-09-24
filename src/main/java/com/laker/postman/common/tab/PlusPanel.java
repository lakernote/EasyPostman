package com.laker.postman.common.tab;

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
 * 优化后的 PlusPanel，内容和渐变效果更美观，注释更详细。
 */
public class PlusPanel extends JPanel {
    // 图标颜色
    private static final Color ICON_COLOR = new Color(100, 100, 100);
    // 渐变起始色
    private static final Color BG_GRADIENT_START = new Color(245, 247, 250);
    // 渐变结束色
    private static final Color BG_GRADIENT_END = new Color(225, 230, 240);
    // 圆角半径
    private static final int CORNER_RADIUS = 18;
    private JLabel plusIcon;
    private JPanel contentPanel;

    public PlusPanel() {
        setLayout(new BorderLayout());
        setOpaque(false); // 使背景透明，以显示自定义渐变
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 创建主要内容面板，使用垂直居中的布局
        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // 采用对角线渐变，提升美观度
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 对角线渐变（左上到右下）
                GradientPaint gp = new GradientPaint(0, 0, BG_GRADIENT_START, getWidth(), getHeight(), BG_GRADIENT_END);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
                // 添加轻微阴影，提升立体感
                g2.setColor(new Color(200, 200, 200, 60));
                g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, CORNER_RADIUS, CORNER_RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 图标部分
        plusIcon = new JLabel();
        // 调整图标大小
        ImageIcon logoIcon = new ImageIcon(Icons.LOGO.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
        plusIcon.setIcon(logoIcon);
        plusIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        plusIcon.setHorizontalAlignment(SwingConstants.CENTER);
        plusIcon.setForeground(ICON_COLOR);
        plusIcon.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPanel.add(plusIcon);

        // 图标与主标题间距
        contentPanel.add(Box.createVerticalStrut(20));

        // 主标题
        JLabel createRequestLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CREATE_NEW_REQUEST));
        createRequestLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        createRequestLabel.setHorizontalAlignment(SwingConstants.CENTER);
        createRequestLabel.setForeground(new Color(50, 50, 50));
        createRequestLabel.setFont(createRequestLabel.getFont().deriveFont(Font.BOLD, 20f));
        contentPanel.add(createRequestLabel);

        // 主标题与提示间距
        contentPanel.add(Box.createVerticalStrut(10));

        // 提示文本（国际化）
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUS_PANEL_HINT));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hintLabel.setForeground(new Color(120, 120, 120));
        hintLabel.setFont(hintLabel.getFont().deriveFont(14f));
        contentPanel.add(hintLabel);

        // 快捷键信息分组显示，提升可读性
        contentPanel.add(Box.createVerticalStrut(18));
        JPanel shortcutPanel = new JPanel();
        shortcutPanel.setOpaque(false);
        shortcutPanel.setLayout(new BoxLayout(shortcutPanel, BoxLayout.Y_AXIS));

        // 新建请求快捷键
        JLabel shortcutLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUS_PANEL_SHORTCUT));
        shortcutLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        shortcutLabel.setHorizontalAlignment(SwingConstants.CENTER);
        shortcutLabel.setForeground(new Color(100, 100, 200));
        shortcutLabel.setFont(shortcutLabel.getFont().deriveFont(Font.BOLD, 13f));
        shortcutPanel.add(shortcutLabel);
        JLabel shortcutDescLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUS_PANEL_SHORTCUT_DESC));
        shortcutDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        shortcutDescLabel.setFont(shortcutDescLabel.getFont().deriveFont(Font.PLAIN, 13f));
        shortcutDescLabel.setForeground(new Color(120, 120, 120));
        shortcutPanel.add(shortcutDescLabel);
        shortcutPanel.add(Box.createVerticalStrut(8));

        // 保存快捷键
        JLabel saveShortcutLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SAVE_SHORTCUT));
        saveShortcutLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveShortcutLabel.setHorizontalAlignment(SwingConstants.CENTER);
        saveShortcutLabel.setForeground(new Color(100, 180, 100));
        saveShortcutLabel.setFont(saveShortcutLabel.getFont().deriveFont(Font.BOLD, 13f));
        shortcutPanel.add(saveShortcutLabel);
        JLabel saveShortcutDescLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SAVE_SHORTCUT_DESC));
        saveShortcutDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveShortcutDescLabel.setFont(saveShortcutDescLabel.getFont().deriveFont(Font.PLAIN, 13f));
        saveShortcutDescLabel.setForeground(new Color(120, 120, 120));
        shortcutPanel.add(saveShortcutDescLabel);
        shortcutPanel.add(Box.createVerticalStrut(8));

        // 退出快捷键
        JLabel exitShortcutLabel = new JLabel(I18nUtil.getMessage(MessageKeys.EXIT_SHORTCUT));
        exitShortcutLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitShortcutLabel.setHorizontalAlignment(SwingConstants.CENTER);
        exitShortcutLabel.setForeground(new Color(180, 100, 100));
        exitShortcutLabel.setFont(exitShortcutLabel.getFont().deriveFont(Font.BOLD, 13f));
        shortcutPanel.add(exitShortcutLabel);
        JLabel exitShortcutDescLabel = new JLabel(I18nUtil.getMessage(MessageKeys.EXIT_SHORTCUT_DESC));
        exitShortcutDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitShortcutDescLabel.setFont(exitShortcutDescLabel.getFont().deriveFont(Font.PLAIN, 13f));
        exitShortcutDescLabel.setForeground(new Color(120, 120, 120));
        shortcutPanel.add(exitShortcutDescLabel);

        contentPanel.add(shortcutPanel);

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 将内容面板添加到中心位置
        add(contentPanel, BorderLayout.CENTER);

        // 点击事件，点击任意区域都可新建请求
        MouseAdapter clickAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    SingletonFactory.getInstance(RequestEditPanel.class).addNewTab(I18nUtil.getMessage(MessageKeys.NEW_REQUEST));
                }
            }
        };
        // 递归为所有子组件添加监听器
        addMouseListenerRecursively(this, clickAdapter);
    }

    /**
     * 递归为所有子组件添加 MouseListener
     */
    private void addMouseListenerRecursively(Component comp, MouseAdapter adapter) {
        comp.addMouseListener(adapter);
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                addMouseListenerRecursively(child, adapter);
            }
        }
    }
}