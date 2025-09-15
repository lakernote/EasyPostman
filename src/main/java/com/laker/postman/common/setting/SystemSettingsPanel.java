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
 * 系统设置面板 - 自动更新等系统级配置
 */
public class SystemSettingsPanel extends JPanel {
    private JCheckBox autoUpdateCheckBox;
    private JTextField autoUpdateIntervalField;
    private JTextField autoUpdateStartupDelayField;
    private JButton saveBtn;
    private JButton cancelBtn;

    private final Map<JTextField, Predicate<String>> validators = new HashMap<>();
    private final Map<JTextField, String> errorMessages = new HashMap<>();

    public SystemSettingsPanel() {
        initUI();
        registerListeners();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 自动更新设置面板
        JPanel autoUpdatePanel = createSectionPanel(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_TITLE));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 自动更新
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel autoUpdateLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED));
        autoUpdateLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED_TOOLTIP));
        autoUpdatePanel.add(autoUpdateLabel, gbc);

        gbc.gridx = 1;
        autoUpdateCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED_CHECKBOX), SettingManager.isAutoUpdateCheckEnabled());
        autoUpdatePanel.add(autoUpdateCheckBox, gbc);

        // 更新间隔
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel autoUpdateIntervalLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_INTERVAL));
        autoUpdateIntervalLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_INTERVAL_TOOLTIP));
        autoUpdatePanel.add(autoUpdateIntervalLabel, gbc);

        gbc.gridx = 1;
        autoUpdateIntervalField = new JTextField(10);
        autoUpdateIntervalField.setText(String.valueOf(SettingManager.getAutoUpdateCheckIntervalHours()));
        autoUpdatePanel.add(autoUpdateIntervalField, gbc);

        // 启动时延迟
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel autoUpdateStartupDelayLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_STARTUP_DELAY));
        autoUpdateStartupDelayLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_STARTUP_DELAY_TOOLTIP));
        autoUpdatePanel.add(autoUpdateStartupDelayLabel, gbc);

        gbc.gridx = 1;
        autoUpdateStartupDelayField = new JTextField(10);
        autoUpdateStartupDelayField.setText(String.valueOf(SettingManager.getAutoUpdateStartupDelaySeconds()));
        autoUpdatePanel.add(autoUpdateStartupDelayField, gbc);

        mainPanel.add(autoUpdatePanel);
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

        setPreferredSize(new Dimension(500, 280));

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
        validators.put(autoUpdateIntervalField, s -> isInteger(s) && Integer.parseInt(s) > 0);
        validators.put(autoUpdateStartupDelayField, s -> isInteger(s) && Integer.parseInt(s) >= 0);

        errorMessages.put(autoUpdateIntervalField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_AUTO_UPDATE_INTERVAL_ERROR));
        errorMessages.put(autoUpdateStartupDelayField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_AUTO_UPDATE_STARTUP_DELAY_ERROR));

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
                    Window window = SwingUtilities.getWindowAncestor(SystemSettingsPanel.this);
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
            }
        };

        autoUpdateCheckBox.addKeyListener(keyAdapter);
        autoUpdateIntervalField.addKeyListener(keyAdapter);
        autoUpdateStartupDelayField.addKeyListener(keyAdapter);
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
            int autoUpdateInterval = Integer.parseInt(autoUpdateIntervalField.getText().trim());
            int autoUpdateStartupDelay = Integer.parseInt(autoUpdateStartupDelayField.getText().trim());

            SettingManager.setAutoUpdateCheckEnabled(autoUpdateCheckBox.isSelected());
            SettingManager.setAutoUpdateCheckIntervalHours(autoUpdateInterval);
            SettingManager.setAutoUpdateStartupDelaySeconds(autoUpdateStartupDelay);

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
