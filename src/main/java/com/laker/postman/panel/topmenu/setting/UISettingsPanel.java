package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;

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
 * 界面设置面板 - 下载进度、历史记录等UI相关配置
 */
public class UISettingsPanel extends JPanel {
    private JCheckBox showDownloadProgressCheckBox;
    private JTextField downloadProgressDialogThresholdField;
    private JTextField maxHistoryCountField;
    private JTextField maxOpenedRequestsCountField;
    private JButton saveBtn;
    private JButton cancelBtn;
    private JCheckBox autoFormatResponseCheckBox;
    private JCheckBox sidebarExpandedCheckBox;

    private final Map<JTextField, Predicate<String>> validators = new HashMap<>();
    private final Map<JTextField, String> errorMessages = new HashMap<>();

    public UISettingsPanel() {
        initUI();
        registerListeners();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 下载设置面板
        JPanel downloadPanel = createSectionPanel(I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_TITLE));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 下载进度对话框
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        showDownloadProgressCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_SHOW_PROGRESS), SettingManager.isShowDownloadProgressDialog());
        showDownloadProgressCheckBox.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_SHOW_PROGRESS_TOOLTIP));
        downloadPanel.add(showDownloadProgressCheckBox, gbc);

        // 进度弹窗阈值
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel downloadProgressDialogThresholdLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_THRESHOLD));
        downloadProgressDialogThresholdLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_THRESHOLD_TOOLTIP));
        downloadPanel.add(downloadProgressDialogThresholdLabel, gbc);

        gbc.gridx = 1;
        downloadProgressDialogThresholdField = new JTextField(10);
        int thresholdMB = SettingManager.getDownloadProgressDialogThreshold() / (1024 * 1024);
        downloadProgressDialogThresholdField.setText(String.valueOf(thresholdMB));
        downloadPanel.add(downloadProgressDialogThresholdField, gbc);

        // 设置下载阈值字段的启用状态根据复选框状态
        downloadProgressDialogThresholdField.setEnabled(showDownloadProgressCheckBox.isSelected());
        downloadProgressDialogThresholdLabel.setEnabled(showDownloadProgressCheckBox.isSelected());
        showDownloadProgressCheckBox.addItemListener(e -> {
            boolean selected = e.getStateChange() == java.awt.event.ItemEvent.SELECTED;
            downloadProgressDialogThresholdField.setEnabled(selected);
            downloadProgressDialogThresholdLabel.setEnabled(selected);
        });

        // 通用设置面板
        JPanel generalPanel = createSectionPanel(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_TITLE));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 历史记录数量
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel maxHistoryCountLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_MAX_HISTORY));
        maxHistoryCountLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_MAX_HISTORY_TOOLTIP));
        generalPanel.add(maxHistoryCountLabel, gbc);

        gbc.gridx = 1;
        maxHistoryCountField = new JTextField(10);
        maxHistoryCountField.setText(String.valueOf(SettingManager.getMaxHistoryCount()));
        generalPanel.add(maxHistoryCountField, gbc);

        // 最大打开请求数
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel maxOpenedRequestsCountLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_MAX_OPENED_REQUESTS));
        maxOpenedRequestsCountLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_MAX_OPENED_REQUESTS_TOOLTIP));
        generalPanel.add(maxOpenedRequestsCountLabel, gbc);

        gbc.gridx = 1;
        maxOpenedRequestsCountField = new JTextField(10);
        maxOpenedRequestsCountField.setText(String.valueOf(SettingManager.getMaxOpenedRequestsCount()));
        generalPanel.add(maxOpenedRequestsCountField, gbc);

        // 自动格式化响应体
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        autoFormatResponseCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_AUTO_FORMAT_RESPONSE), SettingManager.isAutoFormatResponse());
        autoFormatResponseCheckBox.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_AUTO_FORMAT_RESPONSE_TOOLTIP));
        generalPanel.add(autoFormatResponseCheckBox, gbc);

        // 侧边栏展开设置
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        sidebarExpandedCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_EXPANDED), SettingManager.isSidebarExpanded());
        sidebarExpandedCheckBox.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_EXPANDED_TOOLTIP));
        generalPanel.add(sidebarExpandedCheckBox, gbc);


        mainPanel.add(downloadPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(generalPanel);
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

        setPreferredSize(new Dimension(500, 350));

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
        validators.put(downloadProgressDialogThresholdField, s -> isInteger(s) && Integer.parseInt(s) >= 0);
        validators.put(maxHistoryCountField, s -> isInteger(s) && Integer.parseInt(s) > 0);
        validators.put(maxOpenedRequestsCountField, s -> isInteger(s) && Integer.parseInt(s) > 0);

        errorMessages.put(downloadProgressDialogThresholdField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_THRESHOLD_ERROR));
        errorMessages.put(maxHistoryCountField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_HISTORY_ERROR));
        errorMessages.put(maxOpenedRequestsCountField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_OPENED_REQUESTS_ERROR));

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
                    Window window = SwingUtilities.getWindowAncestor(UISettingsPanel.this);
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
            }
        };

        downloadProgressDialogThresholdField.addKeyListener(keyAdapter);
        showDownloadProgressCheckBox.addKeyListener(keyAdapter);
        maxHistoryCountField.addKeyListener(keyAdapter);
        maxOpenedRequestsCountField.addKeyListener(keyAdapter);
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

            // 跳过禁用的字段
            if (!field.isEnabled()) {
                continue;
            }

            if (text.isEmpty() || !entry.getValue().test(text)) {
                field.setBackground(new Color(255, 220, 220));
                field.requestFocus();
                JOptionPane.showMessageDialog(this, errorMessages.get(field),
                        I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_TITLE), JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            int thresholdMB = Integer.parseInt(downloadProgressDialogThresholdField.getText().trim());
            int maxHistoryCount = Integer.parseInt(maxHistoryCountField.getText().trim());
            int maxOpenedRequestsCount = Integer.parseInt(maxOpenedRequestsCountField.getText().trim());

            // 检查侧边栏设置是否改变
            boolean sidebarExpandedChanged = SettingManager.isSidebarExpanded() != sidebarExpandedCheckBox.isSelected();

            SettingManager.setShowDownloadProgressDialog(showDownloadProgressCheckBox.isSelected());
            SettingManager.setDownloadProgressDialogThreshold(thresholdMB * 1024 * 1024);
            SettingManager.setMaxHistoryCount(maxHistoryCount);
            SettingManager.setMaxOpenedRequestsCount(maxOpenedRequestsCount);
            SettingManager.setAutoFormatResponse(autoFormatResponseCheckBox.isSelected());
            SettingManager.setSidebarExpanded(sidebarExpandedCheckBox.isSelected());

            // 如果侧边栏设置改变了，提示用户重启
            if (sidebarExpandedChanged) {
                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SIDEBAR_RESTART_REQUIRED));
            } else {
                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS));
            }

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
