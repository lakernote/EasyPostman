package com.laker.postman.common.panel;

import cn.hutool.json.JSONObject;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatDesktop;
import com.laker.postman.common.combobox.EnvironmentComboBox;
import com.laker.postman.common.dialog.ExitDialog;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
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
        JMenu fileMenu = new JMenu("文件");
        JMenuItem exitMenuItem = new JMenuItem("退出");
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitMenuItem.setMnemonic('X');
        exitMenuItem.addActionListener(e -> ExitDialog.show());
        JMenuItem logMenuItem = new JMenuItem("日志");
        logMenuItem.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(SystemUtil.LOG_DIR));
            } catch (IOException ex) {
                log.error("Failed to open log directory", ex);
                JOptionPane.showMessageDialog(null,
                        "无法打开日志目录，请检查日志。",
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        fileMenu.add(logMenuItem);
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);

        // ---------主题菜单
        JMenu themeMenu = new JMenu("主题");
        ButtonGroup themeGroup = new ButtonGroup();
        JRadioButtonMenuItem lightTheme = new JRadioButtonMenuItem("浅色(Flat Light)");
        JRadioButtonMenuItem intellijTheme = new JRadioButtonMenuItem("IntelliJ 风格");
        JRadioButtonMenuItem macLightTheme = new JRadioButtonMenuItem("Mac Light 风格");
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

        // ---------帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        // 新增“检查更新”菜单项
        JMenuItem updateMenuItem = new JMenuItem("检查更新");
        updateMenuItem.addActionListener(e -> checkUpdate());
        helpMenu.add(updateMenuItem);
        // 新增“反馈建议”菜单项
        JMenuItem feedbackMenuItem = new JMenuItem("反馈建议");
        feedbackMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(null, "请通过 Gitee 或 GitHub 提交 issue。", "反馈建议", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(feedbackMenuItem);
        menuBar.add(helpMenu);

        // ---------关于菜单
        JMenu aboutMenu = new JMenu("关于");
        JMenuItem aboutMenuItem = new JMenuItem("关于 EasyPostman");
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
                + "<a href='https://laker.blog.csdn.net' style='color:#1a0dab; text-decoration:none;'>博客：https://laker.blog.csdn.net</a>"
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
                    JOptionPane.showMessageDialog(null, "无法打开链接：" + e.getURL(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        // 直接用JEditorPane，不用滚动条，且自适应高度
        editorPane.setPreferredSize(new Dimension(300, 340));
        JOptionPane.showMessageDialog(null, editorPane, "关于 EasyPostman", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * 检查更新：访问 Gitee Release API，获取最新版本号并与本地对比。
     */
    private void checkUpdate() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String latestVersion = null;
            final String releaseUrl = "https://gitee.com/lakernote/easy-postman/releases";
            String errorMsg = null;

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
                            log.info("Received update info: {}", json);
                            JSONObject jsonObj = new JSONObject(json);
                            latestVersion = jsonObj.getStr("tag_name");
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
                String currentVersion = getCurrentVersion();
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(null, errorMsg, "检查更新", JOptionPane.ERROR_MESSAGE);
                } else if (latestVersion == null) {
                    JOptionPane.showMessageDialog(null, "未获取到最新版本信息。", "检查更新", JOptionPane.WARNING_MESSAGE);
                } else if (compareVersion(latestVersion, currentVersion) <= 0) {
                    JOptionPane.showMessageDialog(null, "已是最新版本（" + currentVersion + "）", "检查更新", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int r = JOptionPane.showConfirmDialog(null, "发现新版本：" + latestVersion + "\n是否前往下载？", "检查更新", JOptionPane.YES_NO_OPTION);
                    if (r == JOptionPane.YES_OPTION) {
                        try {
                            Desktop.getDesktop().browse(new URI(releaseUrl));
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "无法打开浏览器：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        };
        worker.execute();
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
