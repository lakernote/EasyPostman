package com.laker.postman.service;

import cn.hutool.json.JSONObject;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * 版本更新服务 - 负责后台检测版本更新并显示通知
 */
@Slf4j
public class UpdateService {

    private static UpdateService instance;
    private static final String GITEE_API_URL = "https://gitee.com/api/v5/repos/lakernote/easy-postman/releases/latest";
    private static final String RELEASE_URL = "https://gitee.com/lakernote/easy-postman/releases";

    private UpdateService() {
        // 私有构造函数，防止外部实例化
    }

    public static UpdateService getInstance() {
        if (instance == null) {
            synchronized (UpdateService.class) {
                if (instance == null) {
                    instance = new UpdateService();
                }
            }
        }
        return instance;
    }

    /**
     * 启动时异步检查更新
     */
    public void checkUpdateOnStartup() {
        CompletableFuture.runAsync(() -> {
            try {
                // 延迟几秒后开始检查，避免影响启动速度
                Thread.sleep(3000);

                String currentVersion = SystemUtil.getCurrentVersion();
                String latestVersion = getLatestVersion();

                if (latestVersion != null && compareVersion(latestVersion, currentVersion) > 0) {
                    // 有新版本，显示通知
                    SwingUtilities.invokeLater(() -> showUpdateNotification(latestVersion));
                }
            } catch (Exception e) {
                log.warn("Failed to check update on startup", e);
            }
        });
    }

    /**
     * 获取最新版本号
     */
    private String getLatestVersion() {
        try {
            URL url = new URL(GITEE_API_URL);
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
                    JSONObject latestReleaseJson = new JSONObject(json);
                    return latestReleaseJson.getStr("tag_name");
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get latest version: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 显示更新通知（右下角弹窗）
     */
    private void showUpdateNotification(String latestVersion) {
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        if (!mainFrame.isVisible()) {
            return;
        }

        // 创建通知窗口
        JWindow notificationWindow = new JWindow(mainFrame);
        notificationWindow.setAlwaysOnTop(true);

        // 创建通知面板
        JPanel panel = createNotificationPanel(latestVersion, notificationWindow);
        notificationWindow.setContentPane(panel);
        notificationWindow.pack();

        // 定位到主界面的右下角
        Rectangle mainFrameBounds = mainFrame.getBounds();
        int x = mainFrameBounds.x + mainFrameBounds.width - notificationWindow.getWidth() - 10;
        int y = mainFrameBounds.y + mainFrameBounds.height - notificationWindow.getHeight() - 10;

        // 确保通知窗口不超出屏幕边界
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (x + notificationWindow.getWidth() > screenSize.width) {
            x = screenSize.width - notificationWindow.getWidth() - 10;
        }
        if (y + notificationWindow.getHeight() > screenSize.height) {
            y = screenSize.height - notificationWindow.getHeight() - 10;
        }
        if (x < 0) x = 10;
        if (y < 0) y = 10;

        notificationWindow.setLocation(x, y);

        // 显示通知窗口
        notificationWindow.setVisible(true);

        // 10秒后自动关闭
        Timer autoCloseTimer = new Timer(10000, e -> notificationWindow.dispose());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    /**
     * 创建通知面板
     */
    private JPanel createNotificationPanel(String latestVersion, JWindow parentWindow) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(new Color(248, 249, 250));
        panel.setPreferredSize(new Dimension(350, 120));

        // 图标和标题
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/info.svg", 24, 24));
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE));
        titleLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 14));
        titleLabel.setForeground(new Color(33, 37, 41));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        headerPanel.add(iconLabel);
        headerPanel.add(titleLabel);

        // 版本信息
        JLabel versionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_FOUND, latestVersion));
        versionLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        versionLabel.setForeground(new Color(108, 117, 125));

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        JButton laterButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
        laterButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        laterButton.setFocusPainted(false);
        laterButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        laterButton.setBackground(new Color(248, 249, 250));
        laterButton.addActionListener(e -> parentWindow.dispose());

        JButton updateButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_NOW));
        updateButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        updateButton.setFocusPainted(false);
        updateButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        updateButton.setBackground(new Color(0, 123, 255));
        updateButton.setForeground(Color.WHITE);
        updateButton.addActionListener(e -> {
            parentWindow.dispose();
            showUpdateDialog(latestVersion);
        });

        JButton closeButton = new JButton("×");
        closeButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 14));
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        closeButton.setBackground(new Color(248, 249, 250));
        closeButton.setForeground(new Color(108, 117, 125));
        closeButton.addActionListener(e -> parentWindow.dispose());

        buttonPanel.add(laterButton);
        buttonPanel.add(updateButton);

        // 关闭按钮放在右上角
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(headerPanel, BorderLayout.WEST);
        topPanel.add(closeButton, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(versionLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 显示更新对话框（复用TopMenuBarPanel中的逻辑）
     */
    private void showUpdateDialog(String latestVersion) {
        // 获取完整的release信息
        CompletableFuture.runAsync(() -> {
            try {
                JSONObject latestReleaseJson = getLatestReleaseInfo();
                if (latestReleaseJson != null) {
                    SwingUtilities.invokeLater(() -> {
                        Object[] options = {
                                I18nUtil.getMessage(MessageKeys.UPDATE_MANUAL_DOWNLOAD),
                                I18nUtil.getMessage(MessageKeys.UPDATE_AUTO_DOWNLOAD),
                                I18nUtil.getMessage(MessageKeys.UPDATE_CANCEL)
                        };
                        int r = JOptionPane.showOptionDialog(
                                SingletonFactory.getInstance(MainFrame.class),
                                I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_FOUND, latestVersion),
                                I18nUtil.getMessage(MessageKeys.MENU_HELP_UPDATE),
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null,
                                options,
                                options[1]);

                        if (r == 0) {
                            // 手动下载
                            try {
                                Desktop.getDesktop().browse(new URI(RELEASE_URL));
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(null,
                                        I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, ex.getMessage()),
                                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        } else if (r == 1) {
                            // 自动下载 - 这里可以调用TopMenuBarPanel的下载方法
                            // 为了简化，暂时跳转到手动下载页面
                            try {
                                Desktop.getDesktop().browse(new URI(RELEASE_URL));
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(null,
                                        I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, ex.getMessage()),
                                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Failed to show update dialog", e);
            }
        });
    }

    /**
     * 获取完整的release信息
     */
    private JSONObject getLatestReleaseInfo() {
        try {
            URL url = new URL(GITEE_API_URL);
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
                    return new JSONObject(json);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get latest release info", e);
        }
        return null;
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
}
