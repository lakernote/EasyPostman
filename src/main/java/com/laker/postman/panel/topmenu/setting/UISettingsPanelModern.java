package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.NotificationPosition;
import com.laker.postman.panel.sidebar.SidebarTabPanel;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 现代化界面设置面板 - 下载进度、历史记录等UI相关配置
 */
@Slf4j
public class UISettingsPanelModern extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;

    private JCheckBox showDownloadProgressCheckBox;
    private JTextField downloadProgressDialogThresholdField;
    private JTextField maxHistoryCountField;
    private JTextField maxOpenedRequestsCountField;
    private JCheckBox autoFormatResponseCheckBox;
    private JCheckBox sidebarExpandedCheckBox;
    private JComboBox<String> notificationPositionComboBox;
    private JLabel downloadProgressDialogThresholdLabel;

    @Override
    protected void buildContent(JPanel contentPanel) {
        // 下载设置区域
        JPanel downloadSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_TITLE),
                "" // 移除英文描述，保持简洁
        );

        // 显示下载进度对话框
        showDownloadProgressCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_SHOW_PROGRESS),
                SettingManager.isShowDownloadProgressDialog()
        );
        JPanel showProgressRow = createCheckBoxRow(
                showDownloadProgressCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_SHOW_PROGRESS_TOOLTIP)
        );
        downloadSection.add(showProgressRow);
        downloadSection.add(createVerticalSpace(FIELD_SPACING));

        // 下载阈值
        downloadProgressDialogThresholdField = new JTextField(10);
        int thresholdMB = SettingManager.getDownloadProgressDialogThreshold() / (1024 * 1024);
        downloadProgressDialogThresholdField.setText(String.valueOf(thresholdMB));

        JPanel thresholdRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_THRESHOLD),
                I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_THRESHOLD_TOOLTIP),
                downloadProgressDialogThresholdField
        );
        downloadSection.add(thresholdRow);

        // 设置阈值字段的启用状态
        downloadProgressDialogThresholdLabel = (JLabel) thresholdRow.getComponent(0);
        downloadProgressDialogThresholdField.setEnabled(showDownloadProgressCheckBox.isSelected());
        downloadProgressDialogThresholdLabel.setEnabled(showDownloadProgressCheckBox.isSelected());

        showDownloadProgressCheckBox.addItemListener(e -> {
            boolean selected = e.getStateChange() == java.awt.event.ItemEvent.SELECTED;
            downloadProgressDialogThresholdField.setEnabled(selected);
            downloadProgressDialogThresholdLabel.setEnabled(selected);
        });

        contentPanel.add(downloadSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        // 通用设置区域
        JPanel generalSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_TITLE),
                "" // 移除英文描述
        );

        // 历史记录数量
        maxHistoryCountField = new JTextField(10);
        maxHistoryCountField.setText(String.valueOf(SettingManager.getMaxHistoryCount()));
        JPanel historyRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_MAX_HISTORY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_MAX_HISTORY_TOOLTIP),
                maxHistoryCountField
        );
        generalSection.add(historyRow);
        generalSection.add(createVerticalSpace(FIELD_SPACING));

        // 最大打开请求数
        maxOpenedRequestsCountField = new JTextField(10);
        maxOpenedRequestsCountField.setText(String.valueOf(SettingManager.getMaxOpenedRequestsCount()));
        JPanel requestsRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_MAX_OPENED_REQUESTS),
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_MAX_OPENED_REQUESTS_TOOLTIP),
                maxOpenedRequestsCountField
        );
        generalSection.add(requestsRow);
        generalSection.add(createVerticalSpace(FIELD_SPACING));

        // 自动格式化响应体
        autoFormatResponseCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_AUTO_FORMAT_RESPONSE),
                SettingManager.isAutoFormatResponse()
        );
        JPanel formatRow = createCheckBoxRow(
                autoFormatResponseCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_AUTO_FORMAT_RESPONSE_TOOLTIP)
        );
        generalSection.add(formatRow);
        generalSection.add(createVerticalSpace(FIELD_SPACING));

        // 侧边栏展开设置
        sidebarExpandedCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_EXPANDED),
                SettingManager.isSidebarExpanded()
        );
        JPanel sidebarRow = createCheckBoxRow(
                sidebarExpandedCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_EXPANDED_TOOLTIP)
        );
        generalSection.add(sidebarRow);
        generalSection.add(createVerticalSpace(FIELD_SPACING));

        // 通知位置设置 - 使用枚举的 i18nKey
        NotificationPosition[] positions = NotificationPosition.values();
        String[] positionLabels = new String[positions.length];
        for (int i = 0; i < positions.length; i++) {
            positionLabels[i] = I18nUtil.getMessage(positions[i].getI18nKey());
        }
        notificationPositionComboBox = new JComboBox<>(positionLabels);

        // 设置当前值 - 直接从 SettingManager 获取枚举
        NotificationPosition currentPosition = SettingManager.getNotificationPosition();
        notificationPositionComboBox.setSelectedIndex(currentPosition.getIndex());

        JPanel notificationPositionRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION),
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_TOOLTIP),
                notificationPositionComboBox
        );
        generalSection.add(notificationPositionRow);

        contentPanel.add(generalSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        setupValidators();

        // 跟踪所有组件的初始值
        trackComponentValue(showDownloadProgressCheckBox);
        trackComponentValue(downloadProgressDialogThresholdField);
        trackComponentValue(maxHistoryCountField);
        trackComponentValue(maxOpenedRequestsCountField);
        trackComponentValue(autoFormatResponseCheckBox);
        trackComponentValue(sidebarExpandedCheckBox);
        trackComponentValue(notificationPositionComboBox);
    }

    private void setupValidators() {
        setupValidator(
                downloadProgressDialogThresholdField,
                s -> isInteger(s) && Integer.parseInt(s) >= 0,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_THRESHOLD_ERROR)
        );
        setupValidator(
                maxHistoryCountField,
                this::isPositiveInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_HISTORY_ERROR)
        );
        setupValidator(
                maxOpenedRequestsCountField,
                this::isPositiveInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_OPENED_REQUESTS_ERROR)
        );
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
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }

        try {
            // 保存下载设置
            SettingManager.setShowDownloadProgressDialog(showDownloadProgressCheckBox.isSelected());
            if (downloadProgressDialogThresholdField.isEnabled()) {
                int thresholdMB = Integer.parseInt(downloadProgressDialogThresholdField.getText().trim());
                SettingManager.setDownloadProgressDialogThreshold(thresholdMB * 1024 * 1024);
            }

            // 保存通用设置
            SettingManager.setMaxHistoryCount(Integer.parseInt(maxHistoryCountField.getText().trim()));
            SettingManager.setMaxOpenedRequestsCount(Integer.parseInt(maxOpenedRequestsCountField.getText().trim()));
            SettingManager.setAutoFormatResponse(autoFormatResponseCheckBox.isSelected());

            // 保存侧边栏展开设置并更新UI
            boolean oldSidebarExpanded = SettingManager.isSidebarExpanded();
            boolean newSidebarExpanded = sidebarExpandedCheckBox.isSelected();
            SettingManager.setSidebarExpanded(newSidebarExpanded);
            if (oldSidebarExpanded != newSidebarExpanded) {
                // 通知 SidebarTabPanel 更新展开状态
                updateSidebarExpansion();
            }

            // 保存通知位置设置并更新NotificationUtil - 使用枚举的 fromIndex 方法
            NotificationPosition selectedPosition = NotificationPosition.fromIndex(notificationPositionComboBox.getSelectedIndex());
            SettingManager.setNotificationPosition(selectedPosition);
            NotificationUtil.setDefaultPosition(selectedPosition);

            // 重新跟踪当前值
            originalValues.clear();
            trackComponentValue(showDownloadProgressCheckBox);
            trackComponentValue(downloadProgressDialogThresholdField);
            trackComponentValue(maxHistoryCountField);
            trackComponentValue(maxOpenedRequestsCountField);
            trackComponentValue(autoFormatResponseCheckBox);
            trackComponentValue(sidebarExpandedCheckBox);
            trackComponentValue(notificationPositionComboBox);
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

    /**
     * 更新侧边栏展开状态
     */
    private void updateSidebarExpansion() {
        try {
            // 获取 SidebarTabPanel 单例并更新
            SidebarTabPanel sidebarPanel = SingletonFactory.getInstance(SidebarTabPanel.class);
            sidebarPanel.updateSidebarExpansion();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}