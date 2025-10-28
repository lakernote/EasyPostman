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
 * 性能设置面板 - JMeter相关配置
 */
public class PerformanceSettingsPanel extends JPanel {
    private JTextField jmeterMaxIdleField;
    private JTextField jmeterKeepAliveField;
    private JButton saveBtn;
    private JButton cancelBtn;

    private final Map<JTextField, Predicate<String>> validators = new HashMap<>();
    private final Map<JTextField, String> errorMessages = new HashMap<>();

    public PerformanceSettingsPanel() {
        initUI();
        registerListeners();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // JMeter设置面板
        JPanel jmeterPanel = createSectionPanel(I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_TITLE));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 最大空闲连接数
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel jmeterMaxIdleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_MAX_IDLE));
        jmeterMaxIdleLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_MAX_IDLE_TOOLTIP));
        jmeterPanel.add(jmeterMaxIdleLabel, gbc);

        gbc.gridx = 1;
        jmeterMaxIdleField = new JTextField(10);
        jmeterMaxIdleField.setText(String.valueOf(SettingManager.getJmeterMaxIdleConnections()));
        jmeterPanel.add(jmeterMaxIdleField, gbc);

        // 连接保活时间
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel jmeterKeepAliveLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_KEEP_ALIVE));
        jmeterKeepAliveLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_KEEP_ALIVE_TOOLTIP));
        jmeterPanel.add(jmeterKeepAliveLabel, gbc);

        gbc.gridx = 1;
        jmeterKeepAliveField = new JTextField(10);
        jmeterKeepAliveField.setText(String.valueOf(SettingManager.getJmeterKeepAliveSeconds()));
        jmeterPanel.add(jmeterKeepAliveField, gbc);

        mainPanel.add(jmeterPanel);
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

        setPreferredSize(new Dimension(500, 250));

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
        validators.put(jmeterMaxIdleField, s -> isInteger(s) && Integer.parseInt(s) > 0);
        validators.put(jmeterKeepAliveField, s -> isInteger(s) && Integer.parseInt(s) > 0);

        errorMessages.put(jmeterMaxIdleField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_IDLE_ERROR));
        errorMessages.put(jmeterKeepAliveField, I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_KEEP_ALIVE_ERROR));

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
                    Window window = SwingUtilities.getWindowAncestor(PerformanceSettingsPanel.this);
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
            }
        };

        jmeterMaxIdleField.addKeyListener(keyAdapter);
        jmeterKeepAliveField.addKeyListener(keyAdapter);
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
            int jmeterMaxIdle = Integer.parseInt(jmeterMaxIdleField.getText().trim());
            long jmeterKeepAlive = Long.parseLong(jmeterKeepAliveField.getText().trim());

            SettingManager.setJmeterMaxIdleConnections(jmeterMaxIdle);
            SettingManager.setJmeterKeepAliveSeconds(jmeterKeepAlive);

            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS));

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
