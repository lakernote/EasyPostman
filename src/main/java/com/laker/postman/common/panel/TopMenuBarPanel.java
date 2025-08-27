package com.laker.postman.common.panel;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatDesktop;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.combobox.EnvironmentComboBox;
import com.laker.postman.common.dialog.ExitDialog;
import com.laker.postman.common.setting.SettingDialog;
import com.laker.postman.panel.sidebar.SidebarTabPanel;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static com.laker.postman.util.SystemUtil.getCurrentVersion;

@Slf4j
public class TopMenuBarPanel extends SingletonBasePanel {
    @Getter
    private EnvironmentComboBox environmentComboBox;

    private JMenuBar menuBar;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(createPanelBorder());
        setOpaque(true);
        initComponents();
    }

    private Border createPanelBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, Color.lightGray),
                BorderFactory.createEmptyBorder(1, 4, 1, 4)
        );
    }

    @Override
    protected void registerListeners() {
        FlatDesktop.setAboutHandler(this::aboutActionPerformed);
        FlatDesktop.setQuitHandler((e) -> ExitDialog.show());
    }

    private void initComponents() {
        menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEmptyBorder());
        addFileMenu();
        addThemeMenu();
        addLanguageMenu();
        addSettingMenu();
        addHelpMenu();
        addAboutMenu();
        add(menuBar, BorderLayout.WEST);
        addEnvironmentComboBox();
    }

    private void addFileMenu() {
        JMenu fileMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_FILE));
        JMenuItem logMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG));
        logMenuItem.addActionListener(e -> openLogDirectory());
        JMenuItem exitMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_EXIT));
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitMenuItem.setMnemonic('X');
        exitMenuItem.addActionListener(e -> ExitDialog.show());
        fileMenu.add(logMenuItem);
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
    }

    private void openLogDirectory() {
        try {
            Desktop.getDesktop().open(new File(SystemUtil.LOG_DIR));
        } catch (IOException ex) {
            log.error("Failed to open log directory", ex);
            JOptionPane.showMessageDialog(null,
                    I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LOG_MESSAGE),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addThemeMenu() {
        JMenu themeMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_THEME));
        ButtonGroup themeGroup = new ButtonGroup();
        JRadioButtonMenuItem lightTheme = new JRadioButtonMenuItem(I18nUtil.getMessage(MessageKeys.MENU_THEME_LIGHT));
        JRadioButtonMenuItem intellijTheme = new JRadioButtonMenuItem(I18nUtil.getMessage(MessageKeys.MENU_THEME_INTELLIJ));
        JRadioButtonMenuItem macLightTheme = new JRadioButtonMenuItem(I18nUtil.getMessage(MessageKeys.MENU_THEME_MAC));
        themeGroup.add(lightTheme);
        themeGroup.add(intellijTheme);
        themeGroup.add(macLightTheme);
        themeMenu.add(lightTheme);
        themeMenu.add(intellijTheme);
        themeMenu.add(macLightTheme);
        setThemeSelection(lightTheme, intellijTheme, macLightTheme);
        lightTheme.addActionListener(e -> switchLaf("com.formdev.flatlaf.FlatLightLaf"));
        intellijTheme.addActionListener(e -> switchLaf("com.formdev.flatlaf.FlatIntelliJLaf"));
        macLightTheme.addActionListener(e -> switchLaf("com.formdev.flatlaf.themes.FlatMacLightLaf"));
        menuBar.add(themeMenu);
    }

    private void setThemeSelection(JRadioButtonMenuItem lightTheme, JRadioButtonMenuItem intellijTheme, JRadioButtonMenuItem macLightTheme) {
        String lafClass = UIManager.getLookAndFeel().getClass().getName();
        switch (lafClass) {
            case "com.formdev.flatlaf.FlatLightLaf" -> lightTheme.setSelected(true);
            case "com.formdev.flatlaf.themes.FlatMacLightLaf" -> macLightTheme.setSelected(true);
            default -> intellijTheme.setSelected(true);
        }
    }

    private void addLanguageMenu() {
        JMenu languageMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_LANGUAGE));
        ButtonGroup languageGroup = new ButtonGroup();

        JRadioButtonMenuItem englishItem = new JRadioButtonMenuItem("English");
        JRadioButtonMenuItem chineseItem = new JRadioButtonMenuItem("中文");

        languageGroup.add(englishItem);
        languageGroup.add(chineseItem);

        // 设置当前选中的语言
        if (I18nUtil.isChinese()) {
            chineseItem.setSelected(true);
        } else {
            englishItem.setSelected(true);
        }

        englishItem.addActionListener(e -> switchLanguage("en"));
        chineseItem.addActionListener(e -> switchLanguage("zh"));

        languageMenu.add(englishItem);
        languageMenu.add(chineseItem);
        menuBar.add(languageMenu);
    }

    private void switchLanguage(String languageCode) {
        I18nUtil.setLocale(languageCode);
        // 重新初始化菜单栏以应用新语言
        menuBar.removeAll();
        initComponents();
        // 重新绘制所有窗口
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
        // 通知 SidebarTabPanel 刷新国际化
        try {
            SidebarTabPanel sidebar = SingletonFactory.getInstance(SidebarTabPanel.class);
            sidebar.reloadI18n();
        } catch (Exception e) {
            log.warn("SidebarTabPanel reloadI18n failed", e);
        }
        JOptionPane.showMessageDialog(null,
                I18nUtil.getMessage(MessageKeys.LANGUAGE_CHANGED),
                I18nUtil.getMessage(MessageKeys.GENERAL_INFO),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void addSettingMenu() {
        JMenu settingMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_SETTINGS));
        JMenuItem settingMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_SETTINGS_GLOBAL));
        settingMenuItem.addActionListener(e -> showSettingDialog());
        settingMenu.add(settingMenuItem);
        menuBar.add(settingMenu);
    }

    private void showSettingDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        SettingDialog dialog = new SettingDialog(window);
        dialog.setVisible(true);
    }

    private void addHelpMenu() {
        JMenu helpMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_HELP));
        JMenuItem updateMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_UPDATE));
        updateMenuItem.addActionListener(e -> checkUpdate());
        JMenuItem feedbackMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_FEEDBACK));
        feedbackMenuItem.addActionListener(e -> showFeedbackDialog());
        helpMenu.add(updateMenuItem);
        helpMenu.add(feedbackMenuItem);
        menuBar.add(helpMenu);
    }

    private void showFeedbackDialog() {
        JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.FEEDBACK_MESSAGE),
                I18nUtil.getMessage(MessageKeys.FEEDBACK_TITLE), JOptionPane.INFORMATION_MESSAGE);
    }

    private void addAboutMenu() {
        JMenu aboutMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_ABOUT));
        JMenuItem aboutMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_ABOUT_EASYPOSTMAN));
        aboutMenuItem.addActionListener(e -> aboutActionPerformed());
        aboutMenu.add(aboutMenuItem);
        menuBar.add(aboutMenu);
    }

    private void addEnvironmentComboBox() {
        if (environmentComboBox == null) {
            environmentComboBox = new EnvironmentComboBox();
        } else {
            environmentComboBox.reload();
        }
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(environmentComboBox);
        add(rightPanel, BorderLayout.EAST);
    }

    private void aboutActionPerformed() {
        String iconUrl = getClass().getResource("/icons/icon.png") + "";
        String html = "<html>"
                + "<head>"
                + "<div style='border-radius:16px; border:1px solid #e0e0e0; padding:20px 28px; min-width:340px; max-width:420px;'>"
                + "<div style='text-align:center;'>"
                + "<img src='" + iconUrl + "' width='56' height='56' style='margin-bottom:10px;'/>"
                + "</div>"
                + "<div style='font-size:16px; font-weight:bold; color:#212529; text-align:center; margin-bottom:6px;'>EasyPostman</div>"
                + "<div style='font-size:12px; color:#666; text-align:center; margin-bottom:12px;>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_VERSION, getCurrentVersion()) + "</div>"
                + "<div style='font-size:10px; color:#444; margin-bottom:2px;>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_AUTHOR) + "</div>"
                + "<div style='font-size:10px; color:#444; margin-bottom:2px;>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_LICENSE) + "</div>"
                + "<div style='font-size:10px; color:#444; margin-bottom:8px;>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_WECHAT) + "</div>"
                + "<hr style='border:none; border-top:1px solid #eee; margin:10px 0;'>"
                + "<div style='font-size:9px; margin-bottom:2px;'>"
                + "<a href='https://laker.blog.csdn.net' style='color:#1a0dab; text-decoration:none;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_BLOG) + "</a>"
                + "</div>"
                + "<div style='font-size:9px; margin-bottom:2px;'>"
                + "<a href='https://github.com/lakernote' style='color:#1a0dab; text-decoration:none;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_GITHUB) + "</a>"
                + "</div>"
                + "<div style='font-size:9px;'>"
                + "<a href='https://gitee.com/lakernote' style='color:#1a0dab; text-decoration:none;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_GITEE) + "</a>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
        JEditorPane editorPane = getJEditorPane(html);
        JOptionPane.showMessageDialog(null, editorPane, I18nUtil.getMessage(MessageKeys.MENU_ABOUT_EASYPOSTMAN), JOptionPane.PLAIN_MESSAGE);
    }

    private static JEditorPane getJEditorPane(String html) {
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, e.getURL()),
                            I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        // 直接用JEditorPane，不用滚动条，且自适应高度
        editorPane.setPreferredSize(new Dimension(310, 310));
        return editorPane;
    }

    /**
     * 检查更新：访问 Gitee Release API，获取最新版本号并与本地对比。
     */
    private void checkUpdate() {
        // 显示正在检查更新的对话框
        final JDialog loadingDialog = new JDialog((Frame) null, I18nUtil.getMessage(MessageKeys.MENU_HELP_UPDATE), true);
        loadingDialog.setResizable(false);
        JLabel loadingLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_CHECKING), SwingConstants.CENTER);
        loadingLabel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        loadingDialog.getContentPane().add(loadingLabel);
        loadingDialog.pack();
        loadingDialog.setSize(320, 120);
        loadingDialog.setLocationRelativeTo(null);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String latestVersion = null;
            final String releaseUrl = "https://gitee.com/lakernote/easy-postman/releases";
            String errorMsg = null;
            JSONObject latestReleaseJson = null;

            @Override
            protected Void doInBackground() {
                try {
                    URL url = new URL("https://gitee.com/api/v5/repos/lakernote/easy-postman/releases/latest");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        try (InputStream is = conn.getInputStream();
                             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
                            String json = scanner.useDelimiter("\\A").next();
                            latestReleaseJson = new JSONObject(json);
                            latestVersion = latestReleaseJson.getStr("tag_name");
                        }
                    } else {
                        errorMsg = I18nUtil.getMessage(MessageKeys.ERROR_NETWORK, code);
                    }
                } catch (Exception ex) {
                    errorMsg = I18nUtil.getMessage(MessageKeys.ERROR_UPDATE_FAILED, ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                String currentVersion = getCurrentVersion();
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(null, errorMsg, I18nUtil.getMessage(MessageKeys.MENU_HELP_UPDATE), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (latestVersion == null) {
                    JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.ERROR_NO_VERSION_INFO),
                            I18nUtil.getMessage(MessageKeys.MENU_HELP_UPDATE), JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (compareVersion(latestVersion, currentVersion) <= 0) {
                    JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.UPDATE_LATEST_VERSION, currentVersion),
                            I18nUtil.getMessage(MessageKeys.MENU_HELP_UPDATE), JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                // 新版本弹窗
                Object[] options = {
                        I18nUtil.getMessage(MessageKeys.UPDATE_MANUAL_DOWNLOAD),
                        I18nUtil.getMessage(MessageKeys.UPDATE_AUTO_DOWNLOAD),
                        I18nUtil.getMessage(MessageKeys.UPDATE_CANCEL)
                };
                int r = JOptionPane.showOptionDialog(null,
                        I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_FOUND, latestVersion),
                        I18nUtil.getMessage(MessageKeys.MENU_HELP_UPDATE),
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[1]);
                if (r == 0) {
                    try {
                        Desktop.getDesktop().browse(new URI(releaseUrl));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, ex.getMessage()),
                                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
                    }
                } else if (r == 1) {
                    // 自动下载并安装
                    startDownloadWithProgress(latestReleaseJson);
                }
            }
        };
        worker.execute();
        SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));
    }

    /**
     * 自动下载最新安装包并显示进度弹窗，支持取消
     */
    private void startDownloadWithProgress(JSONObject latestReleaseJson) {
        JSONArray assets = latestReleaseJson.getJSONArray("assets");
        String installerUrl = null;
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            for (int i = 0; i < assets.size(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.getStr("name");
                if (name != null && name.endsWith(".msi")) {
                    installerUrl = asset.getStr("browser_download_url");
                    break;
                }
            }
        } else {
            for (int i = 0; i < assets.size(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.getStr("name");
                if (name != null && name.endsWith(".dmg")) {
                    installerUrl = asset.getStr("browser_download_url");
                    break;
                }
            }
        }
        if (installerUrl == null) {
            JOptionPane.showMessageDialog(null, I18nUtil.getMessage("update.no_installer_found"),
                    I18nUtil.getMessage("update.downloading"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 优化下载弹窗UI，增加图标、剩余时间、重试按钮
        JDialog downloadingDialog = new JDialog((Frame) null, I18nUtil.getMessage("update.downloading"), true);
        downloadingDialog.setResizable(false);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));
        panel.setBackground(new Color(245, 247, 250));
        panel.setOpaque(true);
        // 图标
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/download.svg"));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(8));
        // 状态提示
        JLabel statusLabel = new JLabel(I18nUtil.getMessage("update.connecting"), SwingConstants.CENTER);
        statusLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 16));
        statusLabel.setForeground(new Color(33, 37, 41));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(12));
        // 进度条
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(320, 32));
        progressBar.setMaximumSize(new Dimension(320, 32));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setBackground(new Color(240, 242, 245));
        progressBar.setForeground(new Color(33, 150, 243));
        panel.add(progressBar);
        // 三行信息：下载进度、下载速度、预估时间（布局优化，防止晃动）
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel sizeLabel = createInfoLabel("-- / -- MB");
        infoPanel.add(sizeLabel, gbc);
        JLabel speedLabel = createInfoLabel("-- KB/s");
        infoPanel.add(speedLabel, gbc);
        JLabel timeLabel = createInfoLabel("-- s");
        infoPanel.add(timeLabel, gbc);
        panel.add(Box.createVerticalStrut(8));
        panel.add(infoPanel);
        // 取消和重试按钮
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnPanel.setOpaque(false);
        JButton cancelButton = new JButton(I18nUtil.getMessage("update.cancel_download"));
        cancelButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 14));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton retryButton = new JButton(I18nUtil.getMessage("update.retry"));
        retryButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 14));
        retryButton.setFocusPainted(false);
        retryButton.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        retryButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        retryButton.setVisible(false);
        btnPanel.add(cancelButton);
        btnPanel.add(retryButton);
        panel.add(Box.createVerticalStrut(18));
        panel.add(btnPanel);
        downloadingDialog.getContentPane().add(panel);
        downloadingDialog.pack();
        downloadingDialog.setSize(420, 320);
        downloadingDialog.setLocationRelativeTo(null);
        final boolean[] cancelFlag = {false};
        cancelButton.addActionListener(e -> {
            cancelFlag[0] = true;
            downloadingDialog.dispose();
        });
        retryButton.addActionListener(e -> {
            downloadingDialog.dispose();
            startDownloadWithProgress(latestReleaseJson);
        });
        String finalInstallerUrl = installerUrl;
        SwingWorker<Void, Integer> downloadWorker = new SwingWorker<>() {
            String error = null;
            File downloadedFile = null;
            long lastTime = 0;
            long lastDownloaded = 0;
            int totalSize = 0;
            long startTime = 0;

            @Override
            protected Void doInBackground() {
                try {
                    statusLabel.setText(I18nUtil.getMessage("update.connecting"));
                    URL downloadUrl = new URL(finalInstallerUrl);
                    String fileName = finalInstallerUrl.substring(finalInstallerUrl.lastIndexOf('/') + 1);
                    File tempFile = File.createTempFile("EasyPostman-", fileName);
                    HttpURLConnection downloadConn = (HttpURLConnection) downloadUrl.openConnection();
                    totalSize = downloadConn.getContentLength();
                    if (totalSize <= 0) {
                        SwingUtilities.invokeLater(() -> progressBar.setIndeterminate(true));
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setIndeterminate(false);
                            progressBar.setMaximum(100);
                        });
                    }
                    downloadConn.setConnectTimeout(5000);
                    downloadConn.setReadTimeout(5000);
                    try {
                        InputStream in = null;
                        FileOutputStream out = null;
                        try {
                            in = downloadConn.getInputStream();
                            out = new FileOutputStream(tempFile);
                            byte[] buf = new byte[102400];
                            int len;
                            int downloaded = 0;
                            int lastPercent = 0;
                            lastTime = System.currentTimeMillis();
                            lastDownloaded = 0;
                            startTime = System.currentTimeMillis();
                            statusLabel.setText(I18nUtil.getMessage("update.downloading"));
                            // 优化：只在内容变化时刷新，避免乱晃
                            String lastSizeStr = "";
                            String lastSpeedStr = "";
                            int lastRemainSec = -1;
                            while ((len = in.read(buf)) != -1) {
                                if (cancelFlag[0]) break;
                                out.write(buf, 0, len);
                                downloaded += len;
                                long now = System.currentTimeMillis();
                                double elapsedSec = (now - startTime) / 1000.0;
                                double speed = elapsedSec > 0 ? downloaded / elapsedSec : 0; // bytes/sec
                                // 定长字符串，始终补齐空格，防止乱晃
                                String sizeStr;
                                if (downloaded == 0 && totalSize == 0) {
                                    sizeStr = I18nUtil.getMessage("update.download_progress", "--", "-- MB");
                                } else {
                                    sizeStr = I18nUtil.getMessage("update.download_progress",
                                            String.format("%.1f", downloaded / 1024.0 / 1024),
                                            String.format("%.1f", totalSize / 1024.0 / 1024));
                                }
                                String speedStr = I18nUtil.getMessage("update.download_speed", String.format("%.1f", speed / 1024));
                                int remainSec;
                                String timeStr;
                                if (speed > 0 && totalSize > 0) {
                                    remainSec = (int) ((totalSize - downloaded) / speed);
                                    timeStr = I18nUtil.getMessage("update.estimated_time", remainSec);
                                } else {
                                    remainSec = -1;
                                    timeStr = I18nUtil.getMessage("update.estimated_time", "--");
                                }
                                // 只有内容变化时才刷新，避免乱晃
                                String finalLastSizeStr = lastSizeStr;
                                String finalLastSpeedStr = lastSpeedStr;
                                int finalLastRemainSec = lastRemainSec;
                                SwingUtilities.invokeLater(() -> {
                                    if (!sizeStr.equals(finalLastSizeStr)) {
                                        sizeLabel.setText(sizeStr);
                                    }
                                    if (!speedStr.equals(finalLastSpeedStr)) {
                                        speedLabel.setText(speedStr);
                                    }
                                    if (remainSec != finalLastRemainSec) {
                                        timeLabel.setText(timeStr);
                                    }
                                });
                                lastSizeStr = sizeStr;
                                lastSpeedStr = speedStr;
                                lastRemainSec = remainSec;
                                lastTime = now;
                                lastDownloaded = downloaded;
                                if (totalSize > 0) {
                                    int percent = (int) ((downloaded * 100L) / totalSize);
                                    if (percent != lastPercent) {
                                        publish(percent);
                                        lastPercent = percent;
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            error = getFriendlyError(ex);
                        } finally {
                            if (in != null) try {
                                in.close();
                            } catch (IOException ignore) {
                            }
                            if (out != null) try {
                                out.close();
                            } catch (IOException ignore) {
                            }
                        }
                        if (!cancelFlag[0]) {
                            downloadedFile = tempFile;
                        }
                    } catch (Exception ex) {
                        error = getFriendlyError(ex);
                    }
                } catch (Exception ex) {
                    error = getFriendlyError(ex);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int percent = chunks.get(chunks.size() - 1);
                    SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
                }
            }

            @Override
            protected void done() {
                downloadingDialog.dispose();
                if (cancelFlag[0]) {
                    JOptionPane.showMessageDialog(null, I18nUtil.getMessage("update.download_cancelled"),
                            I18nUtil.getMessage("update.downloading"), JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (error != null) {
                    retryButton.setVisible(true);
                    JOptionPane.showMessageDialog(null, error, I18nUtil.getMessage("update.downloading"), JOptionPane.ERROR_MESSAGE);
                } else if (downloadedFile != null) {
                    String tip = I18nUtil.getMessage("update.install_prompt");
                    int open = JOptionPane.showConfirmDialog(null, tip, I18nUtil.getMessage("update.downloading"), JOptionPane.YES_NO_OPTION);
                    if (open == JOptionPane.YES_OPTION) {
                        try {
                            Desktop.getDesktop().open(downloadedFile);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, I18nUtil.getMessage("update.open_installer_failed", ex.getMessage()),
                                    I18nUtil.getMessage("update.downloading"), JOptionPane.ERROR_MESSAGE);
                        }
                        System.exit(0);
                    }
                }
            }
        };
        downloadWorker.execute();
        SwingUtilities.invokeLater(() -> downloadingDialog.setVisible(true));
    }

    // 错误友好提示
    private String getFriendlyError(Exception ex) {
        if (ex instanceof java.net.SocketTimeoutException) {
            return I18nUtil.getMessage("error.network_timeout");
        } else if (ex instanceof java.net.UnknownHostException) {
            return I18nUtil.getMessage("error.server_unreachable");
        } else if (ex instanceof java.io.FileNotFoundException) {
            return I18nUtil.getMessage("error.invalid_download_link");
        } else if (ex instanceof java.io.IOException) {
            if (ex.getMessage() != null && ex.getMessage().contains("No space left on device")) {
                return I18nUtil.getMessage("error.disk_space_insufficient");
            } else if (ex.getMessage() != null && ex.getMessage().contains("Permission denied")) {
                return I18nUtil.getMessage("error.permission_denied");
            }
            return I18nUtil.getMessage("error.io_exception", ex.getMessage());
        }
        return I18nUtil.getMessage("update.download_failed", ex.getMessage());
    }

    /**
     * 比较两个版本号字符串，返回-1表示v1小于v2，0表示相等，1表示v1大于v2。
     */
    private int compareVersion(String v1, String v2) {
        if (v1 == null || v2 == null) return 0;
        String s1 = v1.startsWith("v") ? v1.substring(1) : v1;
        String s2 = v2.startsWith("v") ? v2.substring(1) : v2;
        String[] arr1 = s1.split("\\.");
        String[] arr2 = s2.split("\\.");
        int len = Math.max(arr1.length, arr2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < arr1.length ? parseIntSafe(arr1[i]) : 0;
            int n2 = i < arr2.length ? parseIntSafe(arr2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void switchLaf(String className) {
        try {
            FlatAnimatedLafChange.showSnapshot();
            switch (className) {
                case "com.formdev.flatlaf.FlatLightLaf" -> com.formdev.flatlaf.FlatLightLaf.setup();
                case "com.formdev.flatlaf.FlatIntelliJLaf" -> com.formdev.flatlaf.FlatIntelliJLaf.setup();
                case "com.formdev.flatlaf.themes.FlatMacLightLaf" -> com.formdev.flatlaf.themes.FlatMacLightLaf.setup();
                default -> UIManager.setLookAndFeel(className);
            }
            // 更新全局字体
            Font font = UIManager.getFont("defaultFont");
            UIManager.put("Label.font", font);
            UIManager.put("Button.font", font);
            UIManager.put("TextField.font", font);
            UIManager.put("TextArea.font", font);
            UIManager.put("ComboBox.font", font);
            // 重新初始化菜单栏以应用新主题
            menuBar.removeAll();
            initComponents();
            // 重新绘制所有窗口
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        } catch (Exception e) {
            log.error("Failed to switch LookAndFeel", e);
        }
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 13));
        label.setForeground(new Color(80, 80, 80));
        Dimension infoLabelSize = new Dimension(220, 24);
        label.setPreferredSize(infoLabelSize);
        label.setMinimumSize(infoLabelSize);
        label.setMaximumSize(infoLabelSize);
        return label;
    }
}

