package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.NotificationPosition;
import com.laker.postman.model.SidebarTab;
import com.laker.postman.panel.sidebar.SidebarTabPanel;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import com.laker.postman.util.UiFontCatalog;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    private JCheckBox startupSplashCheckBox;
    private JCheckBox sidebarExpandedCheckBox;
    private JComboBox<String> notificationPositionComboBox;
    private JLabel downloadProgressDialogThresholdLabel;
    private JComboBox<String> fontNameComboBox;
    private JTextField fontSizeField;
    private JLabel fontPreviewLabel;
    private JLabel fontSupportHintLabel;
    private DefaultListModel<SidebarTabSettingItem> sidebarTabListModel;
    private JList<SidebarTabSettingItem> sidebarTabList;
    private JTextField sidebarTabsStateField;
    private List<UiFontCatalog.FontOption> availableFontOptions = List.of();
    private boolean fontOptionsLoaded;
    private boolean fontOptionsLoading;

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

        startupSplashCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_STARTUP_SPLASH),
                SettingManager.isStartupSplashEnabled()
        );
        JPanel startupSplashRow = createCheckBoxRow(
                startupSplashCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_STARTUP_SPLASH_TOOLTIP)
        );
        generalSection.add(startupSplashRow);
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

        JPanel sidebarTabsRow = createSidebarTabsRow();
        generalSection.add(sidebarTabsRow);
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

        // 创建字体选择下拉框，添加"系统默认"选项
        fontNameComboBox = new JComboBox<>();
        fontNameComboBox.setMaximumRowCount(20);
        fontNameComboBox.setRenderer(new FontFamilyListCellRenderer());

        // 设置当前字体
        String currentFont = SettingManager.getUiFontName();
        resetFontComboItems(currentFont);
        if (currentFont.isEmpty()) {
            fontNameComboBox.setSelectedIndex(0); // 系统默认
        } else {
            fontNameComboBox.setSelectedItem(currentFont);
        }
        preloadFontOptionsInBackground();
        fontNameComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                loadFontOptionsIfNeeded();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

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
                BorderFactory.createLineBorder(getBorderMediumColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        fontPreviewLabel.setOpaque(true);
        fontPreviewLabel.setBackground(getInputBackgroundColor());
        fontPreviewLabel.setForeground(getTextPrimaryColor());

        fontSupportHintLabel = new JLabel();
        fontSupportHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSupportHintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));

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
        fontSection.add(createVerticalSpace(FIELD_SPACING));
        fontSection.add(fontSupportHintLabel);

        contentPanel.add(fontSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        setupValidators();

        // 跟踪所有组件的初始值
        trackComponentValue(showDownloadProgressCheckBox);
        trackComponentValue(downloadProgressDialogThresholdField);
        trackComponentValue(maxHistoryCountField);
        trackComponentValue(maxOpenedRequestsCountField);
        trackComponentValue(autoFormatResponseCheckBox);
        trackComponentValue(startupSplashCheckBox);
        trackComponentValue(sidebarExpandedCheckBox);
        trackComponentValue(notificationPositionComboBox);
        trackComponentValue(fontNameComboBox);
        trackComponentValue(fontSizeField);
        trackComponentValue(sidebarTabsStateField);
    }

    private JPanel createSidebarTabsRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(getCardBackgroundColor());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_TABS));
        label.setFont(com.laker.postman.util.FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(getTextPrimaryColor());
        label.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_TABS_TOOLTIP));
        label.setPreferredSize(new Dimension(220, 32));
        label.setMinimumSize(new Dimension(220, 32));
        label.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));
        label.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel editor = createSidebarTabsEditor();
        editor.setAlignmentY(Component.TOP_ALIGNMENT);

        row.add(label);
        row.add(Box.createHorizontalStrut(16));
        row.add(editor);
        row.add(Box.createHorizontalGlue());

        return row;
    }

    private JPanel createSidebarTabsEditor() {
        JPanel editor = new JPanel();
        editor.setLayout(new BoxLayout(editor, BoxLayout.Y_AXIS));
        editor.setBackground(getCardBackgroundColor());
        editor.setAlignmentX(Component.LEFT_ALIGNMENT);
        editor.setMaximumSize(new Dimension(340, 220));

        sidebarTabListModel = new DefaultListModel<>();
        Set<String> hiddenTabs = SettingManager.getHiddenSidebarTabs();
        for (SidebarTab tab : SettingManager.getOrderedSidebarTabs()) {
            sidebarTabListModel.addElement(new SidebarTabSettingItem(tab, !hiddenTabs.contains(tab.name())));
        }

        sidebarTabList = new JList<>(sidebarTabListModel);
        sidebarTabList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sidebarTabList.setCellRenderer(new SidebarTabListCellRenderer());
        sidebarTabList.setDragEnabled(true);
        sidebarTabList.setDropMode(DropMode.INSERT);
        sidebarTabList.setTransferHandler(new SidebarTabListTransferHandler());
        sidebarTabList.setFixedCellHeight(38);
        sidebarTabList.setVisibleRowCount(Math.min(sidebarTabListModel.size(), 7));
        sidebarTabList.setOpaque(false);
        sidebarTabList.setSelectionBackground(getHoverBackgroundColor());
        sidebarTabList.setSelectionForeground(getTextPrimaryColor());
        sidebarTabList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = sidebarTabList.locationToIndex(e.getPoint());
                if (index < 0) {
                    return;
                }
                Rectangle bounds = sidebarTabList.getCellBounds(index, index);
                if (bounds != null && e.getX() - bounds.x <= 28) {
                    toggleSidebarTabVisibility(index);
                }
            }
        });
        sidebarTabList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleSidebarTabVisibility");
        sidebarTabList.getActionMap().put("toggleSidebarTabVisibility", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int selectedIndex = sidebarTabList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    toggleSidebarTabVisibility(selectedIndex);
                }
            }
        });
        if (!sidebarTabListModel.isEmpty()) {
            sidebarTabList.setSelectedIndex(0);
        }

        JScrollPane scrollPane = new JScrollPane(sidebarTabList);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getBorderMediumColor(), 1),
                new EmptyBorder(4, 4, 4, 4)
        ));
        scrollPane.setBackground(getInputBackgroundColor());
        scrollPane.getViewport().setBackground(getInputBackgroundColor());
        scrollPane.setPreferredSize(new Dimension(320, 160));
        scrollPane.setMaximumSize(new Dimension(320, 160));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        JButton resetButton = createModernButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_TABS_RESET),
                false
        );
        resetButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetButton.setPreferredSize(new Dimension(116, 32));
        resetButton.setMaximumSize(new Dimension(116, 32));
        resetButton.addActionListener(e -> resetSidebarTabsToDefault());

        JLabel hintLabel = new JLabel("<html>" + I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_TABS_HINT) + "</html>");
        hintLabel.setFont(com.laker.postman.util.FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        hintLabel.setForeground(getTextSecondaryColor());
        hintLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebarTabsStateField = new JTextField(buildSidebarTabsStateSnapshot());

        editor.add(scrollPane);
        editor.add(Box.createVerticalStrut(8));
        editor.add(resetButton);
        editor.add(hintLabel);
        return editor;
    }

    private void resetSidebarTabsToDefault() {
        sidebarTabListModel.clear();
        for (SidebarTab tab : SidebarTab.values()) {
            sidebarTabListModel.addElement(new SidebarTabSettingItem(tab, true));
        }
        if (!sidebarTabListModel.isEmpty()) {
            sidebarTabList.setSelectedIndex(0);
        }
        sidebarTabList.repaint();
        syncSidebarTabsState();
    }

    private void toggleSidebarTabVisibility(int index) {
        SidebarTabSettingItem item = sidebarTabListModel.get(index);
        if (item.visible && countVisibleSidebarTabs() == 1) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_TABS_AT_LEAST_ONE));
            return;
        }
        item.visible = !item.visible;
        sidebarTabList.repaint();
        syncSidebarTabsState();
    }

    private int countVisibleSidebarTabs() {
        int visibleCount = 0;
        for (int i = 0; i < sidebarTabListModel.size(); i++) {
            if (sidebarTabListModel.get(i).visible) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    private void syncSidebarTabsState() {
        if (sidebarTabsStateField != null) {
            sidebarTabsStateField.setText(buildSidebarTabsStateSnapshot());
        }
    }

    private String buildSidebarTabsStateSnapshot() {
        List<String> state = new ArrayList<>();
        for (int i = 0; i < sidebarTabListModel.size(); i++) {
            SidebarTabSettingItem item = sidebarTabListModel.get(i);
            state.add(item.tab.name() + ":" + (item.visible ? "1" : "0"));
        }
        return String.join(",", state);
    }

    private List<String> getSidebarTabOrderForSave() {
        List<String> order = new ArrayList<>();
        for (int i = 0; i < sidebarTabListModel.size(); i++) {
            order.add(sidebarTabListModel.get(i).tab.name());
        }
        return order;
    }

    private Set<String> getHiddenSidebarTabsForSave() {
        Set<String> hiddenTabs = new LinkedHashSet<>();
        for (int i = 0; i < sidebarTabListModel.size(); i++) {
            SidebarTabSettingItem item = sidebarTabListModel.get(i);
            if (!item.visible) {
                hiddenTabs.add(item.tab.name());
            }
        }
        return hiddenTabs;
    }

    private String getPersistedSidebarTabsStateSnapshot() {
        List<String> persistedState = new ArrayList<>();
        Set<String> hiddenTabs = SettingManager.getHiddenSidebarTabs();
        for (SidebarTab tab : SettingManager.getOrderedSidebarTabs()) {
            persistedState.add(tab.name() + ":" + (hiddenTabs.contains(tab.name()) ? "0" : "1"));
        }
        return String.join(",", persistedState);
    }

    /**
     * 更新字体预览
     */
    private void updateFontPreview() {
        try {
            String selectedFont = (String) fontNameComboBox.getSelectedItem();
            if (selectedFont == null) return;

            int fontSize = SettingManager.getUiFontSize(); // 使用当前设置的字体大小（首次使用会根据操作系统返回默认值）
            String sizeText = fontSizeField.getText().trim();
            if (!sizeText.isEmpty()) {
                try {
                    fontSize = Integer.parseInt(sizeText);
                    fontSize = Math.max(10, Math.min(24, fontSize));
                } catch (NumberFormatException e) {
                    // 使用默认大小
                }
            }

            // 字体预览处理：
            // 1. 如果选择系统默认，使用 deriveFont 保留降级链（确保 emoji 显示）
            // 2. 如果选择自定义字体，使用 new Font 让用户看到实际字体效果
            //    （注意：自定义字体可能不支持 emoji，这是预期行为，用户需要知道选择的字体效果）

            if (fontNameComboBox.getSelectedIndex() == 0) {
                // 选择了"系统默认"，使用 deriveFont 保留降级链
                Font baseFont = UIManager.getFont("Label.font");
                if (baseFont == null) {
                    baseFont = fontPreviewLabel.getFont();
                }
                // ✅ 使用 deriveFont 确保 emoji 和所有 Unicode 字符正确显示
                fontPreviewLabel.setFont(baseFont.deriveFont(Font.PLAIN, fontSize));
                fontSupportHintLabel.setText(I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_STATUS_SYSTEM_DEFAULT));
                fontSupportHintLabel.setForeground(getTextSecondaryColor());
            } else {
                // 选择了具体字体，创建该字体实例以展示真实效果
                // 这样用户可以看到选择的字体是否支持 emoji
                fontPreviewLabel.setFont(new Font(selectedFont, Font.PLAIN, fontSize));
                UiFontCatalog.FontSupport support = getFontSupport(selectedFont);
                if (support == UiFontCatalog.FontSupport.FULL) {
                    fontSupportHintLabel.setText(I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_STATUS_FULL));
                    fontSupportHintLabel.setForeground(getTextSecondaryColor());
                } else if (support == UiFontCatalog.FontSupport.NO_EMOJI) {
                    fontSupportHintLabel.setText(I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_STATUS_NO_EMOJI));
                    fontSupportHintLabel.setForeground(ModernColors.WARNING);
                } else {
                    fontSupportHintLabel.setText(I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_STATUS_NO_CJK));
                    fontSupportHintLabel.setForeground(ModernColors.ERROR);
                }
            }
        } catch (Exception e) {
            // 忽略预览更新错误
        }
    }

    private UiFontCatalog.FontSupport getFontSupport(String fontName) {
        return availableFontOptions.stream()
                .filter(option -> option.family().equals(fontName))
                .findFirst()
                .map(UiFontCatalog.FontOption::support)
                .orElseGet(() -> UiFontCatalog.inspectFamily(fontName));
    }

    private void loadFontOptionsIfNeeded() {
        if (fontOptionsLoaded || fontOptionsLoading) {
            return;
        }

        List<UiFontCatalog.FontOption> cachedOptions = UiFontCatalog.getCachedAvailableFontOptions();
        String selectedFont = getSelectedCustomFont();
        if (cachedOptions != null) {
            applyLoadedFontOptions(cachedOptions, selectedFont);
            return;
        }

        fontOptionsLoading = true;
        UiFontCatalog.getAvailableFontOptionsAsync().whenComplete((fontOptions, throwable) ->
                SwingUtilities.invokeLater(() -> {
                    fontOptionsLoading = false;
                    if (throwable != null) {
                        log.warn("Failed to load UI font options", throwable);
                        return;
                    }
                    if (fontNameComboBox.isPopupVisible()) {
                        applyLoadedFontOptions(fontOptions, selectedFont);
                        fontNameComboBox.hidePopup();
                        fontNameComboBox.showPopup();
                    }
                }));
    }

    private void preloadFontOptionsInBackground() {
        if (UiFontCatalog.getCachedAvailableFontOptions() != null || fontOptionsLoading) {
            return;
        }

        fontOptionsLoading = true;
        UiFontCatalog.getAvailableFontOptionsAsync().whenComplete((fontOptions, throwable) ->
                SwingUtilities.invokeLater(() -> {
                    fontOptionsLoading = false;
                    if (throwable != null) {
                        log.warn("Failed to preload UI font options", throwable);
                    }
                }));
    }

    private void applyLoadedFontOptions(List<UiFontCatalog.FontOption> fontOptions, String selectedFont) {
        availableFontOptions = fontOptions;
        resetFontComboItems(selectedFont);
        if (selectedFont == null || selectedFont.isBlank()) {
            fontNameComboBox.setSelectedIndex(0);
        } else {
            fontNameComboBox.setSelectedItem(selectedFont);
        }
        fontOptionsLoaded = true;
    }

    private void resetFontComboItems(String currentFont) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_SYSTEM_DEFAULT));

        List<String> families = UiFontCatalog.mergeFamiliesForCombo(
                currentFont,
                availableFontOptions.stream().map(UiFontCatalog.FontOption::family).toList()
        );
        for (String family : families) {
            model.addElement(family);
        }
        fontNameComboBox.setModel(model);
    }

    private String getSelectedCustomFont() {
        if (fontNameComboBox.getSelectedIndex() <= 0) {
            return "";
        }
        Object selectedItem = fontNameComboBox.getSelectedItem();
        return selectedItem == null ? "" : selectedItem.toString();
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

    }

    private void saveSettings(boolean closeAfterSave) {
        // 验证所有字段
        if (!validateAllFields()) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }
        if (countVisibleSidebarTabs() == 0) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_TABS_AT_LEAST_ONE));
            return;
        }

        boolean fontChanged = false; // 声明在 try 块外，以便在 catch 后也能访问

        try {
            String oldSidebarTabsState = getPersistedSidebarTabsStateSnapshot();

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
            SettingManager.setStartupSplashEnabled(startupSplashCheckBox.isSelected());

            // 保存侧边栏展开设置并更新UI
            boolean oldSidebarExpanded = SettingManager.isSidebarExpanded();
            boolean newSidebarExpanded = sidebarExpandedCheckBox.isSelected();
            SettingManager.setSidebarExpanded(newSidebarExpanded);
            SettingManager.setSidebarTabOrder(getSidebarTabOrderForSave());
            SettingManager.setHiddenSidebarTabs(getHiddenSidebarTabsForSave());
            String newSidebarTabsState = buildSidebarTabsStateSnapshot();
            if (oldSidebarExpanded != newSidebarExpanded || !oldSidebarTabsState.equals(newSidebarTabsState)) {
                updateSidebarConfiguration();
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
                if (!getFontSupport(newFontName).isUiSafe()) {
                    NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.SETTINGS_UI_FONT_UNSUPPORTED_WARNING));
                }
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
            trackComponentValue(startupSplashCheckBox);
            trackComponentValue(sidebarExpandedCheckBox);
            trackComponentValue(notificationPositionComboBox);
            trackComponentValue(fontNameComboBox);
            trackComponentValue(fontSizeField);
            trackComponentValue(sidebarTabsStateField);
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
     * 更新侧边栏配置
     */
    private void updateSidebarConfiguration() {
        try {
            SidebarTabPanel sidebarPanel = SingletonFactory.getInstance(SidebarTabPanel.class);
            sidebarPanel.refreshSidebarConfiguration();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static final class SidebarTabSettingItem {
        private final SidebarTab tab;
        private boolean visible;

        private SidebarTabSettingItem(SidebarTab tab, boolean visible) {
            this.tab = tab;
            this.visible = visible;
        }
    }

    private final class SidebarTabListCellRenderer extends JPanel implements ListCellRenderer<SidebarTabSettingItem> {
        private final JCheckBox visibleCheckBox = new JCheckBox();
        private final JLabel titleLabel = new JLabel();
        private final JLabel dragHintLabel = new JLabel("::");

        private SidebarTabListCellRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(6, 8, 6, 8));
            setOpaque(true);

            visibleCheckBox.setOpaque(false);
            visibleCheckBox.setFocusable(false);
            visibleCheckBox.setEnabled(true);

            titleLabel.setOpaque(false);
            dragHintLabel.setOpaque(false);

            add(visibleCheckBox, BorderLayout.WEST);
            add(titleLabel, BorderLayout.CENTER);
            add(dragHintLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SidebarTabSettingItem> list,
                                                      SidebarTabSettingItem value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            visibleCheckBox.setSelected(value.visible);
            titleLabel.setText(value.tab.getDisplayTitle());
            titleLabel.setFont(com.laker.postman.util.FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            titleLabel.setForeground(getTextPrimaryColor());

            dragHintLabel.setFont(com.laker.postman.util.FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
            dragHintLabel.setForeground(getTextSecondaryColor());

            Color background = isSelected ? getHoverBackgroundColor() : getInputBackgroundColor();
            setBackground(background);
            return this;
        }
    }

    private final class SidebarTabListTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(String.valueOf(sidebarTabList.getSelectedIndex()));
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDrop() && support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                int sourceIndex = Integer.parseInt((String) support.getTransferable()
                        .getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor));
                JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
                int targetIndex = dropLocation.getIndex();

                if (sourceIndex < 0 || sourceIndex >= sidebarTabListModel.size()) {
                    return false;
                }
                if (targetIndex < 0 || targetIndex > sidebarTabListModel.size()) {
                    return false;
                }

                SidebarTabSettingItem movedItem = sidebarTabListModel.get(sourceIndex);
                sidebarTabListModel.remove(sourceIndex);
                if (sourceIndex < targetIndex) {
                    targetIndex--;
                }
                sidebarTabListModel.add(targetIndex, movedItem);
                sidebarTabList.setSelectedIndex(targetIndex);
                syncSidebarTabsState();
                return true;
            } catch (Exception ex) {
                log.debug("Failed to reorder sidebar tabs", ex);
                return false;
            }
        }
    }

    private final class FontFamilyListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (index <= 0 || value == null) {
                label.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
                return label;
            }

            String family = value.toString();
            Font listFont = list.getFont();
            int size = listFont == null ? FontsUtil.getDefaultFont(Font.PLAIN).getSize() : listFont.getSize();
            label.setFont(new Font(family, Font.PLAIN, size));
            return label;
        }
    }
}
