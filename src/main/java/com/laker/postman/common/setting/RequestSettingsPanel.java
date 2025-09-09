package com.laker.postman.common.setting;

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
 * 请求设置面板
 */
public class RequestSettingsPanel extends JPanel {
    private JTextField maxBodySizeField;
    private JTextField requestTimeoutField;
    private JTextField maxDownloadSizeField;
    private JCheckBox followRedirectsCheckBox;
    private JButton saveBtn;
    private JButton cancelBtn;

    // 用于输入验证的映射
    private final Map<JTextField, Predicate<String>> validators = new HashMap<>();
    private final Map<JTextField, String> errorMessages = new HashMap<>();

    public RequestSettingsPanel() {
        initUI();
        registerListeners();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建请求设置面板
        JPanel requestPanel = createSectionPanel(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TITLE));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 响应体最大显示大小
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel maxBodySizeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_BODY_SIZE));
        maxBodySizeLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_BODY_SIZE_TOOLTIP));
        requestPanel.add(maxBodySizeLabel, gbc);

        gbc.gridx = 1;
        maxBodySizeField = new JTextField(10);
        int maxBodySizeKB = SettingManager.getMaxBodySize() / 1024;
        maxBodySizeField.setText(String.valueOf(maxBodySizeKB));
        requestPanel.add(maxBodySizeField, gbc);

        // 请求超时时间
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel requestTimeoutLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TIMEOUT));
        requestTimeoutLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TIMEOUT_TOOLTIP));
        requestPanel.add(requestTimeoutLabel, gbc);

        gbc.gridx = 1;
        requestTimeoutField = new JTextField(10);
        requestTimeoutField.setText(String.valueOf(SettingManager.getRequestTimeout()));
        requestPanel.add(requestTimeoutField, gbc);

        // 最大响应下载大小
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel maxDownloadSizeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_DOWNLOAD_SIZE));
        maxDownloadSizeLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_DOWNLOAD_SIZE_TOOLTIP));
        requestPanel.add(maxDownloadSizeLabel, gbc);

        gbc.gridx = 1;
        maxDownloadSizeField = new JTextField(10);
        int maxDownloadSizeMB = SettingManager.getMaxDownloadSize() / (1024 * 1024);
        maxDownloadSizeField.setText(String.valueOf(maxDownloadSizeMB));
        requestPanel.add(maxDownloadSizeField, gbc);

        // 自动重定向设置
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel followRedirectsLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_FOLLOW_REDIRECTS));
        followRedirectsLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_FOLLOW_REDIRECTS_TOOLTIP));
        requestPanel.add(followRedirectsLabel, gbc);

        gbc.gridx = 1;
        followRedirectsCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_FOLLOW_REDIRECTS_CHECKBOX), SettingManager.isFollowRedirects());
        requestPanel.add(followRedirectsCheckBox, gbc);

        mainPanel.add(requestPanel);
        mainPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // 创建按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        saveBtn = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_SAVE));
        cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_CANCEL));

        saveBtn.setPreferredSize(new Dimension(100, 30));
        cancelBtn.setPreferredSize(new Dimension(100, 30));

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        add(scrollPane, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(450, 300));

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
        validators.put(maxBodySizeField, s -> isInteger(s) && Integer.parseInt(s) >= 0);
        validators.put(requestTimeoutField, s -> isInteger(s) && Integer.parseInt(s) >= 0);
        validators.put(maxDownloadSizeField, s -> isInteger(s) && Integer.parseInt(s) >= 0);

        errorMessages.put(maxBodySizeField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_BODY_SIZE_ERROR));
        errorMessages.put(requestTimeoutField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_TIMEOUT_ERROR));
        errorMessages.put(maxDownloadSizeField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_DOWNLOAD_SIZE_ERROR));

        DocumentListener validationListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { validateField(e); }
            @Override
            public void removeUpdate(DocumentEvent e) { validateField(e); }
            @Override
            public void changedUpdate(DocumentEvent e) { validateField(e); }

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
                    Window window = SwingUtilities.getWindowAncestor(RequestSettingsPanel.this);
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
            }
        };

        maxBodySizeField.addKeyListener(keyAdapter);
        requestTimeoutField.addKeyListener(keyAdapter);
        maxDownloadSizeField.addKeyListener(keyAdapter);
        followRedirectsCheckBox.addKeyListener(keyAdapter);
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
            int sizeKB = Integer.parseInt(maxBodySizeField.getText().trim());
            int timeout = Integer.parseInt(requestTimeoutField.getText().trim());
            int maxDownloadMB = Integer.parseInt(maxDownloadSizeField.getText().trim());

            SettingManager.setMaxBodySize(sizeKB * 1024);
            SettingManager.setRequestTimeout(timeout);
            SettingManager.setMaxDownloadSize(maxDownloadMB * 1024 * 1024);
            SettingManager.setFollowRedirects(followRedirectsCheckBox.isSelected());

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
