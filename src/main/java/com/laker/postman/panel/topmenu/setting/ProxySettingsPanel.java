package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 网络代理设置面板
 */
public class ProxySettingsPanel extends JPanel {
    // 网络代理设置组件
    private JCheckBox proxyEnabledCheckBox;
    private JComboBox<String> proxyTypeComboBox;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JTextField proxyUsernameField;
    private JPasswordField proxyPasswordField;
    private JCheckBox sslVerificationDisabledCheckBox;

    private JButton saveBtn;
    private JButton cancelBtn;

    private final Map<JTextField, Predicate<String>> validators = new HashMap<>();
    private final Map<JTextField, String> errorMessages = new HashMap<>();

    public ProxySettingsPanel() {
        initUI();
        registerListeners();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 网络代理设置面板
        JPanel proxyPanel = createSectionPanel(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TITLE));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 代理使能
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel proxyEnabledLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_ENABLED));
        proxyEnabledLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_ENABLED_TOOLTIP));
        proxyPanel.add(proxyEnabledLabel, gbc);

        gbc.gridx = 1;
        proxyEnabledCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_ENABLED_CHECKBOX), SettingManager.isProxyEnabled());
        proxyPanel.add(proxyEnabledCheckBox, gbc);

        // 代理类型
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel proxyTypeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TYPE));
        proxyTypeLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TYPE_TOOLTIP));
        proxyPanel.add(proxyTypeLabel, gbc);

        gbc.gridx = 1;
        proxyTypeComboBox = new JComboBox<>(new String[]{
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TYPE_HTTP),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TYPE_SOCKS)
        });
        proxyTypeComboBox.setSelectedItem(SettingManager.getProxyType());
        proxyPanel.add(proxyTypeComboBox, gbc);

        // 代理主机
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel proxyHostLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_HOST));
        proxyHostLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_HOST_TOOLTIP));
        proxyPanel.add(proxyHostLabel, gbc);

        gbc.gridx = 1;
        proxyHostField = new JTextField(10);
        proxyHostField.setText(SettingManager.getProxyHost());
        proxyPanel.add(proxyHostField, gbc);

        // 代理端口
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel proxyPortLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PORT));
        proxyPortLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PORT_TOOLTIP));
        proxyPanel.add(proxyPortLabel, gbc);

        gbc.gridx = 1;
        proxyPortField = new JTextField(10);
        proxyPortField.setText(String.valueOf(SettingManager.getProxyPort()));
        proxyPanel.add(proxyPortField, gbc);

        // 代理用户名
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel proxyUsernameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_USERNAME));
        proxyUsernameLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_USERNAME_TOOLTIP));
        proxyPanel.add(proxyUsernameLabel, gbc);

        gbc.gridx = 1;
        proxyUsernameField = new JTextField(10);
        proxyUsernameField.setText(SettingManager.getProxyUsername());
        proxyPanel.add(proxyUsernameField, gbc);

        // 代理密码
        gbc.gridx = 0;
        gbc.gridy = 5;
        JLabel proxyPasswordLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PASSWORD));
        proxyPasswordLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PASSWORD_TOOLTIP));
        proxyPanel.add(proxyPasswordLabel, gbc);

        gbc.gridx = 1;
        proxyPasswordField = new JPasswordField(10);
        proxyPasswordField.setText(SettingManager.getProxyPassword());
        proxyPanel.add(proxyPasswordField, gbc);

        // SSL 验证禁用选项
        gbc.gridx = 0;
        gbc.gridy = 6;
        JLabel sslVerificationLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_SSL_VERIFICATION));
        sslVerificationLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_SSL_VERIFICATION_TOOLTIP));
        proxyPanel.add(sslVerificationLabel, gbc);

        gbc.gridx = 1;
        sslVerificationDisabledCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_SSL_VERIFICATION_CHECKBOX), SettingManager.isProxySslVerificationDisabled());
        proxyPanel.add(sslVerificationDisabledCheckBox, gbc);

        mainPanel.add(proxyPanel);
        mainPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        saveBtn = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_SAVE));
        cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_CANCEL));

        saveBtn.setPreferredSize(new Dimension(100, 30));
        cancelBtn.setPreferredSize(new Dimension(100, 30));

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        add(scrollPane, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(480, 360));

        setupValidators();
        setupKeyboardNavigation();
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font(panel.getFont().getName(), Font.BOLD, 12)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private void setupValidators() {
        validators.put(proxyPortField, s -> isInteger(s) && Integer.parseInt(s) > 0);
        errorMessages.put(proxyPortField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_PROXY_PORT_ERROR));

        DocumentListener validationListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateField(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateField(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateField(e);
            }

            private void validateField(DocumentEvent e) {
                for (Map.Entry<JTextField, Predicate<String>> entry : validators.entrySet()) {
                    JTextField field = entry.getKey();
                    if (field.getDocument() == e.getDocument()) {
                        String text = field.getText().trim();
                        boolean valid = text.isEmpty() || entry.getValue().test(text);
                        field.setBackground(valid ? Color.WHITE : new Color(255, 220, 220));
                        field.setToolTipText(valid ? null : errorMessages.get(field));
                        break;
                    }
                }
            }
        };

        for (JTextField field : validators.keySet()) {
            field.getDocument().addDocumentListener(validationListener);
        }
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void setupKeyboardNavigation() {
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    saveSettings();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    Window window = SwingUtilities.getWindowAncestor(ProxySettingsPanel.this);
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
            }
        };

        proxyEnabledCheckBox.addKeyListener(keyAdapter);
        proxyHostField.addKeyListener(keyAdapter);
        proxyPortField.addKeyListener(keyAdapter);
        proxyUsernameField.addKeyListener(keyAdapter);
        proxyPasswordField.addKeyListener(keyAdapter);
        sslVerificationDisabledCheckBox.addKeyListener(keyAdapter);
    }

    private void registerListeners() {
        saveBtn.addActionListener(e -> saveSettings());
        cancelBtn.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JDialog dialog) {
                dialog.dispose();
            }
        });
    }

    private void saveSettings() {
        // 验证所有字段
        for (Map.Entry<JTextField, Predicate<String>> entry : validators.entrySet()) {
            JTextField field = entry.getKey();
            String text = field.getText().trim();

            if (text.isEmpty() || !entry.getValue().test(text)) {
                field.setBackground(new Color(255, 220, 220));
                field.requestFocus();
                JOptionPane.showMessageDialog(this, errorMessages.get(field),
                        I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_TITLE), JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            int proxyPort = Integer.parseInt(proxyPortField.getText().trim());

            // 保存代理设置，并在设置更改后清理HTTP客户端缓存
            boolean proxyEnabledChanged = SettingManager.isProxyEnabled() != proxyEnabledCheckBox.isSelected();
            String oldProxyType = SettingManager.getProxyType();
            String oldProxyHost = SettingManager.getProxyHost();
            int oldProxyPort = SettingManager.getProxyPort();
            String oldProxyUsername = SettingManager.getProxyUsername();
            String oldProxyPassword = SettingManager.getProxyPassword();
            boolean oldSslVerificationDisabled = SettingManager.isProxySslVerificationDisabled();

            SettingManager.setProxyEnabled(proxyEnabledCheckBox.isSelected());
            SettingManager.setProxyType((String) proxyTypeComboBox.getSelectedItem());
            SettingManager.setProxyHost(proxyHostField.getText().trim());
            SettingManager.setProxyPort(proxyPort);
            SettingManager.setProxyUsername(proxyUsernameField.getText().trim());
            SettingManager.setProxyPassword(new String(proxyPasswordField.getPassword()));
            SettingManager.setProxySslVerificationDisabled(sslVerificationDisabledCheckBox.isSelected());

            // 检查代理设置是否有变更，如果有变更则清理HTTP客户端缓存
            boolean proxySettingsChanged = proxyEnabledChanged ||
                    !oldProxyType.equals(proxyTypeComboBox.getSelectedItem()) ||
                    !oldProxyHost.equals(proxyHostField.getText().trim()) ||
                    oldProxyPort != proxyPort ||
                    !oldProxyUsername.equals(proxyUsernameField.getText().trim()) ||
                    !oldProxyPassword.equals(new String(proxyPasswordField.getPassword())) ||
                    oldSslVerificationDisabled != sslVerificationDisabledCheckBox.isSelected();

            if (proxySettingsChanged) {
                // 清理HTTP客户端缓存，确保新的代理设置生效
                OkHttpClientManager.clearClientCache();
            }

            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS),
                    I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_TITLE), JOptionPane.INFORMATION_MESSAGE);

            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JDialog dialog) {
                dialog.dispose();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_INVALID_NUMBER),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
        }
    }
}
