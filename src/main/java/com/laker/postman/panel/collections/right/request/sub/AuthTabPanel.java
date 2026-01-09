package com.laker.postman.panel.collections.right.request.sub;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.component.EasyPostmanTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.AuthType;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证Tab面板 - 现代化版本
 * 使用专业的布局和现代化配色方案
 * 支持亮色和暗色主题自适应
 */
public class AuthTabPanel extends JPanel {
    // Keep constants for backward compatibility
    public static final String AUTH_TYPE_INHERIT = AuthType.INHERIT.getConstant();
    public static final String AUTH_TYPE_NONE = AuthType.NONE.getConstant();
    public static final String AUTH_TYPE_BASIC = AuthType.BASIC.getConstant();
    public static final String AUTH_TYPE_BEARER = AuthType.BEARER.getConstant();

    private final JComboBox<AuthType> typeCombo;
    private final JTextField usernameField;
    private final JTextField passwordField;
    private final JTextField tokenField;
    private AuthType currentType = AuthType.INHERIT;
    private final List<Runnable> dirtyListeners = new ArrayList<>();

    /**
     * 检查当前是否为暗色主题
     */
    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取主题适配的标题颜色（HTML 样式用）
     */
    private String getTitleColorHex() {
        return isDarkTheme() ? "#14b8a6" : "#0891b2"; // 暗色主题用稍亮的青色
    }

    /**
     * 获取主题适配的描述文字颜色（HTML 样式用）
     */
    private String getDescColorHex() {
        return isDarkTheme() ? "#94a3b8" : "#475569"; // 暗色主题用浅灰色
    }

    /**
     * 获取主题适配的普通文字颜色（HTML 样式用）
     */
    private String getTextColorHex() {
        return isDarkTheme() ? "#cbd5e1" : "#334155"; // 暗色主题用浅色
    }

    public AuthTabPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(8, 10, 8, 10));

        // 初始化所有字段
        typeCombo = new JComboBox<>(AuthType.values());
        typeCombo.setPreferredSize(new Dimension(220, 32));
        typeCombo.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));

        usernameField = new EasyPostmanTextField(20);
        usernameField.setPreferredSize(new Dimension(200, 32));

        passwordField = new EasyPostmanTextField(20);
        passwordField.setPreferredSize(new Dimension(200, 32));

        tokenField = new EasyPostmanTextField(30);
        tokenField.setPreferredSize(new Dimension(250, 32));

        // 顶部：认证类型选择
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        JLabel typeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.AUTH_TYPE_LABEL));
        typeLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        typeLabel.setForeground(ModernColors.getTextPrimary());
        topPanel.add(typeLabel);
        topPanel.add(typeCombo);
        add(topPanel, BorderLayout.NORTH);

        // 中间：各种认证类型的配置面板（使用 CardLayout）
        JPanel cardPanel = new JPanel(new CardLayout());

        // Inherit Auth Panel
        cardPanel.add(createInheritPanel(), AUTH_TYPE_INHERIT);

        // No Auth Panel
        cardPanel.add(createNoAuthPanel(), AUTH_TYPE_NONE);

        // Basic Auth Panel
        cardPanel.add(createBasicAuthPanel(), AUTH_TYPE_BASIC);

        // Bearer Token Panel
        cardPanel.add(createBearerPanel(), AUTH_TYPE_BEARER);

        add(cardPanel, BorderLayout.CENTER);

        // 监听认证类型切换
        typeCombo.addActionListener(e -> {
            currentType = (AuthType) typeCombo.getSelectedItem();
            CardLayout cl = (CardLayout) cardPanel.getLayout();
            cl.show(cardPanel, currentType.getConstant());
            fireDirty();
        });

        // 监听文本框内容变化
        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                fireDirty();
            }

            public void removeUpdate(DocumentEvent e) {
                fireDirty();
            }

            public void changedUpdate(DocumentEvent e) {
                fireDirty();
            }
        };
        usernameField.getDocument().addDocumentListener(docListener);
        passwordField.getDocument().addDocumentListener(docListener);
        tokenField.getDocument().addDocumentListener(docListener);

        // 默认显示继承面板
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, AUTH_TYPE_INHERIT);
    }

    /**
     * 创建 Inherit Auth 面板
     */
    private JPanel createInheritPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel infoPanel = new JPanel(new BorderLayout(10, 0));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.ACCENT_LIGHT, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel iconLabel = new JLabel("ℹ");
        iconLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN)); // 使用用户设置的字体大小;
        infoPanel.add(iconLabel, BorderLayout.WEST);

        JLabel textLabel = new JLabel(
                "<html><div style='line-height: 1.5;'>" +
                        "<b style='color: " + getTitleColorHex() + "; font-size: 10px;'>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_INHERIT) + "</b><br>" +
                        "<span style='color: " + getDescColorHex() + "; font-size: 9px;'>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_INHERIT_DESC) + "</span>" +
                        "</div></html>"
        );
        infoPanel.add(textLabel, BorderLayout.CENTER);

        panel.add(infoPanel, BorderLayout.NORTH);
        return panel;
    }

    /**
     * 创建 No Auth 面板
     */
    private JPanel createNoAuthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel infoPanel = new JPanel(new BorderLayout(10, 0));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.BORDER_MEDIUM, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel iconLabel = new JLabel("ℹ");
        iconLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN)); // 使用用户设置的字体大小;
        infoPanel.add(iconLabel, BorderLayout.WEST);

        JLabel textLabel = new JLabel(
                "<html><div style='line-height: 1.5;'>" +
                        "<b style='color: " + getTextColorHex() + "; font-size: 10px;'>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_NONE) + "</b><br>" +
                        "<span style='color: " + getDescColorHex() + "; font-size: 9px;'>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_NONE_DESC) + "</span>" +
                        "</div></html>"
        );
        infoPanel.add(textLabel, BorderLayout.CENTER);

        panel.add(infoPanel, BorderLayout.NORTH);
        return panel;
    }

    /**
     * 创建 Basic Auth 面板
     */
    private JPanel createBasicAuthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        // 顶部：描述信息
        JPanel infoPanel = new JPanel(new BorderLayout(10, 0));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.ACCENT_LIGHT, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel iconLabel = new JLabel("ℹ");
        iconLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN)); // 使用用户设置的字体大小;
        infoPanel.add(iconLabel, BorderLayout.WEST);

        JLabel textLabel = new JLabel(
                "<html><div style='line-height: 1.5;'>" +
                        "<b style='color: " + getTitleColorHex() + "; font-size: 10px;'>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_BASIC) + "</b><br>" +
                        "<span style='color: " + getDescColorHex() + "; font-size: 9px;'>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_BASIC_DESC) + "</span>" +
                        "</div></html>"
        );
        infoPanel.add(textLabel, BorderLayout.CENTER);

        panel.add(infoPanel, BorderLayout.NORTH);

        // 中间：输入字段
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel usernameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.AUTH_USERNAME));
        usernameLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        usernameLabel.setForeground(ModernColors.getTextSecondary());
        formPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(usernameField, gbc);

        // 添加水平填充空间
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(Box.createHorizontalGlue(), gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel passwordLabel = new JLabel(I18nUtil.getMessage(MessageKeys.AUTH_PASSWORD));
        passwordLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        passwordLabel.setForeground(ModernColors.getTextSecondary());
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(passwordField, gbc);

        // 添加水平填充空间
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(Box.createHorizontalGlue(), gbc);

        // 填充剩余垂直空间
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(Box.createGlue(), gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建 Bearer Token 面板
     */
    private JPanel createBearerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        // 顶部：描述信息
        JPanel infoPanel = new JPanel(new BorderLayout(10, 0));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.ACCENT_LIGHT, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel iconLabel = new JLabel("ℹ");
        iconLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN)); // 使用用户设置的字体大小;
        infoPanel.add(iconLabel, BorderLayout.WEST);

        JLabel textLabel = new JLabel(
                "<html><div style='line-height: 1.5;'>" +
                        "<b style='color: " + getTitleColorHex() + "; font-size: 10px;'>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_BEARER) + "</b><br>" +
                        "<span style='color: " + getDescColorHex() + "; font-size: 9px;'>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_BEARER_DESC) + "</span>" +
                        "</div></html>"
        );
        infoPanel.add(textLabel, BorderLayout.CENTER);

        panel.add(infoPanel, BorderLayout.NORTH);

        // 中间：输入字段
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // Token
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel tokenLabel = new JLabel(I18nUtil.getMessage(MessageKeys.AUTH_TOKEN));
        tokenLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        tokenLabel.setForeground(ModernColors.getTextSecondary());
        formPanel.add(tokenLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(tokenField, gbc);

        // 添加水平填充空间
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(Box.createHorizontalGlue(), gbc);

        // 填充剩余垂直空间
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(Box.createGlue(), gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        return panel;
    }

    public String getAuthType() {
        AuthType selected = (AuthType) typeCombo.getSelectedItem();
        return selected != null ? selected.getConstant() : AUTH_TYPE_INHERIT;
    }

    public void setAuthType(String type) {
        AuthType authType = AuthType.fromConstant(type);
        typeCombo.setSelectedItem(authType);
        currentType = authType;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public void setUsername(String u) {
        usernameField.setText(u == null ? "" : u);
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public void setPassword(String p) {
        passwordField.setText(p == null ? "" : p);
    }

    public String getToken() {
        return tokenField.getText();
    }

    public void setToken(String t) {
        tokenField.setText(t == null ? "" : t);
    }

    /**
     * 注册脏数据监听器
     */
    public void addDirtyListener(Runnable listener) {
        if (listener != null) dirtyListeners.add(listener);
    }

    private void fireDirty() {
        for (Runnable l : dirtyListeners) l.run();
    }
}