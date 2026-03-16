package com.laker.postman.panel.topmenu.plugin;

import com.laker.postman.plugin.api.PluginDescriptor;
import com.laker.postman.plugin.manager.PluginManagementService;
import com.laker.postman.plugin.manager.PluginUninstallResult;
import com.laker.postman.plugin.manager.market.PluginCatalogEntry;
import com.laker.postman.plugin.runtime.PluginFileInfo;
import com.laker.postman.service.update.version.VersionComparator;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
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
 * 插件管理对话框。
 */
@Slf4j
public class PluginManagerDialog extends JDialog {

    private final DefaultListModel<PluginFileInfo> installedListModel = new DefaultListModel<>();
    private final JList<PluginFileInfo> installedList = new JList<>(installedListModel);
    private final DefaultListModel<PluginCatalogEntry> marketListModel = new DefaultListModel<>();
    private final JList<PluginCatalogEntry> marketList = new JList<>(marketListModel);
    private final JLabel directoryLabel = new JLabel();
    private final JLabel cacheDirectoryLabel = new JLabel();
    private final JTextField catalogUrlField = new JTextField();
    private final JLabel marketStatusLabel = new JLabel();
    private final JButton enableInstalledButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_ENABLE));
    private final JButton disableInstalledButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DISABLE));
    private final JButton uninstallInstalledButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_UNINSTALL));
    private final JButton loadCatalogButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOAD));
    private final JButton installMarketButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALL));
    private final JButton openHomepageButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_OPEN_HOMEPAGE));
    private Map<String, PluginFileInfo> installedPluginMap = Map.of();
    private boolean marketBusy;

    private PluginManagerDialog(Window owner) {
        super(owner, I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_TITLE), ModalityType.APPLICATION_MODAL);
        initUI();
        reloadPlugins();
        loadSavedCatalogIfPresent();
    }

    public static void showDialog(Window owner) {
        PluginManagerDialog dialog = new PluginManagerDialog(owner);
        dialog.setVisible(true);
    }

    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(860, 580));
        setSize(920, 620);
        setLocationRelativeTo(getOwner());

        JPanel content = new JPanel(new MigLayout(
                "fill, insets 12, gap 10, novisualpadding",
                "[grow,fill]",
                "[grow,fill][]"
        ));
        setContentPane(content);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_TAB_INSTALLED), createInstalledPanel());
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_TAB_MARKET), createMarketPanel());
        content.add(tabbedPane, "grow, push, wrap");

        JButton closeButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE));
        closeButton.addActionListener(e -> dispose());
        JPanel footer = new JPanel(new MigLayout("insets 0, fillx, novisualpadding", "[grow,fill][]", "[]"));
        footer.add(new JLabel(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_RESTART_HINT)), "growx");
        footer.add(closeButton, "alignx right");
        content.add(footer, "growx");

        installedList.setCellRenderer(new InstalledPluginCellRenderer());
        installedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        installedList.addListSelectionListener(e -> updateInstalledActions());

        marketList.setCellRenderer(new MarketPluginCellRenderer());
        marketList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        marketList.addListSelectionListener(e -> updateMarketActions());
    }

    private JPanel createInstalledPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "fill, insets 0, gap 8, novisualpadding",
                "[grow,fill]",
                "[][][][grow,fill]"
        ));

        JPanel header = new JPanel(new MigLayout(
                "insets 0, fillx, gap 8, novisualpadding",
                "[grow,fill][][][][][][]",
                "[]"
        ));
        directoryLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
        header.add(directoryLabel, "growx");

        JButton openDirButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OPEN_FOLDER));
        JButton installLocalButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_INSTALL));
        JButton refreshButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_REFRESH));
        enableInstalledButton.addActionListener(e -> toggleSelectedInstalledPlugin(true));
        disableInstalledButton.addActionListener(e -> toggleSelectedInstalledPlugin(false));
        uninstallInstalledButton.addActionListener(e -> uninstallSelectedInstalledPlugin());
        openDirButton.addActionListener(e -> openManagedPluginDirectory());
        installLocalButton.addActionListener(e -> installLocalPluginJar());
        refreshButton.addActionListener(e -> reloadPlugins());
        header.add(openDirButton);
        header.add(installLocalButton);
        header.add(enableInstalledButton);
        header.add(disableInstalledButton);
        header.add(uninstallInstalledButton);
        header.add(refreshButton);

        cacheDirectoryLabel.setBorder(new EmptyBorder(0, 2, 0, 2));

        JTextArea hintArea = new JTextArea(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_LOCAL_HINT));
        hintArea.setEditable(false);
        hintArea.setOpaque(false);
        hintArea.setLineWrap(true);
        hintArea.setWrapStyleWord(true);
        hintArea.setBorder(new EmptyBorder(0, 2, 0, 2));

        JScrollPane scrollPane = new JScrollPane(installedList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(header, "growx, wrap");
        panel.add(cacheDirectoryLabel, "growx, wrap");
        panel.add(hintArea, "growx, wrap");
        panel.add(scrollPane, "grow, push");
        return panel;
    }

    private JPanel createMarketPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "fill, insets 0, gap 8, novisualpadding",
                "[right][grow,fill][][][][]",
                "[][grow,fill][]"
        ));

        JLabel catalogLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_CATALOG_URL));
        panel.add(catalogLabel);
        panel.add(catalogUrlField, "growx");

        JButton browseCatalogButton = new JButton(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_BROWSE_CATALOG));
        loadCatalogButton.addActionListener(e -> loadCatalog());
        installMarketButton.addActionListener(e -> installSelectedCatalogPlugin());
        openHomepageButton.addActionListener(e -> openSelectedPluginHomepage());
        browseCatalogButton.addActionListener(e -> chooseCatalogLocation());
        panel.add(browseCatalogButton);
        panel.add(loadCatalogButton);
        panel.add(installMarketButton);
        panel.add(openHomepageButton, "wrap");

        JScrollPane scrollPane = new JScrollPane(marketList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, "span 5, grow, push, wrap");

        marketStatusLabel.setBorder(new EmptyBorder(0, 2, 0, 2));
        panel.add(marketStatusLabel, "span 5, growx");
        return panel;
    }

    private void loadSavedCatalogIfPresent() {
        String catalogUrl = PluginManagementService.getCatalogUrl();
        catalogUrlField.setText(catalogUrl);
        if (catalogUrl == null || catalogUrl.isBlank()) {
            setMarketPlaceholder(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_HINT));
            setMarketStatus(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_HINT));
            updateMarketActions();
            return;
        }
        loadCatalog();
    }

    private void reloadPlugins() {
        Path pluginDir = PluginManagementService.getManagedPluginDir();
        directoryLabel.setText(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_DIRECTORY, pluginDir));
        cacheDirectoryLabel.setText(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_CACHE_DIRECTORY,
                PluginManagementService.getPluginCacheDir()));

        installedListModel.clear();
        List<PluginFileInfo> plugins = PluginManagementService.getInstalledPlugins();
        installedPluginMap = buildInstalledPluginMap(plugins);
        if (plugins.isEmpty()) {
            installedListModel.addElement(new PluginFileInfo(
                    new PluginDescriptor("empty", I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_EMPTY), "", "", "", ""),
                    pluginDir,
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
            installedList.setSelectedIndex(0);
        }
        marketList.repaint();
        updateInstalledActions();
        updateMarketActions();
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
        reloadPlugins();
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
            reloadPlugins();
            return;
        }
        if (result.restartRequired()) {
            showInfo(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_UNINSTALL_SCHEDULED, selected.descriptor().name()));
            reloadPlugins();
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
            reloadPlugins();
        } catch (IOException | IllegalArgumentException e) {
            log.error("Failed to install plugin jar: {}", source, e);
            showError(e);
        }
    }

    private void chooseCatalogLocation() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_CATALOG_FILE_CHOOSER));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(new FileNameExtensionFilter("JSON", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path selectedPath = chooser.getSelectedFile().toPath();
        if (selectedPath.toFile().isDirectory()) {
            selectedPath = selectedPath.resolve("catalog.json");
        }
        if (!selectedPath.toFile().isFile()) {
            showError(new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_CATALOG_MISSING)));
            return;
        }
        String normalizedLocation = PluginManagementService.normalizeCatalogLocation(selectedPath.toString());
        catalogUrlField.setText(normalizedLocation);
    }

    private void loadCatalog() {
        String rawCatalogUrl = catalogUrlField.getText().trim();
        String catalogUrl = PluginManagementService.normalizeCatalogLocation(rawCatalogUrl);
        PluginManagementService.saveCatalogUrl(catalogUrl);
        catalogUrlField.setText(catalogUrl);
        if (catalogUrl.isBlank()) {
            setMarketPlaceholder(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_HINT));
            setMarketStatus(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_HINT));
            updateMarketActions();
            return;
        }

        setMarketBusy(true, I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOADING));
        new SwingWorker<List<PluginCatalogEntry>, Void>() {
            @Override
            protected List<PluginCatalogEntry> doInBackground() throws Exception {
                return PluginManagementService.loadCatalog(catalogUrl);
            }

            @Override
            protected void done() {
                try {
                    applyMarketEntries(get());
                } catch (Exception e) {
                    log.error("Failed to load plugin catalog: {}", catalogUrl, e);
                    setMarketPlaceholder(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOAD_FAILED));
                    setMarketStatus(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOAD_FAILED));
                    showError(e);
                } finally {
                    setMarketBusy(false, marketStatusLabel.getText());
                    updateMarketActions();
                }
            }
        }.execute();
    }

    private void applyMarketEntries(List<PluginCatalogEntry> entries) {
        marketListModel.clear();
        if (entries == null || entries.isEmpty()) {
            setMarketPlaceholder(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_EMPTY));
            setMarketStatus(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_EMPTY));
            return;
        }
        for (PluginCatalogEntry entry : entries) {
            marketListModel.addElement(entry);
        }
        marketList.setSelectedIndex(0);
        setMarketStatus(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_HINT));
    }

    private void setMarketPlaceholder(String message) {
        marketListModel.clear();
        marketListModel.addElement(new PluginCatalogEntry("empty", message, "", "", "", "", ""));
        marketList.setSelectedIndex(0);
    }

    private void installSelectedCatalogPlugin() {
        PluginCatalogEntry selected = marketList.getSelectedValue();
        if (selected == null || selected.isPlaceholder()) {
            setMarketStatus(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_NO_SELECTION));
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
                    reloadPlugins();
                    marketList.repaint();
                    setMarketStatus(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALL_SUCCESS, installed.jarPath()));
                    showInfo(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_INSTALL_SUCCESS, installed.jarPath()));
                } catch (Exception e) {
                    log.error("Failed to install plugin from catalog: {}", selected.id(), e);
                    setMarketStatus(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALL_FAILED));
                    showError(e);
                } finally {
                    setMarketBusy(false, marketStatusLabel.getText());
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

    private void updateMarketActions() {
        PluginCatalogEntry selected = marketList.getSelectedValue();
        boolean validSelection = selected != null && !selected.isPlaceholder();
        loadCatalogButton.setEnabled(!marketBusy);
        installMarketButton.setEnabled(!marketBusy && validSelection);
        openHomepageButton.setEnabled(!marketBusy && validSelection && selected.hasHomepageUrl());
        catalogUrlField.setEnabled(!marketBusy);
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

    private void setMarketBusy(boolean busy, String message) {
        marketBusy = busy;
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        setMarketStatus(message);
        updateMarketActions();
    }

    private void setMarketStatus(String message) {
        marketStatusLabel.setText(message == null ? "" : message);
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
        if (!installed.loaded()) {
            return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_RESTART_REQUIRED);
        }
        return I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALLED, installed.descriptor().version());
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static final class InstalledPluginCellRenderer extends JPanel implements ListCellRenderer<PluginFileInfo> {
        private final JLabel titleLabel = new JLabel();
        private final JLabel detailLabel = new JLabel();
        private final JLabel statusLabel = new JLabel();

        private InstalledPluginCellRenderer() {
            setLayout(new BorderLayout(8, 4));
            setBorder(new EmptyBorder(8, 10, 8, 10));
            setOpaque(true);

            JPanel center = new JPanel(new GridLayout(0, 1, 0, 4));
            center.setOpaque(false);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            detailLabel.setFont(detailLabel.getFont().deriveFont(Font.PLAIN, 12f));
            center.add(titleLabel);
            center.add(detailLabel);

            statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            add(center, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PluginFileInfo> list, PluginFileInfo value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            boolean isPlaceholder = "empty".equals(value.descriptor().id());
            titleLabel.setText(value.descriptor().name());

            if (isPlaceholder) {
                detailLabel.setText(escapeHtml(value.jarPath().toString()));
                statusLabel.setText("");
            } else {
                PluginDescriptor descriptor = value.descriptor();
                String compatibility = buildCompatibilityText(descriptor, value.compatible());
                String description = descriptor.hasDescription()
                        ? "<br>" + escapeHtml(descriptor.description())
                        : "";
                detailLabel.setText("<html><span style='font-family:monospace'>" + escapeHtml(descriptor.id())
                        + "</span>  " + escapeHtml(descriptor.version()) + description
                        + compatibility
                        + "<br>" + escapeHtml(value.jarPath().toString()) + "</html>");
                statusLabel.setText(resolveInstalledStatus(value));
            }

            applySelectionColors(list, isSelected, titleLabel, detailLabel, statusLabel, this);
            return this;
        }
    }

    private static String buildCompatibilityText(PluginDescriptor descriptor, boolean compatible) {
        if (!descriptor.hasMinAppVersion() && !descriptor.hasMaxAppVersion()) {
            return "";
        }
        String range = (descriptor.hasMinAppVersion() ? descriptor.minAppVersion() : "*")
                + " - "
                + (descriptor.hasMaxAppVersion() ? descriptor.maxAppVersion() : "*");
        String current = PluginManagementService.getCurrentAppVersion();
        return "<br>App: " + escapeHtml(range) + " / current " + escapeHtml(current)
                + (compatible ? "" : " <b>(" + escapeHtml(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_INCOMPATIBLE)) + ")</b>");
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

    private final class MarketPluginCellRenderer extends JPanel implements ListCellRenderer<PluginCatalogEntry> {
        private final JLabel titleLabel = new JLabel();
        private final JLabel detailLabel = new JLabel();
        private final JLabel statusLabel = new JLabel();

        private MarketPluginCellRenderer() {
            setLayout(new BorderLayout(8, 4));
            setBorder(new EmptyBorder(8, 10, 8, 10));
            setOpaque(true);

            JPanel center = new JPanel(new GridLayout(0, 1, 0, 4));
            center.setOpaque(false);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            detailLabel.setFont(detailLabel.getFont().deriveFont(Font.PLAIN, 12f));
            center.add(titleLabel);
            center.add(detailLabel);

            statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            add(center, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PluginCatalogEntry> list, PluginCatalogEntry value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            titleLabel.setText(value.name());
            if (value.isPlaceholder()) {
                detailLabel.setText("");
                statusLabel.setText("");
            } else {
                String description = value.hasDescription()
                        ? "<br>" + escapeHtml(value.description())
                        : "";
                detailLabel.setText("<html><span style='font-family:monospace'>" + escapeHtml(value.id())
                        + "</span>  " + escapeHtml(value.version()) + description
                        + "<br>" + escapeHtml(value.downloadUrl()) + "</html>");
                statusLabel.setText(getMarketEntryStatus(value));
            }

            applySelectionColors(list, isSelected, titleLabel, detailLabel, statusLabel, this);
            return this;
        }
    }

    private static void applySelectionColors(JList<?> list, boolean isSelected, JLabel titleLabel, JLabel detailLabel,
                                             JLabel statusLabel, JPanel panel) {
        if (isSelected) {
            panel.setBackground(list.getSelectionBackground());
            titleLabel.setForeground(list.getSelectionForeground());
            detailLabel.setForeground(list.getSelectionForeground());
            statusLabel.setForeground(list.getSelectionForeground());
            return;
        }
        panel.setBackground(list.getBackground());
        titleLabel.setForeground(list.getForeground());
        detailLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    }
}
