package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;

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

    private JCheckBox autoUpdateCheckBox;
    private JComboBox<String> updateFrequencyComboBox;
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

        // 检查频率
        String[] frequencyOptions = {
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_STARTUP),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_DAILY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_WEEKLY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_MONTHLY)
        };
        updateFrequencyComboBox = new JComboBox<>(frequencyOptions);

        // 根据当前设置选择对应的选项
        String currentFrequency = SettingManager.getAutoUpdateCheckFrequency();
        switch (currentFrequency) {
            case "startup" -> updateFrequencyComboBox.setSelectedIndex(0);
            case "daily" -> updateFrequencyComboBox.setSelectedIndex(1);
            case "weekly" -> updateFrequencyComboBox.setSelectedIndex(2);
            case "monthly" -> updateFrequencyComboBox.setSelectedIndex(3);
            default -> updateFrequencyComboBox.setSelectedIndex(1); // daily
        }

        JPanel frequencyRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY_TOOLTIP),
                updateFrequencyComboBox
        );
        autoUpdateSection.add(frequencyRow);
        autoUpdateSection.add(createVerticalSpace(FIELD_SPACING));

        // 上次检查时间
        long lastCheckTime = SettingManager.getLastUpdateCheckTime();
        String lastCheckTimeStr = lastCheckTime > 0
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastCheckTime))
                : I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_NEVER_CHECKED);

        JLabel lastCheckTimeLabel = new JLabel(lastCheckTimeStr);
        JPanel lastCheckRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_LAST_CHECK_TIME),
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_LAST_CHECK_TIME_TOOLTIP),
                lastCheckTimeLabel
        );
        autoUpdateSection.add(lastCheckRow);
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
        trackComponentValue(autoUpdateCheckBox);
        trackComponentValue(updateFrequencyComboBox);
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
        try {
            // 保存自动更新设置
            SettingManager.setAutoUpdateCheckEnabled(autoUpdateCheckBox.isSelected());

            // 保存检查频率
            String selectedFrequency = switch (updateFrequencyComboBox.getSelectedIndex()) {
                case 0 -> "startup";
                case 1 -> "daily";
                case 2 -> "weekly";
                case 3 -> "monthly";
                default -> "daily";
            };
            SettingManager.setAutoUpdateCheckFrequency(selectedFrequency);

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
            trackComponentValue(updateFrequencyComboBox);
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