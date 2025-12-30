package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.NotificationPosition;
import com.laker.postman.panel.sidebar.SidebarTabPanel;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontManager;
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
    private JComboBox<String> fontNameComboBox;
    private JTextField fontSizeField;
    private JLabel fontPreviewLabel;
    private String systemDefaultFontName; // 保存系统默认字体名称

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

        // 字体设置区域
        JPanel fontSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_UI_TITLE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_RESTART_RECOMMENDED)
        );

        // 获取系统可用字体
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();

        // 创建字体选择下拉框，添加"系统默认"选项
        String[] fontOptions = new String[fontNames.length + 1];
        fontOptions[0] = I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_SYSTEM_DEFAULT);
        System.arraycopy(fontNames, 0, fontOptions, 1, fontNames.length);

        fontNameComboBox = new JComboBox<>(fontOptions);

        // 设置当前字体
        String currentFont = SettingManager.getUiFontName();
        if (currentFont.isEmpty()) {
            fontNameComboBox.setSelectedIndex(0); // 系统默认
        } else {
            // 在列表中查找并选中
            for (int i = 1; i < fontOptions.length; i++) {
                if (fontNames[i - 1].equals(currentFont)) {
                    fontNameComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel fontNameRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_NAME),
                I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_NAME_TOOLTIP),
                fontNameComboBox
        );
        fontSection.add(fontNameRow);
        fontSection.add(createVerticalSpace(FIELD_SPACING));

        // 字体大小
        fontSizeField = new JTextField(10);
        fontSizeField.setText(String.valueOf(SettingManager.getUiFontSize()));
        JPanel fontSizeRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_SIZE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_SIZE_TOOLTIP),
                fontSizeField
        );
        fontSection.add(fontSizeRow);
        fontSection.add(createVerticalSpace(FIELD_SPACING));

        // 字体预览
        fontPreviewLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_PREVIEW));
        fontPreviewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontPreviewLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        fontPreviewLabel.setOpaque(true);
        fontPreviewLabel.setBackground(Color.WHITE);
        // 保存系统默认字体名称
        systemDefaultFontName = fontPreviewLabel.getFont().getName();
        updateFontPreview();

        // 监听字体变化以更新预览
        fontNameComboBox.addActionListener(e -> updateFontPreview());
        fontSizeField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateFontPreview();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateFontPreview();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateFontPreview();
            }
        });

        fontSection.add(fontPreviewLabel);

        contentPanel.add(fontSection);
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
        trackComponentValue(fontNameComboBox);
        trackComponentValue(fontSizeField);
    }

    /**
     * 更新字体预览
     */
    private void updateFontPreview() {
        try {
            String selectedFont = (String) fontNameComboBox.getSelectedItem();
            if (selectedFont == null) return;

            // 如果选择的是"系统默认"，使用保存的系统默认字体名称
            String fontName;
            if (fontNameComboBox.getSelectedIndex() == 0) {
                fontName = systemDefaultFontName;
            } else {
                fontName = selectedFont;
            }

            int fontSize = 12; // 默认大小
            String sizeText = fontSizeField.getText().trim();
            if (!sizeText.isEmpty()) {
                try {
                    fontSize = Integer.parseInt(sizeText);
                    fontSize = Math.max(10, Math.min(24, fontSize));
                } catch (NumberFormatException e) {
                    // 使用默认大小
                }
            }

            fontPreviewLabel.setFont(new Font(fontName, Font.PLAIN, fontSize));
        } catch (Exception e) {
            // 忽略预览更新错误
        }
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
        setupValidator(
                fontSizeField,
                this::isValidFontSize,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_FONT_SIZE_ERROR)
        );
    }

    private boolean isValidFontSize(String value) {
        try {
            int size = Integer.parseInt(value.trim());
            return size >= 10 && size <= 24;
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
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }

        boolean fontChanged = false; // 声明在 try 块外，以便在 catch 后也能访问

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

            // 检测字体是否有变化（在保存前获取旧值）
            String oldFontName = SettingManager.getUiFontName();
            int oldFontSize = SettingManager.getUiFontSize();

            // 保存字体设置
            int fontNameIndex = fontNameComboBox.getSelectedIndex();
            String newFontName;
            if (fontNameIndex == 0) {
                // 系统默认
                newFontName = "";
                SettingManager.setUiFontName("");
            } else {
                newFontName = (String) fontNameComboBox.getSelectedItem();
                SettingManager.setUiFontName(newFontName);
            }
            int newFontSize = Integer.parseInt(fontSizeField.getText().trim());
            SettingManager.setUiFontSize(newFontSize);

            // 判断字体是否真的有变化（处理 null 情况）
            fontChanged = !java.util.Objects.equals(newFontName, oldFontName) || newFontSize != oldFontSize;

            // 如果字体有变化，立即应用字体设置到整个应用
            if (fontChanged) {
                FontManager.applyFont(newFontName, newFontSize);
            }

            // 重新跟踪当前值
            originalValues.clear();
            trackComponentValue(showDownloadProgressCheckBox);
            trackComponentValue(downloadProgressDialogThresholdField);
            trackComponentValue(maxHistoryCountField);
            trackComponentValue(maxOpenedRequestsCountField);
            trackComponentValue(autoFormatResponseCheckBox);
            trackComponentValue(sidebarExpandedCheckBox);
            trackComponentValue(notificationPositionComboBox);
            trackComponentValue(fontNameComboBox);
            trackComponentValue(fontSizeField);
            setHasUnsavedChanges(false);

            // 根据是否修改了字体显示不同的提示信息
            if (fontChanged) {
                NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_APPLIED));
            } else {
                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_MESSAGE));
            }

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