package com.laker.postman.panel.collections.edit;

import javax.swing.*;
import java.awt.*;

/**
 * 认证Tab面板
 */
public class AuthTabPanel extends JPanel {
    public static final String AUTH_TYPE_NONE = "No Auth";
    public static final String AUTH_TYPE_BASIC = "Basic Auth";
    public static final String AUTH_TYPE_BEARER = "Bearer Token";
    private final JComboBox<String> typeCombo;
    private final JTextField usernameField;
    private final JTextField passwordField;
    private final JTextField tokenField;
    private String currentType = AUTH_TYPE_NONE;

    public AuthTabPanel() {
        setLayout(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Auth Type:"));
        typeCombo = new JComboBox<>(new String[]{AUTH_TYPE_NONE, AUTH_TYPE_BASIC, AUTH_TYPE_BEARER});
        top.add(typeCombo);
        add(top, BorderLayout.NORTH);
        JPanel cardPanel = new JPanel(new CardLayout());
        // No Auth
        JPanel noAuthPanel = new JPanel();
        noAuthPanel.add(new JLabel("This request does not use any authorization."));
        cardPanel.add(noAuthPanel, AUTH_TYPE_NONE);
        // Basic
        JPanel basicPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        basicPanel.add(new JLabel("Username:"));
        usernameField = new JTextField(12);
        basicPanel.add(usernameField);
        basicPanel.add(new JLabel("Password:"));
        passwordField = new JTextField(12);
        basicPanel.add(passwordField);
        cardPanel.add(basicPanel, AUTH_TYPE_BASIC);
        // Bearer
        JPanel bearerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bearerPanel.add(new JLabel("Token:"));
        tokenField = new JTextField(24);
        bearerPanel.add(tokenField);
        cardPanel.add(bearerPanel, AUTH_TYPE_BEARER);
        add(cardPanel, BorderLayout.CENTER);
        typeCombo.addActionListener(e -> {
            currentType = (String) typeCombo.getSelectedItem();
            CardLayout cl = (CardLayout) cardPanel.getLayout();
            cl.show(cardPanel, currentType);
        });
        // 默认显示
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, AUTH_TYPE_NONE);
    }

    public String getAuthType() {
        return (String) typeCombo.getSelectedItem();
    }

    public void setAuthType(String type) {
        if (type == null) type = AUTH_TYPE_NONE;
        typeCombo.setSelectedItem(type);
        currentType = type;
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
}