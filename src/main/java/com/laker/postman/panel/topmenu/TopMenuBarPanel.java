package com.laker.postman.panel.topmenu;

import com.formdev.flatlaf.extras.FlatDesktop;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.combobox.EnvironmentComboBox;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.topmenu.help.ChangelogDialog;
import com.laker.postman.panel.topmenu.setting.ModernSettingsDialog;
import com.laker.postman.service.ExitService;
import com.laker.postman.service.UpdateService;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.laker.postman.util.SystemUtil.getCurrentVersion;

@Slf4j
public class TopMenuBarPanel extends SingletonBasePanel {
    @Getter
    private EnvironmentComboBox environmentComboBox;
    private JComboBox<Workspace> workspaceComboBox;
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
        FlatDesktop.setQuitHandler(e -> BeanFactory.getBean(ExitService.class).exit());
    }

    /**
     * 重新加载快捷键（快捷键设置修改后调用）
     */
    public void reloadShortcuts() {
        // 移除旧的菜单栏
        remove(menuBar);

        // 重新创建菜单栏
        menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEmptyBorder());
        addFileMenu();
        addLanguageMenu();
        addSettingMenu();
        addHelpMenu();
        addAboutMenu();
        add(menuBar, BorderLayout.WEST);

        // 刷新界面
        revalidate();
        repaint();
    }

    private void initComponents() {
        menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEmptyBorder());
        addFileMenu();
        addLanguageMenu();
        addSettingMenu();
        addHelpMenu();
        addAboutMenu();
        add(menuBar, BorderLayout.WEST);
        addRightLableAndComboBox();
    }

    private void addFileMenu() {
        JMenu fileMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_FILE));
        JMenuItem logMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG));
        logMenuItem.addActionListener(e -> openLogDirectory());
        JMenuItem exitMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_EXIT));
        // 使用 ShortcutManager 获取退出快捷键
        KeyStroke exitKey = com.laker.postman.service.setting.ShortcutManager.getKeyStroke(
                com.laker.postman.service.setting.ShortcutManager.EXIT_APP);
        if (exitKey != null) {
            exitMenuItem.setAccelerator(exitKey);
        }
        exitMenuItem.addActionListener(e -> BeanFactory.getBean(ExitService.class).exit());
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
        NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.LANGUAGE_CHANGED));
    }

    private void addSettingMenu() {
        JMenu settingMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_SETTINGS));

        // 统一设置（现代化设置对话框）
        JMenuItem settingsMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_TITLE));
        settingsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        settingsMenuItem.addActionListener(e -> showModernSettingsDialog());
        settingMenu.add(settingsMenuItem);

        settingMenu.addSeparator();

        // 快捷访问各个设置标签页
        JMenuItem uiSettingMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_TITLE));
        uiSettingMenuItem.addActionListener(e -> showModernSettingsDialog(0));
        settingMenu.add(uiSettingMenuItem);

        JMenuItem requestSettingMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TITLE));
        requestSettingMenuItem.addActionListener(e -> showModernSettingsDialog(1));
        settingMenu.add(requestSettingMenuItem);

        JMenuItem proxySettingMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TITLE));
        proxySettingMenuItem.addActionListener(e -> showModernSettingsDialog(2));
        settingMenu.add(proxySettingMenuItem);

        JMenuItem systemSettingMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_TITLE));
        systemSettingMenuItem.addActionListener(e -> showModernSettingsDialog(3));
        settingMenu.add(systemSettingMenuItem);

        JMenuItem performanceSettingMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_TITLE));
        performanceSettingMenuItem.addActionListener(e -> showModernSettingsDialog(4));
        settingMenu.add(performanceSettingMenuItem);

        JMenuItem clientCertMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CERT_TITLE));
        clientCertMenuItem.addActionListener(e -> showModernSettingsDialog(5));
        settingMenu.add(clientCertMenuItem);

        JMenuItem shortcutMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.SETTINGS_SHORTCUTS_TITLE));
        shortcutMenuItem.addActionListener(e -> showModernSettingsDialog(6));
        settingMenu.add(shortcutMenuItem);

        menuBar.add(settingMenu);
    }


    private void showModernSettingsDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        ModernSettingsDialog.showSettings(window);
    }

    private void showModernSettingsDialog(int tabIndex) {
        Window window = SwingUtilities.getWindowAncestor(this);
        ModernSettingsDialog.showSettings(window, tabIndex);
    }

    private void addHelpMenu() {
        JMenu helpMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_HELP));
        JMenuItem updateMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_UPDATE));
        updateMenuItem.addActionListener(e -> checkUpdate());
        JMenuItem changelogMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_CHANGELOG));
        changelogMenuItem.addActionListener(e -> showChangelogDialog());
        JMenuItem feedbackMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_FEEDBACK));
        feedbackMenuItem.addActionListener(e -> showFeedbackDialog());
        helpMenu.add(updateMenuItem);
        helpMenu.add(changelogMenuItem);
        helpMenu.add(feedbackMenuItem);
        menuBar.add(helpMenu);
    }

    private void showFeedbackDialog() {
        JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.FEEDBACK_MESSAGE),
                I18nUtil.getMessage(MessageKeys.FEEDBACK_TITLE), JOptionPane.INFORMATION_MESSAGE);
    }

    private void showChangelogDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Frame frame) {
            ChangelogDialog.showDialog(frame);
        } else {
            log.warn("Cannot show changelog dialog: parent is not a Frame");
        }
    }

    private void addAboutMenu() {
        JMenu aboutMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_ABOUT));
        JMenuItem aboutMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_ABOUT_EASYPOSTMAN));
        aboutMenuItem.addActionListener(e -> aboutActionPerformed());
        aboutMenu.add(aboutMenuItem);
        menuBar.add(aboutMenu);
    }

    private void addRightLableAndComboBox() {
        if (environmentComboBox == null) {
            environmentComboBox = new EnvironmentComboBox();
        } else {
            environmentComboBox.reload();
        }

        // 创建工作区下拉框
        if (workspaceComboBox == null) {
            workspaceComboBox = createWorkspaceComboBox();
        } else {
            updateWorkspaceComboBox();
        }

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        rightPanel.setOpaque(false);

        // 添加工作区图标和下拉框
        JLabel workspaceIconLabel = new JLabel(new FlatSVGIcon("icons/workspace.svg", 20, 20));
        rightPanel.add(workspaceIconLabel);
        rightPanel.add(workspaceComboBox);

        // 添加分隔间距
        rightPanel.add(Box.createHorizontalStrut(2));

        // 添加环境变量图标和下拉框
        JLabel envIconLabel = new JLabel(new FlatSVGIcon("icons/environments.svg", 20, 20));
        rightPanel.add(envIconLabel);
        rightPanel.add(environmentComboBox);

        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * 创建工作区下拉框
     */
    private JComboBox<Workspace> createWorkspaceComboBox() {
        JComboBox<Workspace> comboBox = new JComboBox<>();
        comboBox.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        comboBox.setMaximumRowCount(10);
        comboBox.setPreferredSize(new Dimension(150, 28));

        // 自定义渲染器，只显示名称（不显示图标，因为外面已有图标）
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Workspace workspace) {
                    setText(workspace.getName());
                    // 如果名称太长，使用tooltip显示完整名称
                    if (workspace.getName().length() > 15) {
                        setToolTipText(workspace.getName());
                    }
                }
                return this;
            }
        });

        // 添加选择监听器
        comboBox.addActionListener(e -> {
            Workspace selectedWorkspace = (Workspace) comboBox.getSelectedItem();
            if (selectedWorkspace != null) {
                switchToWorkspace(selectedWorkspace);
            }
        });

        // 先初始化数据，再返回
        loadWorkspaceComboBoxData(comboBox);
        return comboBox;
    }

    /**
     * 加载工作区下拉框数据
     */
    private void loadWorkspaceComboBoxData(JComboBox<Workspace> comboBox) {
        try {
            WorkspaceService workspaceService = WorkspaceService.getInstance();
            List<Workspace> workspaces = workspaceService.getAllWorkspaces();
            Workspace currentWorkspace = workspaceService.getCurrentWorkspace();

            // 移除监听器，避免触发切换事件
            var listeners = comboBox.getActionListeners();
            for (var listener : listeners) {
                comboBox.removeActionListener(listener);
            }

            // 加载数据
            comboBox.removeAllItems();
            for (Workspace workspace : workspaces) {
                comboBox.addItem(workspace);
            }

            // 设置当前选中的工作区
            if (currentWorkspace != null) {
                comboBox.setSelectedItem(currentWorkspace);
            }

            // 重新添加监听器
            for (var listener : listeners) {
                comboBox.addActionListener(listener);
            }
        } catch (Exception e) {
            log.warn("Failed to load workspace combobox data", e);
        }
    }

    /**
     * 更新工作区下拉框内容
     */
    private void updateWorkspaceComboBox() {
        if (workspaceComboBox == null) {
            return;
        }
        loadWorkspaceComboBoxData(workspaceComboBox);
    }

    /**
     * 切换到指定工作区
     */
    private void switchToWorkspace(Workspace workspace) {
        try {
            WorkspaceService workspaceService = WorkspaceService.getInstance();
            Workspace currentWorkspace = workspaceService.getCurrentWorkspace();

            // 如果选中的就是当前工作区，不做任何操作
            if (currentWorkspace != null && currentWorkspace.getId().equals(workspace.getId())) {
                return;
            }

            workspaceService.switchWorkspace(workspace.getId());

            // 切换环境变量文件
            SingletonFactory.getInstance(EnvironmentPanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));

            // 切换请求集合文件
            SingletonFactory.getInstance(RequestCollectionsLeftPanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace));

            // 只记录日志，不显示通知弹窗
            log.info("Switched to workspace: {}", workspace.getName());
        } catch (Exception e) {
            log.error("Failed to switch workspace", e);
            // 只有出错时才显示通知
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.WORKSPACE_OPERATION_FAILED_DETAIL, e.getMessage()));
        }
    }

    /**
     * 更新工作区显示（保留此方法以保持向后兼容）
     */
    public void updateWorkspaceDisplay() {
        updateWorkspaceComboBox();
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
                + "<div style='font-size:12px; color:#666; text-align:center; margin-bottom:12px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_VERSION, getCurrentVersion()) + "</div>"
                + "<div style='font-size:10px; color:#444; margin-bottom:2px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_AUTHOR) + "</div>"
                + "<div style='font-size:10px; color:#444; margin-bottom:2px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_LICENSE) + "</div>"
                + "<div style='font-size:10px; color:#444; margin-bottom:8px;'>"
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
        editorPane.setPreferredSize(new Dimension(310, 350));
        return editorPane;
    }

    /**
     * 检查更新
     */
    private void checkUpdate() {
        BeanFactory.getBean(UpdateService.class).checkUpdateManually();
    }
}