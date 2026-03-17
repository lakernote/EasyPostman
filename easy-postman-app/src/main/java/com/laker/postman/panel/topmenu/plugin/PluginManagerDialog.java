package com.laker.postman.panel.topmenu.plugin;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.plugin.api.PluginDescriptor;
import com.laker.postman.plugin.manager.PluginInstallSource;
import com.laker.postman.plugin.manager.PluginManagementService;
import com.laker.postman.plugin.manager.PluginUninstallResult;
import com.laker.postman.plugin.manager.market.PluginCatalogEntry;
import com.laker.postman.plugin.runtime.PluginCompatibility;
import com.laker.postman.plugin.runtime.PluginFileInfo;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.service.update.version.VersionComparator;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件中心对话框。
 *
 * <p>交互目标：</p>
 * <ul>
 *     <li>插件作为一级能力单独管理，而不是被埋进通用设置页。</li>
 *     <li>把“已安装管理”和“发现/安装”拆成两个清晰视图，降低信息噪音。</li>
 *     <li>右侧统一展示详情与动作，避免列表区塞满按钮和长路径。</li>
 * </ul>
 */
@Slf4j
public class PluginManagerDialog extends JDialog {

    private static final String VIEW_INSTALLED = "installed";
    private static final String VIEW_MARKET = "market";
    private static final int DETAILS_MIN_WIDTH = 340;
    private static final int LIST_MIN_WIDTH = 260;
    private static final int SPLIT_DIVIDER_SIZE = 6;

    private final DefaultListModel<PluginFileInfo> installedListModel = new DefaultListModel<>();
    private final JList<PluginFileInfo> installedList = new JList<>(installedListModel);
    private final DefaultListModel<PluginCatalogEntry> marketListModel = new DefaultListModel<>();
    private final JList<PluginCatalogEntry> marketList = new JList<>(marketListModel);

    private final JLabel installedSummaryLabel = createSummaryMetricLabel();
    private final JLabel loadedSummaryLabel = createSummaryMetricLabel();
    private final JLabel catalogSummaryLabel = createSummaryMetricLabel();
    private final JLabel statusMessageLabel = createMutedLabel();

    private final JTextField catalogUrlField = new JTextField();

    private final JToggleButton installedViewButton = new JToggleButton(
            I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_TAB_INSTALLED));
    private final JToggleButton marketViewButton = new JToggleButton(
            I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_TAB_MARKET));
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);

    private final JButton openDirButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OPEN_FOLDER));
    private final JButton installLocalButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_INSTALL));
    private final JButton refreshInstalledButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_REFRESH));
    private final JButton enableInstalledButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_ENABLE));
    private final JButton disableInstalledButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DISABLE));
    private final JButton uninstallInstalledButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_UNINSTALL));

    private final JButton loadCatalogButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOAD));
    private final JButton installMarketButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALL));
    private final JButton openHomepageButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_OPEN_HOMEPAGE));
    private final JButton useOfficialGithubCatalogButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_OFFICIAL_GITHUB));
    private final JButton useOfficialGiteeCatalogButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_OFFICIAL_GITEE));

    private final JLabel installedDetailTitleLabel = createDetailTitleLabel();
    private final JLabel installedDetailMetaLabel = createMutedLabel();
    private final JLabel installedDetailStatusLabel = createStatusBadgeLabel();
    private final JTextArea installedDetailDescriptionArea = createReadOnlyArea(4, true);
    private final JLabel installedIdValueLabel = createValueLabel();
    private final JLabel installedCompatibilityValueLabel = createValueLabel();
    private final JLabel installedSourceValueLabel = createCompactValueLabel();
    private final JLabel installedPathValueLabel = createCompactValueLabel();

    private final JLabel marketDetailTitleLabel = createDetailTitleLabel();
    private final JLabel marketDetailMetaLabel = createMutedLabel();
    private final JLabel marketDetailStatusLabel = createStatusBadgeLabel();
    private final JTextArea marketDetailDescriptionArea = createReadOnlyArea(4, true);
    private final JLabel marketIdValueLabel = createValueLabel();
    private final JLabel marketCompatibilityValueLabel = createValueLabel();
    private final JLabel marketInstalledSourceValueLabel = createCompactValueLabel();

    private Map<String, PluginFileInfo> installedPluginMap = Map.of();
    private boolean marketBusy;

    private record CatalogLoadResult(List<PluginCatalogEntry> entries, boolean builtinFallback) {
    }

    private PluginManagerDialog(Window owner) {
        super(owner, I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_TITLE), ModalityType.APPLICATION_MODAL);
        initUI();
        reloadPlugins(null);
        loadSavedCatalogIfPresent();
    }

    public static void showDialog(Window owner) {
        PluginManagerDialog dialog = new PluginManagerDialog(owner);
        dialog.setVisible(true);
    }

    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setSize(1080, 720);
        setLocationRelativeTo(getOwner());

        JPanel content = new JPanel(new MigLayout(
                "fill, insets 16, gap 12, novisualpadding",
                "[grow,fill]",
                "[][grow,fill][]"
        ));
        content.setBackground(ModernColors.getBackgroundColor());
        setContentPane(content);

        content.add(createHeaderPanel(), "growx, wrap");

        contentPanel.setOpaque(false);
        contentPanel.add(createInstalledPanel(), VIEW_INSTALLED);
        contentPanel.add(createMarketPanel(), VIEW_MARKET);
        content.add(contentPanel, "grow, push, wrap");
        content.add(createFooterPanel(), "growx");

        configureActionButtons();

        installLocalButton.addActionListener(e -> installLocalPluginJar());
        openDirButton.addActionListener(e -> openManagedPluginDirectory());
        refreshInstalledButton.addActionListener(e -> reloadPlugins(getSelectedInstalledPluginId()));
        enableInstalledButton.addActionListener(e -> toggleSelectedInstalledPlugin(true));
        disableInstalledButton.addActionListener(e -> toggleSelectedInstalledPlugin(false));
        uninstallInstalledButton.addActionListener(e -> uninstallSelectedInstalledPlugin());

        loadCatalogButton.addActionListener(e -> loadCatalog());
        installMarketButton.addActionListener(e -> installSelectedCatalogPlugin());
        openHomepageButton.addActionListener(e -> openSelectedPluginHomepage());
        useOfficialGithubCatalogButton.addActionListener(e -> applyCatalogUrl(
                PluginManagementService.getOfficialCatalogUrl("github"), true));
        useOfficialGiteeCatalogButton.addActionListener(e -> applyCatalogUrl(
                PluginManagementService.getOfficialCatalogUrl("gitee"), true));

        installedList.setCellRenderer(new InstalledPluginCellRenderer());
        installedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        installedList.setVisibleRowCount(-1);
        configureListAppearance(installedList);
        installedList.addListSelectionListener(e -> {
            updateInstalledActions();
            updateInstalledDetails();
        });

        marketList.setCellRenderer(new MarketPluginCellRenderer());
        marketList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        marketList.setVisibleRowCount(-1);
        configureListAppearance(marketList);
        marketList.addListSelectionListener(e -> {
            updateMarketActions();
            updateMarketDetails();
        });

        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(installedViewButton);
        viewGroup.add(marketViewButton);
        installedViewButton.addActionListener(e -> showView(VIEW_INSTALLED));
        marketViewButton.addActionListener(e -> showView(VIEW_MARKET));
        installedViewButton.setSelected(true);
        showView(VIEW_INSTALLED);

        updateInstalledActions();
        updateInstalledDetails();
        updateMarketActions();
        updateMarketDetails();
        setStatusMessage(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_RESTART_HINT));
    }

    private JPanel createHeaderPanel() {
        JPanel panel = createCardPanel(new MigLayout(
                "fillx, insets 14 16 14 16, gap 12, novisualpadding",
                "[grow,fill][right]",
                "[][]"
        ));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_TITLE));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));

        panel.add(titleLabel, "growx");
        panel.add(createHeaderActionPanel(), "aligny top");
        panel.add(createMetricStrip(), "growx");
        panel.add(createNavigationPanel(), "alignx right");
        return panel;
    }

    private JPanel createHeaderActionPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 0, gap 8, novisualpadding",
                "[][][]",
                "[]"
        ));
        panel.setOpaque(false);
        panel.add(installLocalButton);
        panel.add(openDirButton);
        panel.add(refreshInstalledButton);
        return panel;
    }

    private JPanel createMetricStrip() {
        JPanel panel = new JPanel(new MigLayout(
                "fillx, insets 0, gap 10, novisualpadding",
                "[][][]",
                "[]"
        ));
        panel.setOpaque(false);
        panel.add(createMetricPill(installedSummaryLabel, I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_SUMMARY_INSTALLED)));
        panel.add(createMetricPill(loadedSummaryLabel, I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_SUMMARY_LOADED)));
        panel.add(createMetricPill(catalogSummaryLabel, I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_SUMMARY_CATALOG)));
        return panel;
    }

    private JPanel createMetricPill(JLabel valueLabel, String title) {
        JPanel panel = createSoftCard(new MigLayout(
                "insets 6 10 6 10, gap 8, novisualpadding",
                "[][]",
                "[]"
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(ModernColors.getTextHint());
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(titleLabel);
        panel.add(valueLabel);
        return panel;
    }

    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 0, gap 8, novisualpadding",
                "[][]",
                "[]"
        ));
        panel.setOpaque(false);

        configureViewToggle(installedViewButton);
        configureViewToggle(marketViewButton);

        panel.add(installedViewButton);
        panel.add(marketViewButton);
        return panel;
    }

    private void configureViewToggle(AbstractButton button) {
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getBorderLightColor()),
                new EmptyBorder(7, 12, 7, 12)
        ));
    }

    private JPanel createInstalledPanel() {
        return wrapContentCard(createInstalledSplitPane());
    }

    private JSplitPane createInstalledSplitPane() {
        return createHorizontalSplitPane(createInstalledListPanel(), createDetailScrollPane(createInstalledDetailsPanel()), 0.56);
    }

    private JPanel createInstalledListPanel() {
        JPanel panel = createCardPanel(new MigLayout(
                "fill, insets 14, gap 10, novisualpadding",
                "[grow,fill]",
                "[][grow,fill]"
        ));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_TAB_INSTALLED));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        panel.add(titleLabel, "growx, wrap");
        panel.add(createListScrollPane(installedList), "grow, push");
        return panel;
    }

    private JPanel createInstalledDetailsPanel() {
        JPanel panel = createCardPanel(new MigLayout(
                "fill, insets 16, gap 10, novisualpadding",
                "[grow,fill]",
                "[][][][][][][push][]"
        ));

        JPanel titleRow = new JPanel(new MigLayout("insets 0, gap 8, novisualpadding", "[grow,fill][]", "[]"));
        titleRow.setOpaque(false);
        titleRow.add(installedDetailTitleLabel, "growx");
        titleRow.add(installedDetailStatusLabel);

        panel.add(titleRow, "growx, wrap");
        panel.add(installedDetailMetaLabel, "growx, wrap");
        panel.add(installedDetailDescriptionArea, "growx, wrap");
        panel.add(createDetailRow(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_ID), installedIdValueLabel), "growx, wrap");
        panel.add(createDetailRow(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_COMPATIBILITY), installedCompatibilityValueLabel), "growx, wrap");
        panel.add(createDetailRow(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_INSTALL_SOURCE), installedSourceValueLabel), "growx, wrap");
        panel.add(createDetailRow(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_PATH), installedPathValueLabel), "growx, wrap");
        panel.add(new JPanel(), "grow, push, wrap");
        panel.add(createInstalledActionPanel(), "growx");
        return panel;
    }

    private JPanel createInstalledActionPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 0, gap 8, novisualpadding",
                "[grow,fill][grow,fill][grow,fill]",
                "[]"
        ));
        panel.setOpaque(false);
        panel.add(enableInstalledButton, "growx");
        panel.add(disableInstalledButton, "growx");
        panel.add(uninstallInstalledButton, "growx");
        return panel;
    }

    private JPanel createMarketPanel() {
        return wrapContentCard(createMarketSplitPane());
    }

    private JSplitPane createMarketSplitPane() {
        return createHorizontalSplitPane(createMarketListPanel(), createDetailScrollPane(createMarketDetailsPanel()), 0.56);
    }

    private JSplitPane createHorizontalSplitPane(JComponent left, JComponent right, double resizeWeight) {
        left.setMinimumSize(new Dimension(LIST_MIN_WIDTH, 200));
        right.setMinimumSize(new Dimension(DETAILS_MIN_WIDTH, 200));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(SPLIT_DIVIDER_SIZE);
        splitPane.setContinuousLayout(true);
        splitPane.setResizeWeight(resizeWeight);
        splitPane.setOneTouchExpandable(false);

        // 对话框首次布局完成后再按比例落位，避免在不同父窗口/入口下 divider 初始位置漂移。
        SwingUtilities.invokeLater(() -> {
            if (splitPane.isDisplayable()) {
                splitPane.setDividerLocation(resizeWeight);
            }
        });
        return splitPane;
    }

    private JScrollPane createDetailScrollPane(JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createMarketListPanel() {
        JPanel panel = createCardPanel(new MigLayout(
                "fill, insets 14, gap 10, novisualpadding",
                "[grow,fill]",
                "[][][grow,fill]"
        ));
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_TAB_MARKET));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(titleLabel, "growx, wrap");
        panel.add(createCatalogToolbar(), "growx, wrap");
        panel.add(createListScrollPane(marketList), "grow, push");
        return panel;
    }

    private JPanel createCatalogToolbar() {
        JPanel panel = new JPanel(new MigLayout(
                "fillx, insets 0, gap 8, novisualpadding",
                "[grow,fill][][]",
                "[][]"
        ));
        panel.setOpaque(false);

        panel.add(catalogUrlField, "growx");
        panel.add(loadCatalogButton);
        panel.add(useOfficialGithubCatalogButton);
        panel.add(useOfficialGiteeCatalogButton, "wrap");
        panel.add(createMutedLabel(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_SOURCE_HINT)),
                "span 4, growx");
        return panel;
    }

    private JPanel createMarketDetailsPanel() {
        JPanel panel = createCardPanel(new MigLayout(
                "fill, insets 16, gap 10, novisualpadding",
                "[grow,fill]",
                "[][][][][push][]"
        ));

        JPanel titleRow = new JPanel(new MigLayout("insets 0, gap 8, novisualpadding", "[grow,fill][]", "[]"));
        titleRow.setOpaque(false);
        titleRow.add(marketDetailTitleLabel, "growx");
        titleRow.add(marketDetailStatusLabel);

        panel.add(titleRow, "growx, wrap");
        panel.add(marketDetailMetaLabel, "growx, wrap");
        panel.add(marketDetailDescriptionArea, "growx, wrap");
        panel.add(createDetailRow(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_ID), marketIdValueLabel), "growx, wrap");
        panel.add(createDetailRow(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_COMPATIBILITY), marketCompatibilityValueLabel), "growx, wrap");
        panel.add(createDetailRow(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_CURRENT_INSTALL_SOURCE), marketInstalledSourceValueLabel), "growx, wrap");
        panel.add(new JPanel(), "grow, push, wrap");
        panel.add(createMarketActionPanel(), "growx");
        return panel;
    }

    private JPanel createMarketActionPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 0, gap 8, novisualpadding",
                "[grow,fill][grow,fill]",
                "[]"
        ));
        panel.setOpaque(false);
        panel.add(installMarketButton, "growx");
        panel.add(openHomepageButton, "growx");
        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "fillx, insets 0, gap 10, novisualpadding",
                "[grow,fill][right][]",
                "[]"
        ));
        panel.setOpaque(false);

        JButton closeButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE));
        closeButton.addActionListener(e -> dispose());
        configureButton(closeButton, false);

        panel.add(statusMessageLabel, "growx");
        panel.add(createDirectoryHintLabel(), "gapright 8");
        panel.add(closeButton, "alignx right");
        return panel;
    }

    private JLabel createDirectoryHintLabel() {
        JLabel label = createMutedLabel();
        label.setText(shorten(PluginManagementService.getManagedPluginDir().toString(), 52));
        label.setToolTipText("<html>"
                + escapeHtml(PluginManagementService.getManagedPluginDir().toString())
                + "<br>"
                + escapeHtml(PluginManagementService.getPluginPackageDir().toString())
                + "</html>");
        return label;
    }

    private void loadSavedCatalogIfPresent() {
        String catalogUrl = PluginManagementService.getCatalogUrl();
        if (catalogUrl == null || catalogUrl.isBlank()) {
            applyCatalogUrl(resolvePreferredOfficialCatalogUrl(), true);
            return;
        }
        catalogUrlField.setText(catalogUrl);
        loadCatalog();
    }

    private void applyCatalogUrl(String catalogUrl, boolean autoLoad) {
        catalogUrlField.setText(catalogUrl == null ? "" : catalogUrl.trim());
        if (autoLoad) {
            loadCatalog();
        }
    }

    private String resolvePreferredOfficialCatalogUrl() {
        String preference = SettingManager.getUpdateSourcePreference();
        if ("github".equalsIgnoreCase(preference)) {
            return PluginManagementService.getOfficialCatalogUrl("github");
        }
        return PluginManagementService.getOfficialCatalogUrl("gitee");
    }

    private void reloadPlugins(String preferredPluginId) {
        installedListModel.clear();
        List<PluginFileInfo> plugins = PluginManagementService.getInstalledPlugins();
        installedPluginMap = buildInstalledPluginMap(plugins);
        if (plugins.isEmpty()) {
            installedListModel.addElement(new PluginFileInfo(
                    new PluginDescriptor("empty", I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_EMPTY), "", "", "", ""),
                    PluginManagementService.getManagedPluginDir(),
                    false,
                    false,
                    true
            ));
            installedList.setEnabled(false);
        } else {
            installedList.setEnabled(true);
            for (PluginFileInfo info : plugins) {
                installedListModel.addElement(info);
            }
            selectInstalledPlugin(preferredPluginId);
        }
        marketList.repaint();
        updateInstalledActions();
        updateInstalledDetails();
        updateMarketActions();
        updateMarketDetails();
        updateSummaryMetrics();
    }

    private void selectInstalledPlugin(String preferredPluginId) {
        if (preferredPluginId != null) {
            for (int i = 0; i < installedListModel.size(); i++) {
                PluginFileInfo info = installedListModel.getElementAt(i);
                if (preferredPluginId.equals(info.descriptor().id())) {
                    installedList.setSelectedIndex(i);
                    installedList.ensureIndexIsVisible(i);
                    return;
                }
            }
        }
        if (!installedListModel.isEmpty()) {
            installedList.setSelectedIndex(0);
        }
    }

    private Map<String, PluginFileInfo> buildInstalledPluginMap(List<PluginFileInfo> plugins) {
        Map<String, PluginFileInfo> map = new LinkedHashMap<>();
        for (PluginFileInfo info : plugins) {
            PluginFileInfo existing = map.get(info.descriptor().id());
            if (existing == null) {
                map.put(info.descriptor().id(), info);
                continue;
            }
            if (info.loaded() && !existing.loaded()) {
                map.put(info.descriptor().id(), info);
                continue;
            }
            if (VersionComparator.compare(info.descriptor().version(), existing.descriptor().version()) > 0) {
                map.put(info.descriptor().id(), info);
            }
        }
        return map;
    }

    private void openManagedPluginDirectory() {
        Path pluginDir = PluginManagementService.getManagedPluginDir();
        try {
            Desktop.getDesktop().open(pluginDir.toFile());
        } catch (Exception e) {
            log.error("Failed to open plugin directory: {}", pluginDir, e);
            showError(e);
        }
    }

    private void toggleSelectedInstalledPlugin(boolean enabled) {
        PluginFileInfo selected = installedList.getSelectedValue();
        if (selected == null || "empty".equals(selected.descriptor().id())) {
            return;
        }
        PluginManagementService.setPluginEnabled(selected.descriptor().id(), enabled);
        reloadPlugins(selected.descriptor().id());
        setStatusMessage(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_RESTART_HINT));
    }

    private void uninstallSelectedInstalledPlugin() {
        PluginFileInfo selected = installedList.getSelectedValue();
        if (selected == null || "empty".equals(selected.descriptor().id())
                || !PluginManagementService.isManagedPlugin(selected.jarPath())) {
            return;
        }
        int option = JOptionPane.showConfirmDialog(
                this,
                I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_UNINSTALL_CONFIRM, selected.descriptor().name()),
                I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_UNINSTALL_TITLE),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (option != JOptionPane.OK_OPTION) {
            return;
        }
        PluginUninstallResult result = PluginManagementService.uninstallPlugin(selected.descriptor().id());
        if (result.removed()) {
            showInfo(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_UNINSTALL_SUCCESS, selected.descriptor().name()));
            reloadPlugins(null);
            return;
        }
        if (result.restartRequired()) {
            showInfo(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_UNINSTALL_SCHEDULED, selected.descriptor().name()));
            reloadPlugins(selected.descriptor().id());
            return;
        }
        showError(new IllegalStateException(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_UNINSTALL_FAILED)));
    }

    private void installLocalPluginJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_FILE_CHOOSER));
        chooser.setFileFilter(new FileNameExtensionFilter("JAR", "jar"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path source = chooser.getSelectedFile().toPath();
        try {
            PluginFileInfo installed = PluginManagementService.installPluginJar(source);
            showInfo(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_INSTALL_SUCCESS, installed.jarPath()));
            showView(VIEW_INSTALLED);
            reloadPlugins(installed.descriptor().id());
        } catch (IOException | IllegalArgumentException e) {
            log.error("Failed to install plugin jar: {}", source, e);
            showError(e);
        }
    }

    private void loadCatalog() {
        String rawCatalogUrl = catalogUrlField.getText().trim();
        String catalogUrl = rawCatalogUrl;
        PluginManagementService.saveCatalogUrl(catalogUrl);
        catalogUrlField.setText(catalogUrl);
        if (catalogUrl.isBlank()) {
            setMarketPlaceholder(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_HINT));
            setStatusMessage(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_HINT));
            updateMarketActions();
            updateMarketDetails();
            updateSummaryMetrics();
            return;
        }

        showView(VIEW_MARKET);
        setMarketBusy(true, I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOADING));
        new SwingWorker<CatalogLoadResult, Void>() {
            @Override
            protected CatalogLoadResult doInBackground() throws Exception {
                try {
                    return new CatalogLoadResult(PluginManagementService.loadCatalog(catalogUrl), false);
                } catch (Exception remoteError) {
                    String source = PluginManagementService.detectOfficialCatalogSource(catalogUrl);
                    if (source.isBlank()) {
                        throw remoteError;
                    }
                    log.warn("Failed to load official remote plugin catalog, falling back to bundled catalog: {}",
                            catalogUrl, remoteError);
                    try {
                        return new CatalogLoadResult(PluginManagementService.loadBundledOfficialCatalog(source), true);
                    } catch (Exception fallbackError) {
                        remoteError.addSuppressed(fallbackError);
                        throw remoteError;
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    CatalogLoadResult result = get();
                    applyMarketEntries(result.entries());
                    if (result.builtinFallback()) {
                        setStatusMessage(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOAD_FALLBACK_BUILTIN));
                    } else {
                        setStatusMessage(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_HINT));
                    }
                } catch (Exception e) {
                    log.error("Failed to load plugin catalog: {}", catalogUrl, e);
                    setMarketPlaceholder(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOAD_FAILED));
                    setStatusMessage(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOAD_FAILED));
                    showError(e);
                } finally {
                    setMarketBusy(false, statusMessageLabel.getText());
                    updateMarketActions();
                    updateMarketDetails();
                    updateSummaryMetrics();
                }
            }
        }.execute();
    }

    private void applyMarketEntries(List<PluginCatalogEntry> entries) {
        marketListModel.clear();
        if (entries == null || entries.isEmpty()) {
            setMarketPlaceholder(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_EMPTY));
            return;
        }
        for (PluginCatalogEntry entry : entries) {
            marketListModel.addElement(entry);
        }
        marketList.setSelectedIndex(0);
        marketList.ensureIndexIsVisible(0);
    }

    private void setMarketPlaceholder(String message) {
        marketListModel.clear();
        marketListModel.addElement(new PluginCatalogEntry("empty", message, "", "", "", "", ""));
        marketList.setSelectedIndex(0);
    }

    private void installSelectedCatalogPlugin() {
        PluginCatalogEntry selected = marketList.getSelectedValue();
        if (selected == null || selected.isPlaceholder()) {
            setStatusMessage(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_NO_SELECTION));
            return;
        }

        setMarketBusy(true, I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALLING, selected.name()));
        new SwingWorker<PluginFileInfo, Void>() {
            @Override
            protected PluginFileInfo doInBackground() throws Exception {
                return PluginManagementService.installCatalogPlugin(selected);
            }

            @Override
            protected void done() {
                try {
                    PluginFileInfo installed = get();
                    reloadPlugins(installed.descriptor().id());
                    marketList.repaint();
                    showView(VIEW_INSTALLED);
                    setStatusMessage(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALL_SUCCESS, installed.jarPath()));
                    showInfo(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_INSTALL_SUCCESS, installed.jarPath()));
                } catch (Exception e) {
                    log.error("Failed to install plugin from catalog: {}", selected.id(), e);
                    setStatusMessage(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALL_FAILED));
                    showError(e);
                } finally {
                    setMarketBusy(false, statusMessageLabel.getText());
                    updateMarketActions();
                }
            }
        }.execute();
    }

    private void openSelectedPluginHomepage() {
        PluginCatalogEntry selected = marketList.getSelectedValue();
        if (selected == null || selected.isPlaceholder() || !selected.hasHomepageUrl()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(selected.homepageUrl()));
        } catch (Exception e) {
            log.error("Failed to open plugin homepage: {}", selected.homepageUrl(), e);
            showError(e);
        }
    }

    private void updateInstalledActions() {
        PluginFileInfo selected = installedList.getSelectedValue();
        boolean validSelection = selected != null && !"empty".equals(selected.descriptor().id());
        boolean pendingUninstall = validSelection && PluginManagementService.isPluginPendingUninstall(selected.descriptor().id());
        enableInstalledButton.setEnabled(validSelection && !selected.enabled());
        disableInstalledButton.setEnabled(validSelection && selected.enabled() && !pendingUninstall);
        uninstallInstalledButton.setEnabled(validSelection
                && PluginManagementService.isManagedPlugin(selected.jarPath())
                && !pendingUninstall);
    }

    private void updateMarketActions() {
        PluginCatalogEntry selected = marketList.getSelectedValue();
        boolean validSelection = selected != null && !selected.isPlaceholder();
        String actionText = I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALL);
        boolean actionEnabled = !marketBusy && validSelection;
        if (validSelection) {
            PluginCompatibility compatibility = PluginManagementService.evaluateCompatibility(selected);
            if (!compatibility.compatible()) {
                actionText = I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_ACTION_REQUIRES_HOST_UPGRADE);
                actionEnabled = false;
            }
            PluginFileInfo installed = installedPluginMap.get(selected.id());
            if (installed != null && compatibility.compatible()) {
                int compare = VersionComparator.compare(selected.version(), installed.descriptor().version());
                if (compare > 0) {
                    actionText = I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_UPDATE);
                } else if (compare == 0) {
                    actionText = I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_ACTION_INSTALLED);
                    actionEnabled = false;
                } else {
                    actionText = I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_ACTION_LOCAL_NEWER);
                    actionEnabled = false;
                }
            }
        }
        loadCatalogButton.setEnabled(!marketBusy);
        installMarketButton.setText(actionText);
        installMarketButton.setEnabled(actionEnabled);
        openHomepageButton.setEnabled(!marketBusy && validSelection && selected.hasHomepageUrl());
        catalogUrlField.setEnabled(!marketBusy);
        useOfficialGithubCatalogButton.setEnabled(!marketBusy);
        useOfficialGiteeCatalogButton.setEnabled(!marketBusy);
    }

    private void updateInstalledDetails() {
        PluginFileInfo selected = installedList.getSelectedValue();
        if (selected == null || "empty".equals(selected.descriptor().id())) {
            resetInstalledDetails();
            return;
        }

        PluginDescriptor descriptor = selected.descriptor();
        installedDetailTitleLabel.setText(descriptor.name());
        installedDetailMetaLabel.setText(descriptor.id() + "  ·  " + descriptor.version());
        installedDetailDescriptionArea.setText(descriptor.hasDescription()
                ? descriptor.description()
                : I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_EMPTY));
        installedIdValueLabel.setText(descriptor.id());
        installedCompatibilityValueLabel.setText(buildCompatibilityValue(descriptor, selected.compatible()));
        setCompactText(installedSourceValueLabel, resolveInstalledSourceText(descriptor.id(), selected.jarPath()));
        setCompactText(installedPathValueLabel, selected.jarPath().toString());
        applyStatusBadge(installedDetailStatusLabel, resolveInstalledStatus(selected));
    }

    private void updateMarketDetails() {
        PluginCatalogEntry selected = marketList.getSelectedValue();
        if (selected == null || selected.isPlaceholder()) {
            resetMarketDetails();
            return;
        }

        marketDetailTitleLabel.setText(selected.name());
        marketDetailMetaLabel.setText(selected.id() + "  ·  " + selected.version());
        marketDetailDescriptionArea.setText(selected.hasDescription()
                ? selected.description()
                : I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_EMPTY));
        marketIdValueLabel.setText(selected.id());
        marketCompatibilityValueLabel.setText(buildCompatibilityValue(selected));
        setCompactText(marketInstalledSourceValueLabel, resolveInstalledSourceText(selected.id(), null));
        applyStatusBadge(marketDetailStatusLabel, getMarketEntryStatus(selected));
    }

    private void resetInstalledDetails() {
        installedDetailTitleLabel.setText(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_SECTION_DETAILS));
        installedDetailMetaLabel.setText("");
        installedDetailDescriptionArea.setText(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_EMPTY));
        installedIdValueLabel.setText("-");
        installedCompatibilityValueLabel.setText("-");
        setCompactText(installedSourceValueLabel, "-");
        setCompactText(installedPathValueLabel, "-");
        applyStatusBadge(installedDetailStatusLabel, "");
    }

    private void resetMarketDetails() {
        marketDetailTitleLabel.setText(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_SECTION_DETAILS));
        marketDetailMetaLabel.setText("");
        marketDetailDescriptionArea.setText(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DETAIL_EMPTY));
        marketIdValueLabel.setText("-");
        marketCompatibilityValueLabel.setText("-");
        setCompactText(marketInstalledSourceValueLabel, "-");
        applyStatusBadge(marketDetailStatusLabel, "");
    }

    private void updateSummaryMetrics() {
        int installedCount = 0;
        int loadedCount = 0;
        for (int i = 0; i < installedListModel.size(); i++) {
            PluginFileInfo info = installedListModel.getElementAt(i);
            if ("empty".equals(info.descriptor().id())) {
                continue;
            }
            installedCount++;
            if (info.loaded()) {
                loadedCount++;
            }
        }

        int catalogCount = 0;
        for (int i = 0; i < marketListModel.size(); i++) {
            PluginCatalogEntry entry = marketListModel.getElementAt(i);
            if (!entry.isPlaceholder()) {
                catalogCount++;
            }
        }

        installedSummaryLabel.setText(String.valueOf(installedCount));
        loadedSummaryLabel.setText(String.valueOf(loadedCount));
        catalogSummaryLabel.setText(String.valueOf(catalogCount));
    }

    private void showView(String view) {
        contentLayout.show(contentPanel, view);
        installedViewButton.setSelected(VIEW_INSTALLED.equals(view));
        marketViewButton.setSelected(VIEW_MARKET.equals(view));
    }

    private String getSelectedInstalledPluginId() {
        PluginFileInfo selected = installedList.getSelectedValue();
        if (selected == null || "empty".equals(selected.descriptor().id())) {
            return null;
        }
        return selected.descriptor().id();
    }

    private void setMarketBusy(boolean busy, String message) {
        marketBusy = busy;
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        setStatusMessage(message);
        updateMarketActions();
    }

    private void setStatusMessage(String message) {
        statusMessageLabel.setText(message == null ? "" : message);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message,
                I18nUtil.getMessage(MessageKeys.GENERAL_INFO), JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = current.getClass().getSimpleName();
        }
        JOptionPane.showMessageDialog(this, message,
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
    }

    private String getMarketEntryStatus(PluginCatalogEntry entry) {
        PluginCompatibility compatibility = PluginManagementService.evaluateCompatibility(entry);
        if (!compatibility.compatible()) {
            PluginFileInfo installed = installedPluginMap.get(entry.id());
            if (installed != null && VersionComparator.compare(entry.version(), installed.descriptor().version()) > 0) {
                return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_UPDATE_REQUIRES_HOST_UPGRADE);
            }
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_REQUIRES_HOST_UPGRADE);
        }
        PluginFileInfo installed = installedPluginMap.get(entry.id());
        if (installed == null) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_AVAILABLE);
        }
        if (PluginManagementService.isPluginPendingUninstall(installed.descriptor().id())) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_UNINSTALL_PENDING);
        }
        if (installed.loaded() && !installed.enabled()) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_DISABLE_PENDING);
        }
        if (!installed.enabled()) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_DISABLED);
        }
        if (!installed.compatible()) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_INCOMPATIBLE);
        }
        int compare = VersionComparator.compare(entry.version(), installed.descriptor().version());
        if (compare > 0) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_UPDATE_AVAILABLE,
                    installed.descriptor().version());
        }
        if (compare < 0) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOCAL_NEWER,
                    installed.descriptor().version());
        }
        if (!installed.loaded()) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_RESTART_REQUIRED);
        }
        return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALLED, installed.descriptor().version());
    }

    private String resolveInstalledSourceText(String pluginId, Path fallbackJarPath) {
        PluginInstallSource source = PluginManagementService.getInstallSource(pluginId);
        if (source != null && source.hasLocation()) {
            String sourceLabel = source.isLocal()
                    ? I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_SOURCE_LOCAL)
                    : I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_SOURCE_MARKET);
            return sourceLabel + " · " + source.location();
        }

        PluginFileInfo installed = installedPluginMap.get(pluginId);
        if (installed != null) {
            return installed.jarPath().toString();
        }
        if (fallbackJarPath != null) {
            return fallbackJarPath.toString();
        }
        return "-";
    }

    private static String resolveInstalledStatus(PluginFileInfo value) {
        if (PluginManagementService.isPluginPendingUninstall(value.descriptor().id())) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_UNINSTALL_PENDING);
        }
        if (value.loaded() && !value.enabled()) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_DISABLE_PENDING);
        }
        if (!value.enabled()) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_DISABLED);
        }
        if (!value.compatible()) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_INCOMPATIBLE);
        }
        return value.loaded()
                ? I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_LOADED)
                : I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_RESTART_REQUIRED);
    }

    private String buildCompatibilityValue(PluginDescriptor descriptor, boolean compatible) {
        return buildCompatibilityValue(PluginManagementService.evaluateCompatibility(descriptor), compatible);
    }

    private String buildCompatibilityValue(PluginCatalogEntry entry) {
        PluginCompatibility compatibility = PluginManagementService.evaluateCompatibility(entry);
        return buildCompatibilityValue(compatibility, compatibility.compatible());
    }

    private String buildCompatibilityValue(PluginCompatibility compatibility, boolean compatible) {
        if (compatible) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_COMPATIBILITY_CURRENT);
        }
        if (!compatibility.appVersionCompatible()) {
            return buildRequiredAppText(compatibility.minAppVersion(), compatibility.maxAppVersion());
        }
        return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_COMPATIBILITY_REQUIRES_HOST_UPGRADE);
    }

    private String buildRequiredAppText(String minVersion, String maxVersion) {
        boolean hasMin = minVersion != null && !minVersion.isBlank();
        boolean hasMax = maxVersion != null && !maxVersion.isBlank();
        if (hasMin && hasMax) {
            if (minVersion.equals(maxVersion)) {
                return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_COMPATIBILITY_REQUIRES_APP, minVersion);
            }
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_COMPATIBILITY_REQUIRES_APP_RANGE, minVersion, maxVersion);
        }
        if (hasMin) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_COMPATIBILITY_REQUIRES_APP_MIN, minVersion);
        }
        if (hasMax) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_COMPATIBILITY_REQUIRES_APP_MAX, maxVersion);
        }
        return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_COMPATIBILITY_REQUIRES_HOST_UPGRADE);
    }

    private JPanel createDetailRow(String key, JComponent valueComponent) {
        JPanel panel = new JPanel(new MigLayout(
                "fillx, insets 0, gap 10, novisualpadding",
                "[72!][grow,fill]",
                "[]"
        ));
        panel.setOpaque(false);

        JLabel keyLabel = new JLabel(key);
        keyLabel.setForeground(ModernColors.getTextHint());
        panel.add(keyLabel);
        panel.add(valueComponent, "growx");
        return panel;
    }

    private JPanel wrapContentCard(JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createListScrollPane(JList<?> list) {
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ModernColors.getCardBackgroundColor());
        return scrollPane;
    }

    private JPanel createCardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(true);
        panel.setBackground(ModernColors.getCardBackgroundColor());
        panel.setBorder(createCardBorder());
        return panel;
    }

    private JPanel createSoftCard(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(true);
        panel.setBackground(ModernColors.getHoverBackgroundColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getBorderLightColor()),
                new EmptyBorder(0, 0, 0, 0)
        ));
        return panel;
    }

    private Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getBorderLightColor()),
                new EmptyBorder(0, 0, 0, 0)
        );
    }

    private static JLabel createSummaryMetricLabel() {
        JLabel label = new JLabel("0");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        return label;
    }

    private static JLabel createMutedLabel() {
        JLabel label = new JLabel();
        label.setForeground(ModernColors.getTextHint());
        return label;
    }

    private static JLabel createMutedLabel(String text) {
        JLabel label = createMutedLabel();
        label.setText(text);
        return label;
    }

    private static JLabel createDetailTitleLabel() {
        JLabel label = new JLabel();
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        return label;
    }

    private static JLabel createValueLabel() {
        JLabel label = new JLabel("-");
        label.setForeground(ModernColors.getTextPrimary());
        return label;
    }

    private static JLabel createCompactValueLabel() {
        JLabel label = createValueLabel();
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    private static JTextArea createReadOnlyArea(int rows) {
        return createReadOnlyArea(rows, true);
    }

    private static JTextArea createReadOnlyArea(int rows, boolean wrapLines) {
        JTextArea area = new JTextArea(rows, 0);
        area.setEditable(false);
        area.setLineWrap(wrapLines);
        area.setWrapStyleWord(wrapLines);
        area.setOpaque(false);
        area.setBorder(new EmptyBorder(0, 0, 0, 0));
        area.setForeground(ModernColors.getTextSecondary());
        return area;
    }

    private void setCompactText(JLabel label, String text) {
        String value = text == null || text.isBlank() ? "-" : text;
        label.setText(shorten(value, 96));
        label.setToolTipText(value.length() > 96 ? value : null);
    }

    private void configureActionButtons() {
        configureButton(installLocalButton, true);
        configureButton(openDirButton, false);
        configureButton(refreshInstalledButton, false);
        configureButton(enableInstalledButton, false);
        configureButton(disableInstalledButton, false);
        configureButton(uninstallInstalledButton, false);
        configureButton(loadCatalogButton, true);
        configureButton(installMarketButton, true);
        configureButton(openHomepageButton, false);
        configureButton(useOfficialGithubCatalogButton, false);
        configureButton(useOfficialGiteeCatalogButton, false);
    }

    private void configureListAppearance(JList<?> list) {
        list.setBackground(ModernColors.getCardBackgroundColor());
        list.setForeground(ModernColors.getTextPrimary());
        list.setSelectionBackground(adaptSelectionBackground(ModernColors.PRIMARY));
        list.setSelectionForeground(ModernColors.getTextPrimary());
        list.setBorder(BorderFactory.createEmptyBorder());
    }

    private Color adaptSelectionBackground(Color color) {
        if (ModernColors.isDarkTheme()) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 110);
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 28);
    }

    private void configureButton(AbstractButton button, boolean primary) {
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ModernColors.PRIMARY : ModernColors.getBorderLightColor()),
                new EmptyBorder(7, 12, 7, 12)
        ));
        if (primary) {
            button.setBackground(ModernColors.PRIMARY);
            button.setForeground(Color.WHITE);
            button.setOpaque(true);
        } else {
            button.setOpaque(true);
            button.setBackground(ModernColors.getCardBackgroundColor());
            button.setForeground(ModernColors.getTextPrimary());
        }
    }

    private static String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static JLabel createStatusBadgeLabel() {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBorder(new EmptyBorder(4, 8, 4, 8));
        return label;
    }

    private void applyStatusBadge(JLabel label, String text) {
        label.setText(text == null ? "" : text);
        if (text == null || text.isBlank()) {
            label.setVisible(false);
            return;
        }
        label.setVisible(true);
        StatusPalette palette = resolveStatusPalette(text);
        label.setBackground(palette.background());
        label.setForeground(palette.foreground());
    }

    private StatusPalette resolveStatusPalette(String text) {
        if (text.contains(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_UNINSTALL_PENDING))
                || text.contains(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_DISABLE_PENDING))
                || text.contains(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_RESTART_REQUIRED))
                || text.contains(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_UPDATE_AVAILABLE).split("\\{")[0])) {
            return new StatusPalette(adaptStatusBackground(ModernColors.WARNING), adaptStatusForeground(ModernColors.WARNING));
        }
        if (text.contains(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_DISABLED))
                || text.contains(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_INCOMPATIBLE))) {
            return new StatusPalette(adaptStatusBackground(ModernColors.ERROR), adaptStatusForeground(ModernColors.ERROR));
        }
        return new StatusPalette(adaptStatusBackground(ModernColors.SUCCESS), adaptStatusForeground(ModernColors.SUCCESS));
    }

    private Color adaptStatusBackground(Color color) {
        if (ModernColors.isDarkTheme()) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 90);
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 32);
    }

    private Color adaptStatusForeground(Color color) {
        if (ModernColors.isDarkTheme()) {
            return Color.WHITE;
        }
        return color.darker();
    }

    private static final class StatusPalette {
        private final Color background;
        private final Color foreground;

        private StatusPalette(Color background, Color foreground) {
            this.background = background;
            this.foreground = foreground;
        }

        private Color background() {
            return background;
        }

        private Color foreground() {
            return foreground;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private final class InstalledPluginCellRenderer extends JPanel implements ListCellRenderer<PluginFileInfo> {
        private final JLabel titleLabel = new JLabel();
        private final JLabel metaLabel = new JLabel();
        private final JLabel detailLabel = new JLabel();
        private final JLabel statusLabel = createStatusBadgeLabel();

        private InstalledPluginCellRenderer() {
            setLayout(new BorderLayout(10, 6));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getBorderLightColor()),
                    new EmptyBorder(10, 12, 10, 12)
            ));
            setOpaque(true);

            JPanel center = new JPanel(new GridLayout(0, 1, 0, 4));
            center.setOpaque(false);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
            metaLabel.setForeground(ModernColors.getTextHint());
            detailLabel.setForeground(ModernColors.getTextSecondary());
            center.add(titleLabel);
            center.add(metaLabel);
            center.add(detailLabel);

            add(center, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PluginFileInfo> list, PluginFileInfo value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            boolean isPlaceholder = "empty".equals(value.descriptor().id());
            titleLabel.setText(value.descriptor().name());

            if (isPlaceholder) {
                metaLabel.setText("");
                detailLabel.setText(escapeHtml(value.jarPath().toString()));
                applyStatusBadge(statusLabel, "");
            } else {
                PluginDescriptor descriptor = value.descriptor();
                metaLabel.setText(descriptor.id() + "  ·  " + descriptor.version());
                detailLabel.setText(descriptor.hasDescription() ? descriptor.description() : value.jarPath().toString());
                applyStatusBadge(statusLabel, resolveInstalledStatus(value));
            }

            applySelectionColors(list, isSelected, this, titleLabel, metaLabel, detailLabel, statusLabel);
            return this;
        }
    }

    private final class MarketPluginCellRenderer extends JPanel implements ListCellRenderer<PluginCatalogEntry> {
        private final JLabel titleLabel = new JLabel();
        private final JLabel metaLabel = new JLabel();
        private final JLabel detailLabel = new JLabel();
        private final JLabel statusLabel = createStatusBadgeLabel();

        private MarketPluginCellRenderer() {
            setLayout(new BorderLayout(10, 6));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getBorderLightColor()),
                    new EmptyBorder(10, 12, 10, 12)
            ));
            setOpaque(true);

            JPanel center = new JPanel(new GridLayout(0, 1, 0, 4));
            center.setOpaque(false);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
            metaLabel.setForeground(ModernColors.getTextHint());
            detailLabel.setForeground(ModernColors.getTextSecondary());
            center.add(titleLabel);
            center.add(metaLabel);
            center.add(detailLabel);

            add(center, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PluginCatalogEntry> list, PluginCatalogEntry value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            titleLabel.setText(value.name());
            if (value.isPlaceholder()) {
                metaLabel.setText("");
                detailLabel.setText("");
                applyStatusBadge(statusLabel, "");
            } else {
                metaLabel.setText(value.id() + "  ·  " + value.version());
                detailLabel.setText(value.hasDescription() ? value.description() : value.installUrl());
                applyStatusBadge(statusLabel, getMarketEntryStatus(value));
            }

            applySelectionColors(list, isSelected, this, titleLabel, metaLabel, detailLabel, statusLabel);
            return this;
        }
    }

    private static void applySelectionColors(JList<?> list, boolean isSelected, JPanel panel,
                                             JLabel titleLabel, JLabel metaLabel, JLabel detailLabel, JLabel statusLabel) {
        if (isSelected) {
            panel.setBackground(list.getSelectionBackground());
            titleLabel.setForeground(list.getSelectionForeground());
            metaLabel.setForeground(list.getSelectionForeground());
            detailLabel.setForeground(list.getSelectionForeground());
            return;
        }
        panel.setBackground(list.getBackground());
        titleLabel.setForeground(ModernColors.getTextPrimary());
        metaLabel.setForeground(ModernColors.getTextHint());
        detailLabel.setForeground(ModernColors.getTextSecondary());
        if (statusLabel.isVisible()) {
            statusLabel.setForeground(statusLabel.getForeground());
        }
    }
}
