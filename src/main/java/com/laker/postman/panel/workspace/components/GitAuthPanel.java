package com.laker.postman.panel.workspace.components;

import com.laker.postman.model.GitAuthType;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Git认证配置面板公共组件
 * 用于WorkspaceCreateDialog和RemoteConfigDialog复用
 */
public class GitAuthPanel extends JPanel {

    @Getter
    private JComboBox<GitAuthType> authTypeCombo;
    @Getter
    private JTextField passwordUsernameField;
    @Getter
    private JPasswordField passwordField;
    @Getter
    private JTextField tokenUsernameField;
    @Getter
    private JTextField tokenField;

    private JPanel authDetailsPanel;

    public GitAuthPanel() {
        initComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initComponents() {
        authTypeCombo = new JComboBox<>(GitAuthType.values());
        authTypeCombo.setRenderer(new GitAuthTypeRenderer());

        passwordUsernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        tokenUsernameField = new JTextField(15);
        tokenField = new JTextField(25);

        // 设置默认字体
        Font defaultFont = EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12);
        passwordUsernameField.setFont(defaultFont);
        passwordField.setFont(defaultFont);
        tokenUsernameField.setFont(defaultFont);
        tokenField.setFont(defaultFont);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // 认证类型选择
        JPanel typePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        typePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_TYPE) + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        typePanel.add(authTypeCombo, gbc);

        add(typePanel, BorderLayout.NORTH);

        // 认证详情面板
        authDetailsPanel = new JPanel(new CardLayout());
        authDetailsPanel.add(createNoAuthPanel(), GitAuthType.NONE.name());
        authDetailsPanel.add(createPasswordAuthPanel(), GitAuthType.PASSWORD.name());
        authDetailsPanel.add(createTokenAuthPanel(), GitAuthType.TOKEN.name());
        authDetailsPanel.add(createSshAuthPanel(), GitAuthType.SSH_KEY.name());

        add(authDetailsPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        authTypeCombo.addActionListener(e -> updateAuthDetailsPanel());
        updateAuthDetailsPanel(); // 初始状态
    }

    private JPanel createNoAuthPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_NONE));
        label.setFont(EasyPostManFontUtil.getDefaultFont(Font.ITALIC, 11));
        label.setForeground(Color.GRAY);
        panel.add(label);
        return panel;
    }

    private JPanel createPasswordAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_USERNAME) + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(passwordUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_PASSWORD) + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(passwordField, gbc);

        return panel;
    }

    private JPanel createTokenAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_USERNAME) + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(tokenUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_TOKEN) + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(tokenField, gbc);

        return panel;
    }

    private JPanel createSshAuthPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("SSH Key 认证（暂未实现）");
        label.setFont(EasyPostManFontUtil.getDefaultFont(Font.ITALIC, 11));
        label.setForeground(Color.GRAY);
        panel.add(label);
        return panel;
    }

    private void updateAuthDetailsPanel() {
        GitAuthType selectedAuth = (GitAuthType) authTypeCombo.getSelectedItem();
        if (selectedAuth != null) {
            CardLayout layout = (CardLayout) authDetailsPanel.getLayout();
            layout.show(authDetailsPanel, selectedAuth.name());
        }
        authDetailsPanel.revalidate();
        authDetailsPanel.repaint();
    }

    /**
     * 设置组件启用状态
     */
    public void setComponentsEnabled(boolean enabled) {
        authTypeCombo.setEnabled(enabled);
        passwordUsernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        tokenUsernameField.setEnabled(enabled);
        tokenField.setEnabled(enabled);
    }

    /**
     * 验证认证信息
     */
    public void validateAuth() throws IllegalArgumentException {
        GitAuthType selectedAuthType = (GitAuthType) authTypeCombo.getSelectedItem();
        if (selectedAuthType == GitAuthType.PASSWORD) {
            String usernameText = passwordUsernameField.getText().trim();
            String passwordText = new String(passwordField.getPassword()).trim();
            if (usernameText.isEmpty() || passwordText.isEmpty()) {
                throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_AUTH_REQUIRED));
            }
        } else if (selectedAuthType == GitAuthType.TOKEN) {
            String usernameText = tokenUsernameField.getText().trim();
            String tokenText = tokenField.getText().trim();
            if (usernameText.isEmpty() || tokenText.isEmpty()) {
                throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_AUTH_REQUIRED));
            }
        }
    }

    /**
     * 获取用户名（根据认证类型）
     */
    public String getUsername() {
        GitAuthType authType = (GitAuthType) authTypeCombo.getSelectedItem();
        if (authType == GitAuthType.PASSWORD) {
            return passwordUsernameField.getText().trim();
        } else if (authType == GitAuthType.TOKEN) {
            return tokenUsernameField.getText().trim();
        }
        return "";
    }

    /**
     * 获取密码
     */
    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    /**
     * 获取Token
     */
    public String getToken() {
        return tokenField.getText().trim();
    }

    /**
     * Git认证类型渲染器
     */
    private static class GitAuthTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof GitAuthType authType) {
                switch (authType) {
                    case NONE:
                        setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_NONE));
                        break;
                    case PASSWORD:
                        setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_PASSWORD));
                        break;
                    case TOKEN:
                        setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_TOKEN));
                        break;
                    case SSH_KEY:
                        setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_SSH));
                        break;
                }
            }

            return this;
        }
    }
}
