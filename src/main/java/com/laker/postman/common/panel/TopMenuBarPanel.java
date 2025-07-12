package com.laker.postman.common.panel;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatDesktop;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.combobox.EnvironmentComboBox;
import com.laker.postman.common.dialog.ExitDialog;
import com.laker.postman.common.setting.SettingDialog;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
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
public class TopMenuBarPanel extends BasePanel {
    @Getter
    private EnvironmentComboBox environmentComboBox;

    private JMenuBar menuBar;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, Color.lightGray),
                BorderFactory.createEmptyBorder(1, 4, 1, 4)
        ));
        initComponents();
        setBackground(new Color(245, 247, 250));
        setOpaque(true);
    }

    @Override
    protected void registerListeners() {
        FlatDesktop.setAboutHandler(this::aboutActionPerformed);
        FlatDesktop.setQuitHandler((e) -> ExitDialog.show());
    }

    private void initComponents() {
        menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEmptyBorder());
        // ---------文件菜单
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitMenuItem.setMnemonic('X');
        exitMenuItem.addActionListener(e -> ExitDialog.show());
        JMenuItem logMenuItem = new JMenuItem("Log");
        logMenuItem.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(SystemUtil.LOG_DIR));
            } catch (IOException ex) {
                log.error("Failed to open log directory", ex);
                JOptionPane.showMessageDialog(null,
                        "Failed to open log directory. Please check the log.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        fileMenu.add(logMenuItem);
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);

        // ---------主题菜单
        JMenu themeMenu = new JMenu("Theme");
        ButtonGroup themeGroup = new ButtonGroup();
        JRadioButtonMenuItem lightTheme = new JRadioButtonMenuItem("Light (Flat Light)");
        JRadioButtonMenuItem intellijTheme = new JRadioButtonMenuItem("IntelliJ Style");
        JRadioButtonMenuItem macLightTheme = new JRadioButtonMenuItem("Mac Light Style");
        themeGroup.add(lightTheme);
        themeGroup.add(intellijTheme);
        themeGroup.add(macLightTheme);
        themeMenu.add(lightTheme);
        themeMenu.add(intellijTheme);
        themeMenu.add(macLightTheme);
        // 根据当前主题设置默认选中项
        String lafClass = UIManager.getLookAndFeel().getClass().getName();
        switch (lafClass) {
            case "com.formdev.flatlaf.FlatLightLaf" -> lightTheme.setSelected(true);
            case "com.formdev.flatlaf.themes.FlatMacLightLaf" -> macLightTheme.setSelected(true);
            default -> intellijTheme.setSelected(true);
        }
        // 切换主题事件
        lightTheme.addActionListener(e -> switchLaf("com.formdev.flatlaf.FlatLightLaf"));
        intellijTheme.addActionListener(e -> switchLaf("com.formdev.flatlaf.FlatIntelliJLaf"));
        macLightTheme.addActionListener(e -> switchLaf("com.formdev.flatlaf.themes.FlatMacLightLaf"));
        menuBar.add(themeMenu);

        // ---------设置菜单
        JMenu settingMenu = new JMenu("Settings");
        JMenuItem settingMenuItem = new JMenuItem("Global Settings");
        settingMenuItem.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            SettingDialog dialog = new SettingDialog(window);
            dialog.setVisible(true);
        });
        settingMenu.add(settingMenuItem);
        menuBar.add(settingMenu);

        // ---------帮助菜单
        JMenu helpMenu = new JMenu("Help");
        // 新增“检查更新”菜单项
        JMenuItem updateMenuItem = new JMenuItem("Check for Updates");
        updateMenuItem.addActionListener(e -> checkUpdate());
        helpMenu.add(updateMenuItem);
        // 新增“反馈建议”菜单项
        JMenuItem feedbackMenuItem = new JMenuItem("Feedback");
        feedbackMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(null, "Please submit issues via Gitee or GitHub.", "Feedback", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(feedbackMenuItem);
        menuBar.add(helpMenu);

        // ---------关于菜单
        JMenu aboutMenu = new JMenu("About");
        JMenuItem aboutMenuItem = new JMenuItem("About EasyPostman");
        aboutMenuItem.addActionListener(e -> aboutActionPerformed());
        aboutMenu.add(aboutMenuItem);
        menuBar.add(aboutMenu);

        // 菜单栏放左侧
        add(menuBar, BorderLayout.WEST);

        // ---------环境选择器下拉框（右侧）
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
                + "<div style='font-size:12px; color:#666; text-align:center; margin-bottom:12px;'>版本：" + getCurrentVersion() + "</div>"
                + "<div style='font-size:10px; color:#444; margin-bottom:2px;'>作者：lakernote</div>"
                + "<div style='font-size:10px; color:#444; margin-bottom:2px;'>协议：Apache-2.0</div>"
                + "<div style='font-size:10px; color:#444; margin-bottom:8px;'>微信：lakernote</div>"
                + "<hr style='border:none; border-top:1px solid #eee; margin:10px 0;'>"
                + "<div style='font-size:9px; margin-bottom:2px;'>"
                + "<a href='https://laker.blog.csdn.net' style='color:#1a0dab; text-decoration:none;'>Blog: https://laker.blog.csdn.net</a>"
                + "</div>"
                + "<div style='font-size:9px; margin-bottom:2px;'>"
                + "<a href='https://github.com/lakernote' style='color:#1a0dab; text-decoration:none;'>GitHub: https://github.com/lakernote</a>"
                + "</div>"
                + "<div style='font-size:9px;'>"
                + "<a href='https://gitee.com/lakernote' style='color:#1a0dab; text-decoration:none;'>Gitee: https://gitee.com/lakernote</a>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Failed to open link: " + e.getURL(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        // 直接用JEditorPane，不用滚动条，且自适应高度
        editorPane.setPreferredSize(new Dimension(340, 360));
        JOptionPane.showMessageDialog(null, editorPane, "About EasyPostman", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * 检查更新：访问 Gitee Release API，获取最新版本号并与本地对比。
     */
    private void checkUpdate() {
        // 显示正在检查更新的对话框
        final JDialog loadingDialog = new JDialog((Frame) null, "检查更新", true);
        loadingDialog.setResizable(false);
        JLabel loadingLabel = new JLabel("正在检查更新...", SwingConstants.CENTER);
        loadingLabel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        loadingDialog.getContentPane().add(loadingLabel);
        loadingDialog.pack();
        loadingDialog.setSize(320, 120);
        loadingDialog.setLocationRelativeTo(null);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String latestVersion = null;
            String releaseUrl = "https://gitee.com/lakernote/easy-postman/releases";
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
                        errorMsg = "网络错误，状态码：" + code;
                    }
                } catch (Exception ex) {
                    errorMsg = "检查更新失败：" + ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                String currentVersion = getCurrentVersion();
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(null, errorMsg, "检查更新", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (latestVersion == null) {
                    JOptionPane.showMessageDialog(null, "未获取到最新版本信息。", "检查更新", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (compareVersion(latestVersion, currentVersion) <= 0) {
                    JOptionPane.showMessageDialog(null, "当前已是最新版本 (" + currentVersion + ")", "检查更新", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                // 新版本弹窗
                Object[] options = {"手动下载", "自动下载并安装", "取消"};
                int r = JOptionPane.showOptionDialog(null,
                        "发现新版本：" + latestVersion + "\n请选择升级方式：",
                        "检查更新",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[1]);
                if (r == 0) {
                    try {
                        Desktop.getDesktop().browse(new URI(releaseUrl));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "打开浏览器失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(null, "未找到最新安装包（.msi/.dmg）下载链接。", "自动下载并安装", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 优化下载弹窗UI，增加图标、剩余时间、重试按钮
        JDialog downloadingDialog = new JDialog((Frame) null, "自动下载并安装", true);
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
        JLabel statusLabel = new JLabel("正在连接服务器...", SwingConstants.CENTER);
        statusLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 16));
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
        Dimension infoLabelSize = new Dimension(220, 24);
        JLabel sizeLabel = new JLabel("下载进度：-- / -- MB", SwingConstants.LEFT);
        sizeLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 13));
        sizeLabel.setForeground(new Color(80, 80, 80));
        sizeLabel.setPreferredSize(infoLabelSize);
        sizeLabel.setMinimumSize(infoLabelSize);
        sizeLabel.setMaximumSize(infoLabelSize);
        infoPanel.add(sizeLabel, gbc);
        JLabel speedLabel = new JLabel("下载速度：-- KB/s", SwingConstants.LEFT);
        speedLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 13));
        speedLabel.setForeground(new Color(80, 80, 80));
        speedLabel.setPreferredSize(infoLabelSize);
        speedLabel.setMinimumSize(infoLabelSize);
        speedLabel.setMaximumSize(infoLabelSize);
        infoPanel.add(speedLabel, gbc);
        JLabel timeLabel = new JLabel("预估时间：-- s", SwingConstants.LEFT);
        timeLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 13));
        timeLabel.setForeground(new Color(80, 80, 80));
        timeLabel.setPreferredSize(infoLabelSize);
        timeLabel.setMinimumSize(infoLabelSize);
        timeLabel.setMaximumSize(infoLabelSize);
        infoPanel.add(timeLabel, gbc);
        panel.add(Box.createVerticalStrut(8));
        panel.add(infoPanel);
        // 取消和重试按钮
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnPanel.setOpaque(false);
        JButton cancelButton = new JButton("取消下载");
        cancelButton.setFont(FontUtil.getDefaultFont(Font.PLAIN, 14));
        cancelButton.setBackground(new Color(220, 230, 245));
        cancelButton.setForeground(new Color(33, 37, 41));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton retryButton = new JButton("重试");
        retryButton.setFont(FontUtil.getDefaultFont(Font.PLAIN, 14));
        retryButton.setBackground(new Color(220, 230, 245));
        retryButton.setForeground(new Color(33, 37, 41));
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
        final boolean[] errorFlag = {false};
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
                    statusLabel.setText("正在连接服务器...");
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
                            statusLabel.setText("正在下载最新安装包...");
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
                                    sizeStr = "下载进度：-- / -- MB";
                                } else {
                                    sizeStr = String.format("下载进度：%7.1f / %7.1f MB", downloaded / 1024.0 / 1024, totalSize / 1024.0 / 1024);
                                }
                                String speedStr = String.format("下载速度：%7.1f KB/s ", speed / 1024);
                                int remainSec;
                                String timeStr;
                                if (speed > 0 && totalSize > 0) {
                                    remainSec = (int) ((totalSize - downloaded) / speed);
                                    timeStr = String.format("预估时间：%5d s ", remainSec);
                                } else {
                                    remainSec = -1;
                                    timeStr = "预估时间：      s ";
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
                    JOptionPane.showMessageDialog(null, "下载已取消。", "自动下载并安装", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (error != null) {
                    errorFlag[0] = true;
                    retryButton.setVisible(true);
                    JOptionPane.showMessageDialog(null, error, "自动下载并安装", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (downloadedFile != null) {
                    String tip = "安装包已下载，是否立即打开安装？\n请确保已关闭所有 EasyPostman 程序，否则安装可能失败。\n点击“是”将自动关闭本程序并打开安装包。";
                    int open = JOptionPane.showConfirmDialog(null, tip, "自动下载并安装", JOptionPane.YES_NO_OPTION);
                    if (open == JOptionPane.YES_OPTION) {
                        try {
                            Desktop.getDesktop().open(downloadedFile);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "打开安装包失败：" + ex.getMessage(), "自动下载并安装", JOptionPane.ERROR_MESSAGE);
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
            return "网络连接超时，请检查网络后重试。";
        } else if (ex instanceof java.net.UnknownHostException) {
            return "无法连接服务器，请检查网络。";
        } else if (ex instanceof java.io.FileNotFoundException) {
            return "下载链接无效或文件不存在。";
        } else if (ex instanceof java.io.IOException) {
            if (ex.getMessage() != null && ex.getMessage().contains("No space left on device")) {
                return "磁盘空间不足，请清理后重试。";
            } else if (ex.getMessage() != null && ex.getMessage().contains("Permission denied")) {
                return "没有写入权限，请检查文件夹权限。";
            }
            return "下载文件时发生IO异常: " + ex.getMessage();
        }
        return "自动下载失败：" + ex.getMessage();
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
}
