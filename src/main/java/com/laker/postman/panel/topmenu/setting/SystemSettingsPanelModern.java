package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 现代化系统设置面板 - 自动更新等系统级配置
 */
public class SystemSettingsPanelModern extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;

    private JCheckBox autoUpdateCheckBox;
    private JTextField autoUpdateIntervalField;
    private JTextField autoUpdateStartupDelayField;
    private JComboBox<String> updateSourceComboBox;

    @Override
    protected void buildContent(JPanel contentPanel) {
        // 自动更新设置区域
        JPanel autoUpdateSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_TITLE),
                ""
        );

        // 自动更新开关
        autoUpdateCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED_CHECKBOX),
                SettingManager.isAutoUpdateCheckEnabled()
        );
        JPanel autoUpdateRow = createCheckBoxRow(
                autoUpdateCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED_TOOLTIP)
        );
        autoUpdateSection.add(autoUpdateRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // 更新间隔
        autoUpdateIntervalField = new JTextField(10);
        autoUpdateIntervalField.setText(String.valueOf(SettingManager.getAutoUpdateCheckIntervalHours()));
        JPanel intervalRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_INTERVAL),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_INTERVAL_TOOLTIP),
                autoUpdateIntervalField
        );
        autoUpdateSection.add(intervalRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // 启动时延迟
        autoUpdateStartupDelayField = new JTextField(10);
        autoUpdateStartupDelayField.setText(String.valueOf(SettingManager.getAutoUpdateStartupDelaySeconds()));
        JPanel startupDelayRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_STARTUP_DELAY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_STARTUP_DELAY_TOOLTIP),
                autoUpdateStartupDelayField
        );
        autoUpdateSection.add(startupDelayRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // 更新源选择
        String[] sourceOptions = {
                I18nUtil.getMessage(MessageKeys.SETTINGS_UPDATE_SOURCE_AUTO),
                I18nUtil.getMessage(MessageKeys.SETTINGS_UPDATE_SOURCE_GITHUB),
                I18nUtil.getMessage(MessageKeys.SETTINGS_UPDATE_SOURCE_GITEE)
        };
        updateSourceComboBox = new JComboBox<>(sourceOptions);

        // 根据当前设置选择对应的选项
        String currentPreference = SettingManager.getUpdateSourcePreference();
        switch (currentPreference) {
            case "github" -> updateSourceComboBox.setSelectedIndex(1);
            case "gitee" -> updateSourceComboBox.setSelectedIndex(2);
            default -> updateSourceComboBox.setSelectedIndex(0); // auto
        }

        JPanel sourceRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_UPDATE_SOURCE_PREFERENCE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_UPDATE_SOURCE_PREFERENCE_TOOLTIP),
                updateSourceComboBox
        );
        autoUpdateSection.add(sourceRow);

        contentPanel.add(autoUpdateSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        setupValidators();

        // 跟踪所有组件的初始值
        trackComponentValue(autoUpdateCheckBox);
        trackComponentValue(autoUpdateIntervalField);
        trackComponentValue(autoUpdateStartupDelayField);
        trackComponentValue(updateSourceComboBox);
    }

    private void setupValidators() {
        setupValidator(
                autoUpdateIntervalField,
                this::isPositiveInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_UPDATE_INTERVAL_ERROR)
        );
        setupValidator(
                autoUpdateStartupDelayField,
                s -> isInteger(s) && Integer.parseInt(s) >= 0,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_STARTUP_DELAY_ERROR)
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
            NotificationUtil.showError(
            I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }

        try {
            // 保存自动更新设置
            SettingManager.setAutoUpdateCheckEnabled(autoUpdateCheckBox.isSelected());
            SettingManager.setAutoUpdateCheckIntervalHours(Integer.parseInt(autoUpdateIntervalField.getText().trim()));
            SettingManager.setAutoUpdateStartupDelaySeconds(Integer.parseInt(autoUpdateStartupDelayField.getText().trim()));

            // 保存更新源设置
            String selectedSource = switch (updateSourceComboBox.getSelectedIndex()) {
                case 1 -> "github";
                case 2 -> "gitee";
                default -> "auto";
            };
            SettingManager.setUpdateSourcePreference(selectedSource);

            // 重新跟踪当前值
            originalValues.clear();
            trackComponentValue(autoUpdateCheckBox);
            trackComponentValue(autoUpdateIntervalField);
            trackComponentValue(autoUpdateStartupDelayField);
            trackComponentValue(updateSourceComboBox);
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
