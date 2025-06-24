package com.laker.postman.common.panel;

import cn.hutool.json.JSONObject;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatDesktop;
import com.laker.postman.common.combobox.EnvironmentComboBox;
import com.laker.postman.common.dialog.ExitDialog;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
public class TopMenuBarPanel extends AbstractBasePanel {
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
        JRadioButtonMenuItem darkTheme = new JRadioButtonMenuItem("深色(Flat Dark)");
        JRadioButtonMenuItem intellijTheme = new JRadioButtonMenuItem("IntelliJ 风格");
        JRadioButtonMenuItem macLightTheme = new JRadioButtonMenuItem("Mac Light 风格");
        themeGroup.add(lightTheme);
        themeGroup.add(darkTheme);
        themeGroup.add(intellijTheme);
        themeGroup.add(macLightTheme);
        themeMenu.add(lightTheme);
        themeMenu.add(darkTheme);
        themeMenu.add(intellijTheme);
        themeMenu.add(macLightTheme);
        // 根据当前主题设置默认选中项
        String lafClass = UIManager.getLookAndFeel().getClass().getName();
        switch (lafClass) {
            case "com.formdev.flatlaf.FlatLightLaf" -> lightTheme.setSelected(true);
            case "com.formdev.flatlaf.FlatDarkLaf" -> darkTheme.setSelected(true);
            case "com.formdev.flatlaf.themes.FlatMacLightLaf" -> macLightTheme.setSelected(true);
            default -> intellijTheme.setSelected(true);
        }
        // 切换主题事件
        lightTheme.addActionListener(e -> switchLaf("com.formdev.flatlaf.FlatLightLaf"));
        darkTheme.addActionListener(e -> switchLaf("com.formdev.flatlaf.FlatDarkLaf"));
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("EasyPostman");
        title.setFont(FontUtil.getDefaultFont(Font.BOLD, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel version = new JLabel("版本：" + getCurrentVersion());
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel author = new JLabel("作者：lakernote");
        author.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel license = new JLabel("协议：Apache-2.0");
        license.setAlignmentX(Component.CENTER_ALIGNMENT);
        // 可点击的博客、GitHub、Gitee
        JLabel blog = createLinkLabel("博客：https://laker.blog.csdn.net", "https://laker.blog.csdn.net");
        blog.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel github = createLinkLabel("GitHub: https://github.com/lakernote", "https://github.com/lakernote");
        github.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel gitee = createLinkLabel("Gitee: https://gitee.com/lakernote", "https://gitee.com/lakernote");
        gitee.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel contact = new JLabel("微信：lakernote");
        contact.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createVerticalStrut(8));
        panel.add(title);
        panel.add(version);
        panel.add(author);
        panel.add(license);
        panel.add(contact);
        panel.add(blog);
        panel.add(github);
        panel.add(gitee);
        JOptionPane.showMessageDialog(null, panel, "关于 EasyPostman", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * 创建可点击的 JLabel 链接（无下划线样式）
     */
    private JLabel createLinkLabel(String text, String url) {
        JLabel label = new JLabel(
                "<html><span style='color:#1a0dab;cursor:pointer;'>" + text + "</span></html>");
        label.setForeground(new Color(26, 13, 171)); // Google 蓝色
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "无法打开链接：" + url, "错误", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(new Color(66, 133, 244)); // 鼠标悬停变色
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(new Color(26, 13, 171));
            }
        });
        return label;
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
                } else if (latestVersion.equals(currentVersion)) {
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

    private void switchLaf(String className) {
        try {
            FlatAnimatedLafChange.showSnapshot();
            switch (className) {
                case "com.formdev.flatlaf.FlatLightLaf" -> com.formdev.flatlaf.FlatLightLaf.setup();
                case "com.formdev.flatlaf.FlatDarkLaf" -> com.formdev.flatlaf.FlatDarkLaf.setup();
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