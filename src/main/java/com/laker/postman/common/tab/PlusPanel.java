package com.laker.postman.common.tab;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PlusPanel extends JPanel {
    private static final Color ICON_COLOR = new Color(100, 100, 100);
    private static final Color BG_GRADIENT_START = new Color(245, 247, 250);
    private static final Color BG_GRADIENT_END = new Color(225, 230, 240);
    private static final int CORNER_RADIUS = 18;
    private JLabel plusIcon;
    private JPanel contentPanel;

    public PlusPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        // 创建主要内容面板，使用垂直居中的布局
        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BG_GRADIENT_START, 0, getHeight(), BG_GRADIENT_END);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 添加加号图标
        plusIcon = new JLabel();
        plusIcon.setIcon(IconFontSwing.buildIcon(FontAwesome.PLUS_CIRCLE, 40, ICON_COLOR));
        plusIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        plusIcon.setHorizontalAlignment(SwingConstants.CENTER);
        plusIcon.setForeground(ICON_COLOR);
        plusIcon.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPanel.add(plusIcon);

        // 添加垂直间距
        contentPanel.add(Box.createVerticalStrut(16));

        // 添加主标题
        JLabel createRequestLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CREATE_NEW_REQUEST));
        createRequestLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        createRequestLabel.setHorizontalAlignment(SwingConstants.CENTER);
        createRequestLabel.setForeground(new Color(60, 60, 60));
        createRequestLabel.setFont(createRequestLabel.getFont().deriveFont(Font.BOLD, 18f));
        contentPanel.add(createRequestLabel);

        // 添加垂直间距
        contentPanel.add(Box.createVerticalStrut(8));

        // 添加提示文本（国际化）
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUS_PANEL_HINT));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hintLabel.setForeground(new Color(120, 120, 120));
        hintLabel.setFont(hintLabel.getFont().deriveFont(13f));
        contentPanel.add(hintLabel);

        // 添加快捷键提示
        JLabel shortcutLabel = new JLabel("⌘N");
        shortcutLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        shortcutLabel.setHorizontalAlignment(SwingConstants.CENTER);
        shortcutLabel.setForeground(new Color(180, 180, 180));
        shortcutLabel.setFont(shortcutLabel.getFont().deriveFont(Font.BOLD, 12f));
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(shortcutLabel);

        // 添加快捷键描述
        JLabel shortcutDescLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SAVE_SHORTCUT_DESC));
        shortcutDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        shortcutDescLabel.setFont(shortcutDescLabel.getFont().deriveFont(Font.PLAIN, 14f));
        shortcutDescLabel.setForeground(new Color(120, 120, 120));
        contentPanel.add(Box.createVerticalStrut(16));
        contentPanel.add(shortcutDescLabel);

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 将内容面板添加到中心位置
        add(contentPanel, BorderLayout.CENTER);

        // 点击事件
        MouseAdapter clickAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    SingletonFactory.getInstance(RequestEditPanel.class).addNewTab(I18nUtil.getMessage(MessageKeys.NEW_REQUEST));
                }
            }
        };
        addMouseListener(clickAdapter);
        contentPanel.addMouseListener(clickAdapter);
        plusIcon.addMouseListener(clickAdapter);
    }
}