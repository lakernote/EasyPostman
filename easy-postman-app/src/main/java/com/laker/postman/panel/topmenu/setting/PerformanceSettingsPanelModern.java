package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.service.js.JsScriptExecutor;
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
    private JTextField jmeterMaxRequestsField;
    private JTextField jmeterMaxRequestsPerHostField;
    private JTextField jsContextPoolSizeField;
    private JTextField jsContextAcquireTimeoutField;
    private JTextField jmeterSlowRequestThresholdField;
    private JTextField responseBodyPreviewLimitField;
    private JTextField resultRowLimitField;
    private JTextField trendSamplingField;
    private JCheckBox eventLoggingCheckBox;

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

        // 最大并发请求数
        jmeterMaxRequestsField = new JTextField(10);
        jmeterMaxRequestsField.setText(String.valueOf(SettingManager.getJmeterMaxRequests()));
        JPanel maxRequestsRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_MAX_REQUESTS),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_MAX_REQUESTS_TOOLTIP),
                jmeterMaxRequestsField
        );
        jmeterSection.add(maxRequestsRow);
        jmeterSection.add(createVerticalSpace(FIELD_SPACING));

        // 单主机最大并发数
        jmeterMaxRequestsPerHostField = new JTextField(10);
        jmeterMaxRequestsPerHostField.setText(String.valueOf(SettingManager.getJmeterMaxRequestsPerHost()));
        JPanel maxRequestsPerHostRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_MAX_REQUESTS_PER_HOST),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_MAX_REQUESTS_PER_HOST_TOOLTIP),
                jmeterMaxRequestsPerHostField
        );
        jmeterSection.add(maxRequestsPerHostRow);
        jmeterSection.add(createVerticalSpace(FIELD_SPACING));

        // JS Context 池大小
        jsContextPoolSizeField = new JTextField(10);
        jsContextPoolSizeField.setText(String.valueOf(SettingManager.getJmeterJsContextPoolSize()));
        JPanel jsContextPoolSizeRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_JS_CONTEXT_POOL_SIZE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_JS_CONTEXT_POOL_SIZE_TOOLTIP),
                jsContextPoolSizeField
        );
        jmeterSection.add(jsContextPoolSizeRow);
        jmeterSection.add(createVerticalSpace(FIELD_SPACING));

        // JS Context 获取超时
        jsContextAcquireTimeoutField = new JTextField(10);
        jsContextAcquireTimeoutField.setText(String.valueOf(SettingManager.getJmeterJsContextAcquireTimeoutMs()));
        JPanel jsContextAcquireTimeoutRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_JS_CONTEXT_ACQUIRE_TIMEOUT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_JS_CONTEXT_ACQUIRE_TIMEOUT_TOOLTIP),
                jsContextAcquireTimeoutField
        );
        jmeterSection.add(jsContextAcquireTimeoutRow);
        jmeterSection.add(createVerticalSpace(FIELD_SPACING));

        // 慢请求阈值
        jmeterSlowRequestThresholdField = new JTextField(10);
        jmeterSlowRequestThresholdField.setText(String.valueOf(SettingManager.getJmeterSlowRequestThreshold()));
        JPanel slowRequestThresholdRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_SLOW_REQUEST_THRESHOLD),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_SLOW_REQUEST_THRESHOLD_TOOLTIP),
                jmeterSlowRequestThresholdField
        );
        jmeterSection.add(slowRequestThresholdRow);
        jmeterSection.add(createVerticalSpace(FIELD_SPACING));

        // 精简明细响应体预览上限
        responseBodyPreviewLimitField = new JTextField(10);
        responseBodyPreviewLimitField.setText(String.valueOf(SettingManager.getPerformanceResponseBodyPreviewLimitKb()));
        JPanel responseBodyPreviewLimitRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_RESPONSE_BODY_PREVIEW_LIMIT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_RESPONSE_BODY_PREVIEW_LIMIT_TOOLTIP),
                responseBodyPreviewLimitField
        );
        jmeterSection.add(responseBodyPreviewLimitRow);
        jmeterSection.add(createVerticalSpace(FIELD_SPACING));

        // 结果表保留行数上限
        resultRowLimitField = new JTextField(10);
        resultRowLimitField.setText(String.valueOf(SettingManager.getPerformanceResultRowLimit()));
        JPanel resultRowLimitRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_RESULT_ROW_LIMIT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_RESULT_ROW_LIMIT_TOOLTIP),
                resultRowLimitField
        );
        jmeterSection.add(resultRowLimitRow);
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
        jmeterSection.add(createVerticalSpace(FIELD_SPACING));

        // 事件日志开关
        eventLoggingCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_EVENT_LOGGING));
        eventLoggingCheckBox.setSelected(SettingManager.isPerformanceEventLoggingEnabled());
        JPanel eventLoggingRow = createCheckBoxRow(
                eventLoggingCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_EVENT_LOGGING_TOOLTIP)
        );
        jmeterSection.add(eventLoggingRow);

        contentPanel.add(jmeterSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        setupValidators();

        // 跟踪所有组件的初始值
        trackComponentValue(jmeterMaxIdleField);
        trackComponentValue(jmeterKeepAliveField);
        trackComponentValue(jmeterMaxRequestsField);
        trackComponentValue(jmeterMaxRequestsPerHostField);
        trackComponentValue(jsContextPoolSizeField);
        trackComponentValue(jsContextAcquireTimeoutField);
        trackComponentValue(jmeterSlowRequestThresholdField);
        trackComponentValue(responseBodyPreviewLimitField);
        trackComponentValue(resultRowLimitField);
        trackComponentValue(trendSamplingField);
        trackComponentValue(eventLoggingCheckBox);
    }

    private void setupValidators() {
        setupValidator(
                jmeterMaxIdleField,
                this::isGreaterThanZeroInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_IDLE_ERROR)
        );
        setupValidator(
                jmeterKeepAliveField,
                this::isGreaterThanZeroInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_KEEP_ALIVE_ERROR)
        );
        setupValidator(
                jmeterMaxRequestsField,
                this::isGreaterThanZeroInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_IDLE_ERROR)
        );
        setupValidator(
                jmeterMaxRequestsPerHostField,
                this::isGreaterThanZeroInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_IDLE_ERROR)
        );
        setupValidator(
                jsContextPoolSizeField,
                this::isGreaterThanZeroInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_JS_CONTEXT_POOL_SIZE_ERROR)
        );
        setupValidator(
                jsContextAcquireTimeoutField,
                this::isGreaterThanZeroInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_JS_CONTEXT_ACQUIRE_TIMEOUT_ERROR)
        );
        setupValidator(
                jmeterSlowRequestThresholdField,
                this::isPositiveInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_SLOW_REQUEST_THRESHOLD_ERROR)
        );
        setupValidator(
                responseBodyPreviewLimitField,
                this::isValidResponseBodyPreviewLimit,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_RESPONSE_BODY_PREVIEW_LIMIT_ERROR)
        );
        setupValidator(
                resultRowLimitField,
                this::isValidResultRowLimit,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_RESULT_ROW_LIMIT_ERROR)
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

    private boolean isValidResponseBodyPreviewLimit(String value) {
        try {
            int limitKb = Integer.parseInt(value.trim());
            return limitKb >= SettingManager.MIN_PERFORMANCE_RESPONSE_BODY_PREVIEW_LIMIT_KB
                    && limitKb <= SettingManager.MAX_PERFORMANCE_RESPONSE_BODY_PREVIEW_LIMIT_KB;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidResultRowLimit(String value) {
        try {
            int rowLimit = Integer.parseInt(value.trim());
            return rowLimit >= SettingManager.MIN_PERFORMANCE_RESULT_ROW_LIMIT
                    && rowLimit <= SettingManager.MAX_PERFORMANCE_RESULT_ROW_LIMIT;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isGreaterThanZeroInteger(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0;
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
            SettingManager.setJmeterMaxRequests(Integer.parseInt(jmeterMaxRequestsField.getText().trim()));
            SettingManager.setJmeterMaxRequestsPerHost(Integer.parseInt(jmeterMaxRequestsPerHostField.getText().trim()));
            SettingManager.setJmeterJsContextPoolSize(Integer.parseInt(jsContextPoolSizeField.getText().trim()));
            SettingManager.setJmeterJsContextAcquireTimeoutMs(Integer.parseInt(jsContextAcquireTimeoutField.getText().trim()));
            SettingManager.setJmeterSlowRequestThreshold(Integer.parseInt(jmeterSlowRequestThresholdField.getText().trim()));
            SettingManager.setPerformanceResponseBodyPreviewLimitKb(Integer.parseInt(responseBodyPreviewLimitField.getText().trim()));
            SettingManager.setPerformanceResultRowLimit(Integer.parseInt(resultRowLimitField.getText().trim()));
            SettingManager.setTrendSamplingIntervalSeconds(Integer.parseInt(trendSamplingField.getText().trim()));
            SettingManager.setPerformanceEventLoggingEnabled(eventLoggingCheckBox.isSelected());
            JsScriptExecutor.reconfigureContextPoolFromSettings();

            // 重新跟踪当前值
            originalValues.clear();
            trackComponentValue(jmeterMaxIdleField);
            trackComponentValue(jmeterKeepAliveField);
            trackComponentValue(jmeterMaxRequestsField);
            trackComponentValue(jmeterMaxRequestsPerHostField);
            trackComponentValue(jsContextPoolSizeField);
            trackComponentValue(jsContextAcquireTimeoutField);
            trackComponentValue(jmeterSlowRequestThresholdField);
            trackComponentValue(responseBodyPreviewLimitField);
            trackComponentValue(resultRowLimitField);
            trackComponentValue(trendSamplingField);
            trackComponentValue(eventLoggingCheckBox);
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
