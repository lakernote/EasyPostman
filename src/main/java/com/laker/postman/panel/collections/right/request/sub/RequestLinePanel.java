package com.laker.postman.panel.collections.right.request.sub;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.EasyPostmanTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 顶部请求行面板 - 现代化设计
 * 包含：方法选择、URL输入、发送/保存按钮
 * 设计理念：简洁、专业、易用
 */
@Getter
public class RequestLinePanel extends JPanel {
    // 尺寸常量
    private static final int ICON_SIZE = 14;
    private static final int COMPONENT_HEIGHT = 28;
    private static final int METHOD_COMBO_WIDTH = 85;
    private static final int PANEL_PADDING = 4;

    // 组件
    private final JComboBox<String> methodBox;
    private final JTextField urlField;
    private final JButton sendButton;
    private final JButton saveButton;
    private final RequestItemProtocolEnum protocol;

    public RequestLinePanel(ActionListener sendAction, RequestItemProtocolEnum protocol) {
        this.protocol = protocol;

        // 初始化组件
        methodBox = createMethodComboBox();
        urlField = createUrlField();
        sendButton = createSendButton(sendAction);
        saveButton = createSaveButton();


        // 设置面板样式
        setupPanelStyle();

        // 构建布局
        buildLayout();
    }

    /**
     * 设置面板整体样式
     */
    private void setupPanelStyle() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(true);
        // 添加精致的边框和内边距
        setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.BORDER_LIGHT),
                new EmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING)
        ));
    }

    /**
     * 创建方法选择下拉框
     */
    private JComboBox<String> createMethodComboBox() {
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE"};
        JComboBox<String> combo = new JComboBox<>(methods);

        // 设置尺寸
        Dimension size = new Dimension(METHOD_COMBO_WIDTH, COMPONENT_HEIGHT);
        combo.setPreferredSize(size);
        combo.setMaximumSize(size);
        combo.setMinimumSize(size);

        // 设置字体
        combo.setFont(combo.getFont().deriveFont(Font.BOLD, 12f));

        // WebSocket 协议特殊处理
        if (protocol.isWebSocketProtocol()) {
            combo.setVisible(false);
            combo.setSelectedItem("GET");
        }

        return combo;
    }

    /**
     * 创建 URL 输入框
     */
    private JTextField createUrlField() {
        JTextField field = new EasyPostmanTextField(
                null,
                30,
                I18nUtil.getMessage(MessageKeys.REQUEST_URL_PLACEHOLDER)
        );

        // 设置尺寸
        field.setPreferredSize(new Dimension(500, COMPONENT_HEIGHT));
        field.setMinimumSize(new Dimension(300, COMPONENT_HEIGHT));

        // 设置字体
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 13f));

        return field;
    }

    /**
     * 创建发送/连接按钮
     */
    private JButton createSendButton(ActionListener sendAction) {
        String text = protocol.isWebSocketProtocol() ?
                I18nUtil.getMessage(MessageKeys.BUTTON_CONNECT) :
                I18nUtil.getMessage(MessageKeys.BUTTON_SEND);
        String iconPath = protocol.isWebSocketProtocol() ?
                "icons/connect-white.svg" : "icons/send-white.svg";

        JButton button = createPrimaryButton(text, iconPath);
        button.addActionListener(sendAction);

        return button;
    }

    /**
     * 创建保存按钮
     */
    private JButton createSaveButton() {
        JButton button = createSecondaryButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_SAVE),
                "icons/save.svg"
        );
        button.setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE_TOOLTIP));
        button.addActionListener(e ->
                SingletonFactory.getInstance(RequestEditPanel.class).saveCurrentRequest()
        );

        return button;
    }

    /**
     * 创建主按钮（发送/连接）
     */
    private JButton createPrimaryButton(String text, String iconPath) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 从 ClientProperty 读取颜色，如果没有则使用默认蓝色
                Color baseColor = (Color) getClientProperty("baseColor");
                Color hoverColor = (Color) getClientProperty("hoverColor");
                Color pressColor = (Color) getClientProperty("pressColor");

                if (baseColor == null) baseColor = ModernColors.PRIMARY;
                if (hoverColor == null) hoverColor = ModernColors.PRIMARY_DARK;
                if (pressColor == null) pressColor = ModernColors.PRIMARY_DARKER;

                // 背景颜色
                if (!isEnabled()) {
                    g2.setColor(ModernColors.TEXT_DISABLED);
                } else if (getModel().isPressed()) {
                    g2.setColor(pressColor);
                } else if (getModel().isRollover()) {
                    g2.setColor(hoverColor);
                } else {
                    g2.setColor(baseColor);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();

                // 文字和图标
                super.paintComponent(g);
            }
        };

        // 设置图标
        button.setIcon(new FlatSVGIcon(iconPath, ICON_SIZE, ICON_SIZE));
        button.setIconTextGap(4);

        // 设置字体和样式
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setForeground(ModernColors.TEXT_INVERSE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(5, 12, 5, 12));

        // 悬停动画
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.repaint();
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.repaint();
            }
        });

        return button;
    }

    /**
     * 创建次要按钮（保存）
     */
    private JButton createSecondaryButton(String text, String iconPath) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 背景颜色
                if (!isEnabled()) {
                    g2.setColor(ModernColors.BG_LIGHT);
                } else if (getModel().isPressed()) {
                    g2.setColor(ModernColors.BG_DARK);
                } else if (getModel().isRollover()) {
                    g2.setColor(ModernColors.HOVER_BG);
                } else {
                    g2.setColor(ModernColors.BG_WHITE);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // 边框
                g2.setColor(isEnabled() ? ModernColors.BORDER_MEDIUM : ModernColors.BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                g2.dispose();

                // 文字和图标
                super.paintComponent(g);
            }
        };

        // 设置图标
        button.setIcon(new FlatSVGIcon(iconPath, ICON_SIZE, ICON_SIZE));
        button.setIconTextGap(4);

        // 设置字体和样式
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 12f));
        button.setForeground(ModernColors.TEXT_PRIMARY);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(5, 12, 5, 12));

        // 悬停动画
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.repaint();
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.repaint();
            }
        });

        return button;
    }

    /**
     * 构建面板布局
     */
    private void buildLayout() {
        // 左侧：方法选择 + URL
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);

        if (methodBox.isVisible()) {
            leftPanel.add(methodBox);
            leftPanel.add(Box.createHorizontalStrut(6));
        }
        leftPanel.add(urlField);
        leftPanel.add(Box.createHorizontalStrut(10)); // URL输入框与按钮组之间的间距

        // 右侧：按钮组
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);

        rightPanel.add(sendButton);
        rightPanel.add(Box.createHorizontalStrut(6));
        rightPanel.add(saveButton);

        // 添加到主面板
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }


    /**
     * 动态更新按钮样式（颜色）
     */
    private void updateButtonStyle(JButton button, Color baseColor, Color hoverColor, Color pressColor) {
        // 移除旧的鼠标监听器
        for (java.awt.event.MouseListener ml : button.getMouseListeners()) {
            if (ml instanceof java.awt.event.MouseAdapter) {
                button.removeMouseListener(ml);
            }
        }

        // 重新设置按钮颜色
        button.putClientProperty("baseColor", baseColor);
        button.putClientProperty("hoverColor", hoverColor);
        button.putClientProperty("pressColor", pressColor);

        // 添加新的鼠标监听器
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.repaint();
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.repaint();
            }
        });

        // 强制刷新
        button.repaint();
    }

    /**
     * 切换按钮为 Send 状态
     */
    public void setSendButtonToSend(ActionListener sendAction) {
        // 移除旧监听器
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }

        // 设置文本和图标
        if (protocol.isWebSocketProtocol()) {
            sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CONNECT));
            sendButton.setIcon(new FlatSVGIcon("icons/connect-white.svg", ICON_SIZE, ICON_SIZE));
        } else {
            sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_SEND));
            sendButton.setIcon(new FlatSVGIcon("icons/send-white.svg", ICON_SIZE, ICON_SIZE));
        }

        sendButton.setEnabled(true);

        // 重置为默认蓝色
        updateButtonStyle(sendButton, ModernColors.PRIMARY, ModernColors.PRIMARY_DARK,
                ModernColors.PRIMARY_DARKER);

        // 添加新监听器
        sendButton.addActionListener(sendAction);
    }

    /**
     * 切换按钮为 Cancel 状态
     */
    public void setSendButtonToCancel(ActionListener cancelAction) {
        // 移除旧监听器
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }

        // 设置为取消按钮样式
        sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        sendButton.setIcon(new FlatSVGIcon("icons/cancel-white.svg", ICON_SIZE, ICON_SIZE));
        sendButton.setEnabled(true);

        // 改变按钮为警告色（橙色）
        updateButtonStyle(sendButton, ModernColors.WARNING, ModernColors.WARNING_DARK,
                ModernColors.WARNING_DARKER);

        // 添加新监听器
        sendButton.addActionListener(cancelAction);
    }

    /**
     * 切换按钮为 Close 状态
     */
    public void setSendButtonToClose(ActionListener closeAction) {
        // 移除旧监听器
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }

        // 设置为关闭按钮样式
        sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE));
        sendButton.setIcon(new FlatSVGIcon("icons/close-white.svg", ICON_SIZE, ICON_SIZE));
        sendButton.setEnabled(true);

        // 改变按钮为中性灰色
        updateButtonStyle(sendButton, ModernColors.NEUTRAL, ModernColors.NEUTRAL_DARK,
                ModernColors.NEUTRAL_DARKER);

        // 添加新监听器
        sendButton.addActionListener(closeAction);
    }
}