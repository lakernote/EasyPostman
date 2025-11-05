package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.EasyPostmanTextField;
import com.laker.postman.model.AuthType;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证Tab面板
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

    public AuthTabPanel() {
        setLayout(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel(I18nUtil.getMessage(MessageKeys.AUTH_TYPE_LABEL)));
        typeCombo = new JComboBox<>(AuthType.values());
        top.add(typeCombo);
        add(top, BorderLayout.NORTH);
        JPanel cardPanel = new JPanel(new CardLayout());
        // Inherit Auth
        JPanel inheritPanel = new JPanel();
        inheritPanel.add(new JLabel("<html><i>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_INHERIT_DESC) + "</i></html>"));
        cardPanel.add(inheritPanel, AUTH_TYPE_INHERIT);
        // No Auth
        JPanel noAuthPanel = new JPanel();
        noAuthPanel.add(new JLabel("<html><i>" + I18nUtil.getMessage(MessageKeys.AUTH_TYPE_NONE_DESC) + "</i></html>"));
        cardPanel.add(noAuthPanel, AUTH_TYPE_NONE);
        // Basic
        JPanel basicPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        basicPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.AUTH_USERNAME)));
        usernameField = new EasyPostmanTextField(12);
        basicPanel.add(usernameField);
        basicPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.AUTH_PASSWORD)));
        passwordField = new EasyPostmanTextField(12);
        basicPanel.add(passwordField);
        cardPanel.add(basicPanel, AUTH_TYPE_BASIC);
        // Bearer
        JPanel bearerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bearerPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.AUTH_TOKEN)));
        tokenField = new EasyPostmanTextField(24);
        bearerPanel.add(tokenField);
        cardPanel.add(bearerPanel, AUTH_TYPE_BEARER);
        add(cardPanel, BorderLayout.CENTER);
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