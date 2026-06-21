package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.platform.update.UpdateCenter;
import com.laker.postman.platform.update.model.UpdatePolicy;
import com.laker.postman.platform.update.model.UpdateTarget;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.service.update.AppUpdateCenter;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 自动更新设置面板
 */
public class AutoUpdateSettingsPanel extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;

    private JCheckBox appUpdateCheckBox;
    private JComboBox<String> appUpdateFrequencyComboBox;
    private JCheckBox pluginUpdateCheckBox;
    private JComboBox<String> pluginUpdateFrequencyComboBox;
    private JComboBox<String> updateSourceComboBox;
    private final UpdateCenter updateCenter = AppUpdateCenter.get();

    @Override
    protected void buildContent(JPanel contentPanel) {
        // 自动更新设置区域
        JPanel autoUpdateSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_TITLE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_DESCRIPTION)
        );

        // 主程序更新开关
        UpdatePolicy appPolicy = updateCenter.policy(UpdateTarget.APP);
        appUpdateCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED_CHECKBOX),
                appPolicy.enabled()
        );
        JPanel appUpdateRow = createCheckBoxRow(
                appUpdateCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED_TOOLTIP)
        );
        autoUpdateSection.add(appUpdateRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // 检查频率
        String[] frequencyOptions = createFrequencyOptions();
        appUpdateFrequencyComboBox = new JComboBox<>(frequencyOptions);

        // 根据当前设置选择对应的选项
        selectFrequency(appUpdateFrequencyComboBox, appPolicy.frequency().getCode());

        JPanel frequencyRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_TOOLTIP),
                appUpdateFrequencyComboBox
        );
        autoUpdateSection.add(frequencyRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // 上次检查时间
        JLabel lastCheckTimeLabel = new JLabel(formatCheckTime(
                updateCenter.state(UpdateTarget.APP).lastCheckTimeMillis()
        ));
        JPanel lastCheckRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_LAST_CHECK_TIME),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_LAST_CHECK_TIME_TOOLTIP),
                lastCheckTimeLabel
        );
        autoUpdateSection.add(lastCheckRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // 插件更新开关
        UpdatePolicy pluginPolicy = updateCenter.policy(UpdateTarget.PLUGIN);
        pluginUpdateCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_ENABLED_CHECKBOX),
                pluginPolicy.enabled()
        );
        JPanel pluginUpdateRow = createCheckBoxRow(
                pluginUpdateCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_ENABLED_TOOLTIP)
        );
        autoUpdateSection.add(pluginUpdateRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        pluginUpdateFrequencyComboBox = new JComboBox<>(frequencyOptions);
        selectFrequency(pluginUpdateFrequencyComboBox, pluginPolicy.frequency().getCode());
        JPanel pluginFrequencyRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_FREQUENCY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_FREQUENCY_TOOLTIP),
                pluginUpdateFrequencyComboBox
        );
        autoUpdateSection.add(pluginFrequencyRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        JLabel pluginLastCheckTimeLabel = new JLabel(formatCheckTime(
                updateCenter.state(UpdateTarget.PLUGIN).lastCheckTimeMillis()
        ));
        JPanel pluginLastCheckRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_LAST_CHECK_TIME),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_LAST_CHECK_TIME_TOOLTIP),
                pluginLastCheckTimeLabel
        );
        autoUpdateSection.add(pluginLastCheckRow);
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

        // 跟踪所有组件的初始值
        trackComponentValue(appUpdateCheckBox);
        trackComponentValue(appUpdateFrequencyComboBox);
        trackComponentValue(pluginUpdateCheckBox);
        trackComponentValue(pluginUpdateFrequencyComboBox);
        trackComponentValue(updateSourceComboBox);
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
        try {
            // 保存自动更新设置
            SettingManager.setAutoUpdateCheckEnabled(appUpdateCheckBox.isSelected());

            // 保存检查频率
            SettingManager.setAutoUpdateCheckFrequency(selectedFrequency(appUpdateFrequencyComboBox));
            SettingManager.setPluginUpdateCheckEnabled(pluginUpdateCheckBox.isSelected());
            SettingManager.setPluginUpdateCheckFrequency(selectedFrequency(pluginUpdateFrequencyComboBox));

            // 保存更新源设置
            String selectedSource = switch (updateSourceComboBox.getSelectedIndex()) {
                case 1 -> "github";
                case 2 -> "gitee";
                default -> "auto";
            };
            SettingManager.setUpdateSourcePreference(selectedSource);

            // 重新跟踪当前值
            originalValues.clear();
            trackComponentValue(appUpdateCheckBox);
            trackComponentValue(appUpdateFrequencyComboBox);
            trackComponentValue(pluginUpdateCheckBox);
            trackComponentValue(pluginUpdateFrequencyComboBox);
            trackComponentValue(updateSourceComboBox);
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

    private String[] createFrequencyOptions() {
        return new String[]{
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_STARTUP),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_DAILY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_WEEKLY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_MONTHLY)
        };
    }

    private void selectFrequency(JComboBox<String> comboBox, String frequency) {
        switch (frequency) {
            case "startup" -> comboBox.setSelectedIndex(0);
            case "weekly" -> comboBox.setSelectedIndex(2);
            case "monthly" -> comboBox.setSelectedIndex(3);
            default -> comboBox.setSelectedIndex(1);
        }
    }

    private String selectedFrequency(JComboBox<String> comboBox) {
        return switch (comboBox.getSelectedIndex()) {
            case 0 -> "startup";
            case 2 -> "weekly";
            case 3 -> "monthly";
            default -> "daily";
        };
    }

    private String formatCheckTime(long timestamp) {
        return timestamp > 0
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp))
                : I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_NEVER_CHECKED);
    }
}
