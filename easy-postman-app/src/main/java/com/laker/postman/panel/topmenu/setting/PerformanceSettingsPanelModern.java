package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;

import javax.swing.*;
import java.awt.*;

/**
 * 现代化性能设置面板 - 性能相关配置
 */
public class PerformanceSettingsPanelModern extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;

    private JTextField performanceMaxIdleField;
    private JTextField performanceKeepAliveField;
    private JTextField performanceMaxRequestsField;
    private JTextField performanceMaxRequestsPerHostField;
    private JTextField jsContextPoolSizeField;
    private JTextField jsContextAcquireTimeoutField;
    private JTextField performanceSlowRequestThresholdField;
    private JTextField responseBodyPreviewLimitField;
    private JTextField resultRowLimitField;
    private JTextField trendSamplingField;
    private JCheckBox eventLoggingCheckBox;

    @Override
    protected void buildContent(JPanel contentPanel) {
        // 性能设置区域
        JPanel performanceSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_TITLE),
                ""
        );

        // 最大空闲连接数
        performanceMaxIdleField = new JTextField(10);
        performanceMaxIdleField.setText(String.valueOf(SettingManager.getPerformanceMaxIdleConnections()));
        JPanel maxIdleRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_MAX_IDLE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_MAX_IDLE_TOOLTIP),
                performanceMaxIdleField
        );
        performanceSection.add(maxIdleRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // 连接保活时间
        performanceKeepAliveField = new JTextField(10);
        performanceKeepAliveField.setText(String.valueOf(SettingManager.getPerformanceKeepAliveSeconds()));
        JPanel keepAliveRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_KEEP_ALIVE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_KEEP_ALIVE_TOOLTIP),
                performanceKeepAliveField
        );
        performanceSection.add(keepAliveRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // 最大并发请求数
        performanceMaxRequestsField = new JTextField(10);
        performanceMaxRequestsField.setText(String.valueOf(SettingManager.getPerformanceMaxRequests()));
        JPanel maxRequestsRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_MAX_REQUESTS),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_MAX_REQUESTS_TOOLTIP),
                performanceMaxRequestsField
        );
        performanceSection.add(maxRequestsRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // 单主机最大并发数
        performanceMaxRequestsPerHostField = new JTextField(10);
        performanceMaxRequestsPerHostField.setText(String.valueOf(SettingManager.getPerformanceMaxRequestsPerHost()));
        JPanel maxRequestsPerHostRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_MAX_REQUESTS_PER_HOST),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_MAX_REQUESTS_PER_HOST_TOOLTIP),
                performanceMaxRequestsPerHostField
        );
        performanceSection.add(maxRequestsPerHostRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // JS Context 池大小
        jsContextPoolSizeField = new JTextField(10);
        jsContextPoolSizeField.setText(String.valueOf(SettingManager.getPerformanceJsContextPoolSize()));
        JPanel jsContextPoolSizeRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_JS_CONTEXT_POOL_SIZE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_JS_CONTEXT_POOL_SIZE_TOOLTIP),
                jsContextPoolSizeField
        );
        performanceSection.add(jsContextPoolSizeRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // JS Context 获取超时
        jsContextAcquireTimeoutField = new JTextField(10);
        jsContextAcquireTimeoutField.setText(String.valueOf(SettingManager.getPerformanceJsContextAcquireTimeoutMs()));
        JPanel jsContextAcquireTimeoutRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_JS_CONTEXT_ACQUIRE_TIMEOUT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_JS_CONTEXT_ACQUIRE_TIMEOUT_TOOLTIP),
                jsContextAcquireTimeoutField
        );
        performanceSection.add(jsContextAcquireTimeoutRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // 慢请求阈值
        performanceSlowRequestThresholdField = new JTextField(10);
        performanceSlowRequestThresholdField.setText(String.valueOf(SettingManager.getPerformanceSlowRequestThreshold()));
        JPanel slowRequestThresholdRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_SLOW_REQUEST_THRESHOLD),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_SLOW_REQUEST_THRESHOLD_TOOLTIP),
                performanceSlowRequestThresholdField
        );
        performanceSection.add(slowRequestThresholdRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // 精简明细响应体预览上限
        responseBodyPreviewLimitField = new JTextField(10);
        responseBodyPreviewLimitField.setText(String.valueOf(SettingManager.getPerformanceResponseBodyPreviewLimitKb()));
        JPanel responseBodyPreviewLimitRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_RESPONSE_BODY_PREVIEW_LIMIT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_RESPONSE_BODY_PREVIEW_LIMIT_TOOLTIP),
                responseBodyPreviewLimitField
        );
        performanceSection.add(responseBodyPreviewLimitRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // 结果表保留行数上限
        resultRowLimitField = new JTextField(10);
        resultRowLimitField.setText(String.valueOf(SettingManager.getPerformanceResultRowLimit()));
        JPanel resultRowLimitRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_RESULT_ROW_LIMIT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_RESULT_ROW_LIMIT_TOOLTIP),
                resultRowLimitField
        );
        performanceSection.add(resultRowLimitRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // 趋势图采样间隔
        trendSamplingField = new JTextField(10);
        trendSamplingField.setText(String.valueOf(SettingManager.getTrendSamplingIntervalSeconds()));
        JPanel trendSamplingRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_TREND_SAMPLING),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_TREND_SAMPLING_TOOLTIP),
                trendSamplingField
        );
        performanceSection.add(trendSamplingRow);
        performanceSection.add(createVerticalSpace(FIELD_SPACING));

        // 事件日志开关
        eventLoggingCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_EVENT_LOGGING));
        eventLoggingCheckBox.setSelected(SettingManager.isPerformanceEventLoggingEnabled());
        JPanel eventLoggingRow = createCheckBoxRow(
                eventLoggingCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_PERFORMANCE_EVENT_LOGGING_TOOLTIP)
        );
        performanceSection.add(eventLoggingRow);

        contentPanel.add(performanceSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        setupValidators();

        // 跟踪所有组件的初始值
        trackComponentValue(performanceMaxIdleField);
        trackComponentValue(performanceKeepAliveField);
        trackComponentValue(performanceMaxRequestsField);
        trackComponentValue(performanceMaxRequestsPerHostField);
        trackComponentValue(jsContextPoolSizeField);
        trackComponentValue(jsContextAcquireTimeoutField);
        trackComponentValue(performanceSlowRequestThresholdField);
        trackComponentValue(responseBodyPreviewLimitField);
        trackComponentValue(resultRowLimitField);
        trackComponentValue(trendSamplingField);
        trackComponentValue(eventLoggingCheckBox);
    }

    private void setupValidators() {
        setupValidator(
                performanceMaxIdleField,
                this::isGreaterThanZeroInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_IDLE_ERROR)
        );
        setupValidator(
                performanceKeepAliveField,
                this::isGreaterThanZeroInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_KEEP_ALIVE_ERROR)
        );
        setupValidator(
                performanceMaxRequestsField,
                this::isGreaterThanZeroInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_IDLE_ERROR)
        );
        setupValidator(
                performanceMaxRequestsPerHostField,
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
                performanceSlowRequestThresholdField,
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
        registerSaveShortcut(() -> saveSettings(false));
    }

    private void saveSettings(boolean closeAfterSave) {
        // 验证所有字段
        if (!validateAllFields()) {
            NotificationCenter.showError(
                    I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }

        try {
            // 保存性能设置
            SettingManager.setPerformanceMaxIdleConnections(Integer.parseInt(performanceMaxIdleField.getText().trim()));
            SettingManager.setPerformanceKeepAliveSeconds(Integer.parseInt(performanceKeepAliveField.getText().trim()));
            SettingManager.setPerformanceMaxRequests(Integer.parseInt(performanceMaxRequestsField.getText().trim()));
            SettingManager.setPerformanceMaxRequestsPerHost(Integer.parseInt(performanceMaxRequestsPerHostField.getText().trim()));
            SettingManager.setPerformanceJsContextPoolSize(Integer.parseInt(jsContextPoolSizeField.getText().trim()));
            SettingManager.setPerformanceJsContextAcquireTimeoutMs(Integer.parseInt(jsContextAcquireTimeoutField.getText().trim()));
            SettingManager.setPerformanceSlowRequestThreshold(Integer.parseInt(performanceSlowRequestThresholdField.getText().trim()));
            SettingManager.setPerformanceResponseBodyPreviewLimitKb(Integer.parseInt(responseBodyPreviewLimitField.getText().trim()));
            SettingManager.setPerformanceResultRowLimit(Integer.parseInt(resultRowLimitField.getText().trim()));
            SettingManager.setTrendSamplingIntervalSeconds(Integer.parseInt(trendSamplingField.getText().trim()));
            SettingManager.setPerformanceEventLoggingEnabled(eventLoggingCheckBox.isSelected());
            JsScriptExecutor.reconfigureContextPoolFromSettings();

            // 重新跟踪当前值
            originalValues.clear();
            trackComponentValue(performanceMaxIdleField);
            trackComponentValue(performanceKeepAliveField);
            trackComponentValue(performanceMaxRequestsField);
            trackComponentValue(performanceMaxRequestsPerHostField);
            trackComponentValue(jsContextPoolSizeField);
            trackComponentValue(jsContextAcquireTimeoutField);
            trackComponentValue(performanceSlowRequestThresholdField);
            trackComponentValue(responseBodyPreviewLimitField);
            trackComponentValue(resultRowLimitField);
            trackComponentValue(trendSamplingField);
            trackComponentValue(eventLoggingCheckBox);
            setHasUnsavedChanges(false);

            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_MESSAGE));

            // 根据参数决定是否关闭对话框
            if (closeAfterSave) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
        } catch (Exception ex) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_ERROR_MESSAGE) + ": " + ex.getMessage());
        }
    }
}
