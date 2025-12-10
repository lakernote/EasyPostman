package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 现代化性能设置面板 - JMeter相关配置
 */
public class PerformanceSettingsPanelModern extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;

    private JTextField jmeterMaxIdleField;
    private JTextField jmeterKeepAliveField;
    private JTextField trendSamplingField;

    @Override
    protected void buildContent(JPanel contentPanel) {
        // JMeter设置区域
        JPanel jmeterSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_TITLE),
                ""
        );

        // 最大空闲连接数
        jmeterMaxIdleField = new JTextField(10);
        jmeterMaxIdleField.setText(String.valueOf(SettingManager.getJmeterMaxIdleConnections()));
        JPanel maxIdleRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_MAX_IDLE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_MAX_IDLE_TOOLTIP),
                jmeterMaxIdleField
        );
        jmeterSection.add(maxIdleRow);
        jmeterSection.add(createVerticalSpace(FIELD_SPACING));

        // 连接保活时间
        jmeterKeepAliveField = new JTextField(10);
        jmeterKeepAliveField.setText(String.valueOf(SettingManager.getJmeterKeepAliveSeconds()));
        JPanel keepAliveRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_KEEP_ALIVE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_KEEP_ALIVE_TOOLTIP),
                jmeterKeepAliveField
        );
        jmeterSection.add(keepAliveRow);
        jmeterSection.add(createVerticalSpace(FIELD_SPACING));

        // 趋势图采样间隔
        trendSamplingField = new JTextField(10);
        trendSamplingField.setText(String.valueOf(SettingManager.getTrendSamplingIntervalSeconds()));
        JPanel trendSamplingRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_TREND_SAMPLING),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_TREND_SAMPLING_TOOLTIP),
                trendSamplingField
        );
        jmeterSection.add(trendSamplingRow);

        contentPanel.add(jmeterSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        setupValidators();

        // 跟踪所有组件的初始值
        trackComponentValue(jmeterMaxIdleField);
        trackComponentValue(jmeterKeepAliveField);
        trackComponentValue(trendSamplingField);
    }

    private void setupValidators() {
        setupValidator(
                jmeterMaxIdleField,
                this::isPositiveInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_IDLE_ERROR)
        );
        setupValidator(
                jmeterKeepAliveField,
                this::isPositiveInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_KEEP_ALIVE_ERROR)
        );
        setupValidator(
                trendSamplingField,
                this::isValidTrendSamplingInterval,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_TREND_SAMPLING_ERROR)
        );
    }

    private boolean isValidTrendSamplingInterval(String value) {
        try {
            int interval = Integer.parseInt(value.trim());
            return interval >= 1 && interval <= 60;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected void registerListeners() {
        saveBtn.addActionListener(e -> saveSettings(true));
        applyBtn.addActionListener(e -> saveSettings(false));
        cancelBtn.addActionListener(e -> {
            if (confirmDiscardChanges()) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
        });

        // 键盘快捷键
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("control S"), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveSettings(false);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        actionMap.put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cancelBtn.doClick();
            }
        });
    }

    private void saveSettings(boolean closeAfterSave) {
        // 验证所有字段
        if (!validateAllFields()) {
            NotificationUtil.showError(
            I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }

        try {
            // 保存JMeter设置
            SettingManager.setJmeterMaxIdleConnections(Integer.parseInt(jmeterMaxIdleField.getText().trim()));
            SettingManager.setJmeterKeepAliveSeconds(Integer.parseInt(jmeterKeepAliveField.getText().trim()));
            SettingManager.setTrendSamplingIntervalSeconds(Integer.parseInt(trendSamplingField.getText().trim()));

            // 重新跟踪当前值
            originalValues.clear();
            trackComponentValue(jmeterMaxIdleField);
            trackComponentValue(jmeterKeepAliveField);
            trackComponentValue(trendSamplingField);
            setHasUnsavedChanges(false);

            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_MESSAGE));

            // 根据参数决定是否关闭对话框
            if (closeAfterSave) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
        } catch (Exception ex) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_ERROR_MESSAGE) + ": " + ex.getMessage());
        }
    }
}
