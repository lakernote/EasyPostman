package com.laker.postman.panel.workspace.components;

import com.laker.postman.common.component.EasyPasswordField;
import com.laker.postman.common.component.setting.SettingsInputStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.GitAuthType;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Git认证配置面板公共组件
 */
public class GitAuthPanel extends JPanel {

    private static final String PASSWORD_FIELD_STYLE = "arc: 8; margin: 7, 12, 7, 12";
    private static final Dimension BROWSE_BUTTON_SIZE = new Dimension(34, 32);

    @Getter
    private JComboBox<GitAuthType> authTypeCombo;
    @Getter
    private JTextField passwordUsernameField;
    @Getter
    private EasyPasswordField passwordField;
    @Getter
    private JTextField tokenUsernameField;
    @Getter
    private EasyPasswordField tokenField;
    @Getter
    private JTextField sshKeyPathField;
    @Getter
    private EasyPasswordField sshPassphraseField;

    private JPanel authDetailsPanel;

    // SSH 认证相关组件
    private JButton sshKeyBrowseButton;

    public GitAuthPanel() {
        initComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initComponents() {
        authTypeCombo = new JComboBox<>(GitAuthType.values());
        authTypeCombo.setRenderer(new GitAuthTypeRenderer());
        SettingsInputStyle.apply(authTypeCombo);

        // 调整字段宽度以更好匹配父对话框
        passwordUsernameField = new JTextField(20);
        passwordField = new EasyPasswordField(20);
        tokenUsernameField = new JTextField(20);
        tokenField = new EasyPasswordField(20);
        sshKeyPathField = new JTextField(20);
        sshPassphraseField = new EasyPasswordField(20);

        sshKeyBrowseButton = new JButton(IconUtil.createThemed("icons/file.svg", 16, 16));
        sshKeyBrowseButton.setToolTipText(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_SSH_SELECT_KEY));
        sshKeyBrowseButton.setFocusPainted(false);
        sshKeyBrowseButton.setPreferredSize(BROWSE_BUTTON_SIZE);
        sshKeyBrowseButton.setMinimumSize(BROWSE_BUTTON_SIZE);
        sshKeyBrowseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 设置默认字体（EasyPostmanPasswordField 已自动设置默认字体）
        Font defaultFont = FontsUtil.getDefaultFont(Font.PLAIN);
        passwordUsernameField.setFont(defaultFont);
        tokenUsernameField.setFont(defaultFont);
        sshKeyPathField.setFont(defaultFont);
        styleEditableField(passwordUsernameField);
        styleEditableField(tokenUsernameField);
        styleEditableField(sshKeyPathField);
        stylePasswordField(passwordField);
        stylePasswordField(tokenField);
        stylePasswordField(sshPassphraseField);
    }

    private void setupLayout() {
        setLayout(new MigLayout(
                "insets 0, fillx, novisualpadding",
                "[right]12[grow,fill]",
                "[]8[]"
        ));
        setOpaque(false);

        JLabel authTypeLabel = createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_TYPE) + ":");
        add(authTypeLabel, "cell 0 0, aligny center");
        add(authTypeCombo, "cell 1 0, growx");

        // 认证详情面板
        authDetailsPanel = new JPanel(new CardLayout()) {
            @Override
            public Dimension getPreferredSize() {
                for (Component component : getComponents()) {
                    if (component.isVisible()) {
                        return component.getPreferredSize();
                    }
                }
                return super.getPreferredSize();
            }
        };
        authDetailsPanel.setOpaque(false);
        authDetailsPanel.add(createNoAuthPanel(), GitAuthType.NONE.name());
        authDetailsPanel.add(createPasswordAuthPanel(), GitAuthType.PASSWORD.name());
        authDetailsPanel.add(createTokenAuthPanel(), GitAuthType.TOKEN.name());
        authDetailsPanel.add(createSshAuthPanel(), GitAuthType.SSH_KEY.name());

        add(authDetailsPanel, "cell 0 1 2 1, growx");
    }

    private void setupEventHandlers() {
        authTypeCombo.addActionListener(e -> updateAuthDetailsPanel());

        // SSH 私钥文件选择事件
        sshKeyBrowseButton.addActionListener(e -> selectSshKeyFile());

        updateAuthDetailsPanel(); // 初始状态
    }

    /**
     * 选择SSH私钥文件
     */
    private void selectSshKeyFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_SSH_SELECT_KEY));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // 设置默认目录为用户主目录下的 .ssh 文件夹
        String userHome = System.getProperty("user.home");
        File sshDir = new File(userHome, ".ssh");
        if (sshDir.exists() && sshDir.isDirectory()) {
            fileChooser.setCurrentDirectory(sshDir);
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            sshKeyPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private JPanel createNoAuthPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
        JLabel iconLabel = new JLabel(IconUtil.createThemed("icons/info.svg", 14, 14));
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_NONE));
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -1));
        label.setForeground(ModernColors.getTextSecondary());
        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPasswordAuthPanel() {
        JPanel panel = createCredentialFormPanel();
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_USERNAME) + ":"), "cell 0 0, aligny center");
        panel.add(passwordUsernameField, "cell 1 0, growx");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_PASSWORD) + ":"), "cell 0 1, aligny center");
        panel.add(passwordField, "cell 1 1, growx");
        return panel;
    }

    private JPanel createTokenAuthPanel() {
        JPanel panel = createCredentialFormPanel();
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_USERNAME) + ":"), "cell 0 0, aligny center");
        panel.add(tokenUsernameField, "cell 1 0, growx");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_TOKEN) + ":"), "cell 0 1, aligny center");
        panel.add(tokenField, "cell 1 1, growx");
        return panel;
    }

    private JPanel createSshAuthPanel() {
        JPanel panel = createCredentialFormPanel();
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_SSH_KEY_PATH) + ":"), "cell 0 0, aligny center");
        panel.add(sshKeyPathField, "cell 1 0, growx");
        panel.add(sshKeyBrowseButton, "cell 2 0, w 34!, h 32!");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_SSH_PASSPHRASE) + ":"), "cell 0 1, aligny center");
        panel.add(sshPassphraseField, "cell 1 1 2 1, growx");
        return panel;
    }

    private JPanel createCredentialFormPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding",
                "[right]12[grow,fill]8[pref!]",
                "[]8[]"
        ));
        panel.setOpaque(false);
        return panel;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(ModernColors.getTextPrimary());
        return label;
    }

    private void styleEditableField(JTextField field) {
        SettingsInputStyle.apply(field);
    }

    private void stylePasswordField(EasyPasswordField field) {
        field.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        field.setBackground(ModernColors.getInputBackgroundColor());
        field.setForeground(ModernColors.getTextPrimary());
        field.setCustomStyle(PASSWORD_FIELD_STYLE);
    }

    private void updateAuthDetailsPanel() {
        GitAuthType selectedAuth = (GitAuthType) authTypeCombo.getSelectedItem();
        if (selectedAuth != null) {
            CardLayout layout = (CardLayout) authDetailsPanel.getLayout();
            layout.show(authDetailsPanel, selectedAuth.name());
            syncAuthDetailsSize();
        }
        authDetailsPanel.revalidate();
        authDetailsPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.pack();
            }
        });
    }

    private void syncAuthDetailsSize() {
        Component selectedComponent = getVisibleAuthDetailsComponent();
        if (selectedComponent == null) {
            return;
        }
        Dimension preferredSize = selectedComponent.getPreferredSize();
        Dimension fixedHeightSize = new Dimension(preferredSize.width, preferredSize.height);
        authDetailsPanel.setPreferredSize(fixedHeightSize);
        authDetailsPanel.setMinimumSize(new Dimension(0, preferredSize.height));
        authDetailsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));
    }

    private Component getVisibleAuthDetailsComponent() {
        for (Component component : authDetailsPanel.getComponents()) {
            if (component.isVisible()) {
                return component;
            }
        }
        return null;
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
        sshKeyPathField.setEnabled(enabled);
        sshPassphraseField.setEnabled(enabled);
        sshKeyBrowseButton.setEnabled(enabled);
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
            String tokenText = tokenField.getPasswordText().trim();
            if (usernameText.isEmpty() || tokenText.isEmpty()) {
                throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_AUTH_REQUIRED));
            }
        } else if (selectedAuthType == GitAuthType.SSH_KEY) {
            String sshKeyPathText = sshKeyPathField.getText().trim();
            if (sshKeyPathText.isEmpty()) {
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
        return passwordField.getPasswordText();
    }

    /**
     * 获取Token
     */
    public String getToken() {
        return tokenField.getPasswordText();
    }

    /**
     * 获取SSH私钥路径
     */
    public String getSshKeyPath() {
        return sshKeyPathField.getText().trim();
    }

    /**
     * 获取SSH私钥密码
     */
    public String getSshPassphrase() {
        return sshPassphraseField.getPasswordText();
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
