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

    public PlusPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 设置手型光标

        // 创建主要内容面板，使用垂直居中的布局
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 添加加号图标
        JLabel plusIcon = new JLabel();
        plusIcon.setIcon(IconFontSwing.buildIcon(FontAwesome.PLUS_CIRCLE, 32, new Color(100, 100, 100)));
        plusIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        plusIcon.setHorizontalAlignment(SwingConstants.CENTER);
        plusIcon.setForeground(new Color(100, 100, 100));
        contentPanel.add(plusIcon);

        // 添加垂直间距
        contentPanel.add(Box.createVerticalStrut(10));

        // 添加主标题
        JLabel createRequestLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CREATE_NEW_REQUEST));
        createRequestLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        createRequestLabel.setHorizontalAlignment(SwingConstants.CENTER);
        createRequestLabel.setForeground(new Color(100, 100, 100));
        createRequestLabel.setFont(createRequestLabel.getFont().deriveFont(Font.BOLD, 14f));
        contentPanel.add(createRequestLabel);

        // 添加垂直间距
        contentPanel.add(Box.createVerticalStrut(5));

        // 添加提示文本
        JLabel hintLabel = new JLabel("Click here to create a new request");
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hintLabel.setForeground(new Color(150, 150, 150));
        hintLabel.setFont(hintLabel.getFont().deriveFont(11f));
        contentPanel.add(hintLabel);

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 将内容面板添加到中心位置
        add(contentPanel, BorderLayout.CENTER);

        // 添加鼠标悬停效果
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    SingletonFactory.getInstance(RequestEditPanel.class).addNewTab(I18nUtil.getMessage(MessageKeys.NEW_REQUEST));
                }
            }
        });
    }
}