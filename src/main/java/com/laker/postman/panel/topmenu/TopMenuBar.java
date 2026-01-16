package com.laker.postman.panel.topmenu;

import com.formdev.flatlaf.extras.FlatDesktop;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemInfo;
import com.laker.postman.common.IRefreshable;
import com.laker.postman.common.SingletonBaseMenuBar;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.combobox.EnvironmentComboBox;
import com.laker.postman.common.component.combobox.WorkspaceComboBox;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.themes.SimpleThemeManager;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.RemoteStatus;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.topmenu.help.ChangelogDialog;
import com.laker.postman.panel.topmenu.setting.ModernSettingsDialog;
import com.laker.postman.panel.workspace.components.GitOperationDialog;
import com.laker.postman.service.ExitService;
import com.laker.postman.service.UpdateService;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.service.setting.ShortcutManager;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import static com.laker.postman.util.SystemUtil.getCurrentVersion;

@Slf4j
public class TopMenuBar extends SingletonBaseMenuBar implements IRefreshable {
    private static final String BUTTON_FOREGROUND_KEY = "Button.foreground";

    @Getter
    private EnvironmentComboBox environmentComboBox;
    @Getter
    private WorkspaceComboBox workspaceComboBox;

    /**
     * 获取主题适配的边框颜色（用于HTML）
     */
    @SuppressWarnings("unused")
    private static String getThemeBorderColor() {
        Color color = ModernColors.getDividerBorderColor();
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 获取主题适配的主文本颜色（用于HTML）
     */
    @SuppressWarnings("unused")
    private static String getThemeTextColor() {
        Color color = ModernColors.getTextPrimary();
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 获取主题适配的次文本颜色（用于HTML）
     */
    @SuppressWarnings("unused")
    private static String getThemeSecondaryTextColor() {
        Color color = ModernColors.getTextSecondary();
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 获取主题适配的提示文本颜色（用于HTML）
     */
    @SuppressWarnings("unused")
    private static String getThemeHintTextColor() {
        Color color = ModernColors.getTextHint();
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 获取主题适配的链接颜色（用于HTML）
     */
    @SuppressWarnings("unused")
    private static String getThemeLinkColor() {
        return SimpleThemeManager.isDarkTheme() ? "#60a5fa" : "#1a0dab";
    }

    @Override
    protected void initUI() {
        setOpaque(true);
        setBorder(createPanelBorder());
        initComponents();
    }

    private Border createPanelBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(2, 4, 1, 8)
        );
    }

    @Override
    protected void registerListeners() {
        FlatDesktop.setAboutHandler(this::aboutActionPerformed);
        FlatDesktop.setQuitHandler(e -> BeanFactory.getBean(ExitService.class).exit());
    }

    /**
     * 刷新菜单栏（实现 IRefreshable 接口）
     * <p>
     * 重新加载菜单栏（包括菜单项、快捷键、Git 工具栏等所有组件）
     * 在以下场景调用：
     * 1. 语言切换后（需要更新所有菜单文本）
     * 2. 快捷键设置修改后
     * 3. 工作区切换后（需要更新 Git 工具栏显示状态）
     */
    @Override
    public void refresh() {
        removeAll();
        // 重新创建菜单栏所有组件
        initComponents();
        // 刷新界面
        revalidate();
        repaint();
    }

    private void initComponents() {
        if (SystemInfo.isMacFullWindowContentSupported) {
            // macOS Full Window Content 模式下，左侧留出更多空间给红黄绿按钮
            // macOS 红黄绿按钮宽度约 70-76px
            add(Box.createHorizontalStrut(70));
        }

        addFileMenu();
        addLanguageMenu();
        addThemeMenu();
        addSettingMenu();
        addHelpMenu();
        addAboutMenu();

        add(Box.createGlue()); // 添加弹性空间，将后续组件推到右侧

        addRightLableAndComboBox();
    }

    private void addFileMenu() {
        JMenu fileMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_FILE));
        JMenuItem logMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG));
        logMenuItem.addActionListener(e -> openLogDirectory());
        JMenuItem exitMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_FILE_EXIT));
        // 使用 ShortcutManager 获取退出快捷键
        KeyStroke exitKey = ShortcutManager.getKeyStroke(ShortcutManager.EXIT_APP);
        if (exitKey != null) {
            exitMenuItem.setAccelerator(exitKey);
        }
        exitMenuItem.addActionListener(e -> BeanFactory.getBean(ExitService.class).exit());
        fileMenu.add(logMenuItem);
        fileMenu.add(exitMenuItem);
        add(fileMenu);
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
        add(languageMenu);
    }

    private void switchLanguage(String languageCode) {
        I18nUtil.setLocale(languageCode);

        // 使用统一的刷新管理器刷新所有组件（包括窗口标题、菜单栏、面板等）
        UIRefreshManager.refreshLanguage();

        NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.LANGUAGE_CHANGED));
    }

    private void addThemeMenu() {
        JMenu themeMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_THEME));
        ButtonGroup themeGroup = new ButtonGroup();

        JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem(I18nUtil.getMessage(MessageKeys.MENU_THEME_LIGHT));
        JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem(I18nUtil.getMessage(MessageKeys.MENU_THEME_DARK));

        themeGroup.add(lightItem);
        themeGroup.add(darkItem);

        // 设置当前选中的主题
        if (SimpleThemeManager.isLightTheme()) {
            lightItem.setSelected(true);
        } else {
            darkItem.setSelected(true);
        }

        lightItem.addActionListener(e -> SimpleThemeManager.switchToLightTheme());
        darkItem.addActionListener(e -> SimpleThemeManager.switchToDarkTheme());

        themeMenu.add(lightItem);
        themeMenu.add(darkItem);
        add(themeMenu);
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

        add(settingMenu);
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
        add(helpMenu);
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
        add(aboutMenu);
    }


    private void addRightLableAndComboBox() {
        // 初始化或重新加载工作区下拉框
        if (workspaceComboBox == null) {
            workspaceComboBox = ComboBoxStyleHelper.createWithPanelStyle(WorkspaceComboBox::new);
            workspaceComboBox.setOnWorkspaceChange(this::switchToWorkspace);
        } else {
            workspaceComboBox.reload();
        }

        // 初始化或重新加载环境下拉框
        if (environmentComboBox == null) {
            environmentComboBox = ComboBoxStyleHelper.createWithPanelStyle(EnvironmentComboBox::new);
        } else {
            environmentComboBox.reload();
        }

        // 添加 Git 工具栏（在工作区下拉框左侧）
        addGitToolbarIfNeeded();

        // 添加工作区图标和下拉框
        FlatSVGIcon workspaceIcon = new FlatSVGIcon("icons/workspace.svg", 20, 20);
        workspaceIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor(BUTTON_FOREGROUND_KEY)));
        JLabel workspaceIconLabel = new JLabel(workspaceIcon);
        add(workspaceIconLabel);
        add(workspaceComboBox);

        // 添加分隔间距
        add(Box.createHorizontalStrut(2));

        // 添加环境变量图标和下拉框
        FlatSVGIcon envIcon = new FlatSVGIcon("icons/environments.svg", 20, 20);
        envIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor(BUTTON_FOREGROUND_KEY)));
        JLabel envIconLabel = new JLabel(envIcon);
        add(envIconLabel);
        add(environmentComboBox);
    }

    /**
     * 添加 Git 工具栏（仅在当前工作区为 Git 类型时添加）
     */
    private void addGitToolbarIfNeeded() {
        try {
            WorkspaceService workspaceService = WorkspaceService.getInstance();
            Workspace currentWorkspace = workspaceService.getCurrentWorkspace();

            // 只有当前工作区是 Git 工作区时才显示 Git 工具栏
            if (currentWorkspace != null && currentWorkspace.getType() == WorkspaceType.GIT) {
                JPanel gitToolbarPanel = createGitToolbar(currentWorkspace);
                add(gitToolbarPanel);
                add(Box.createHorizontalStrut(12)); // Git 工具栏和工作区图标之间的间距
            }
        } catch (Exception e) {
            log.error("Failed to create Git toolbar", e);
        }
    }

    /**
     * 更新工作区下拉框内容（不重新加载整个菜单栏）
     * 用于工作区列表变化但当前工作区类型未变化的场景（如创建、重命名）
     */
    public void updateWorkspaceComboBox() {
        if (workspaceComboBox != null) {
            workspaceComboBox.reload();
        }
    }

    /**
     * 切换到指定工作区
     * 包括切换环境变量文件、请求集合文件，并刷新 Git 工具栏
     *
     * @param workspace 目标工作区
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

            // 重新加载菜单栏（根据新工作区类型更新 Git 工具栏显示状态）
            refresh();

            log.info("Switched to workspace: {}", workspace.getName());
        } catch (Exception e) {
            log.error("Failed to switch workspace", e);
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.WORKSPACE_OPERATION_FAILED_DETAIL, e.getMessage()));
        }
    }

    /**
     * 更新工作区显示（包括工作区下拉框和 Git 工具栏）
     * 在外部切换工作区后调用（例如从 WorkspacePanel 切换）
     * 会重新加载整个菜单栏以更新 Git 工具栏显示状态
     */
    public void updateWorkspaceDisplay() {
        // 重新加载整个菜单栏以更新 Git 工具栏显示状态
        refresh();
    }

    private void aboutActionPerformed() {
        String iconUrl = getClass().getResource("/icons/icon.png") + "";
        String html = "<html>"
                + "<head>"
                + "<div style='border-radius:16px; border:1px solid " + getThemeBorderColor() + "; padding:20px 28px; min-width:340px; max-width:420px;'>"
                + "<div style='text-align:center;'>"
                + "<img src='" + iconUrl + "' width='56' height='56' style='margin-bottom:10px;'/>"
                + "</div>"
                + "<div style='font-size:16px; font-weight:bold; color:" + getThemeTextColor() + "; text-align:center; margin-bottom:6px;'>EasyPostman</div>"
                + "<div style='font-size:12px; color:" + getThemeSecondaryTextColor() + "; text-align:center; margin-bottom:12px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_VERSION, getCurrentVersion()) + "</div>"
                + "<div style='font-size:10px; color:" + getThemeHintTextColor() + "; margin-bottom:2px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_AUTHOR) + "</div>"
                + "<div style='font-size:10px; color:" + getThemeHintTextColor() + "; margin-bottom:2px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_LICENSE) + "</div>"
                + "<div style='font-size:10px; color:" + getThemeHintTextColor() + "; margin-bottom:8px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_WECHAT) + "</div>"
                + "<hr style='border:none; border-top:1px solid " + getThemeBorderColor() + "; margin:10px 0;'>"
                + "<div style='font-size:9px; margin-bottom:2px;'>"
                + "<a href='https://laker.blog.csdn.net' style='color:" + getThemeLinkColor() + "; text-decoration:none;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_BLOG) + "</a>"
                + "</div>"
                + "<div style='font-size:9px; margin-bottom:2px;'>"
                + "<a href='https://github.com/lakernote' style='color:" + getThemeLinkColor() + "; text-decoration:none;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_GITHUB) + "</a>"
                + "</div>"
                + "<div style='font-size:9px;'>"
                + "<a href='https://gitee.com/lakernote' style='color:" + getThemeLinkColor() + "; text-decoration:none;'>"
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
     * 创建 Git 工具栏面板
     * 根据工作区的 Git 配置情况动态显示不同的操作按钮
     *
     * @param workspace 当前 Git 工作区
     * @return Git 工具栏面板
     */
    private JPanel createGitToolbar(Workspace workspace) {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setOpaque(false);

        try {
            WorkspaceService workspaceService = WorkspaceService.getInstance();
            RemoteStatus remoteStatus = workspaceService.getRemoteStatus(workspace.getId());

            // Commit 按钮（本地提交，始终显示）
            JButton commitButton = createGitButton(
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_COMMIT),
                    GitOperation.COMMIT.getIconName(),
                    e -> performGitOperation(workspace, GitOperation.COMMIT)
            );
            toolbar.add(commitButton);

            // 远程操作按钮（仅在配置了远程仓库时显示）
            if (remoteStatus.hasRemote) {
                // Pull 按钮（拉取远程更新）
                JButton pullButton = createGitButton(
                        I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_PULL),
                        GitOperation.PULL.getIconName(),
                        e -> performGitOperation(workspace, GitOperation.PULL)
                );
                toolbar.add(pullButton);

                // Push 按钮（仅在设置了上游分支时显示）
                if (remoteStatus.hasUpstream) {
                    JButton pushButton = createGitButton(
                            I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_PUSH),
                            GitOperation.PUSH.getIconName(),
                            e -> performGitOperation(workspace, GitOperation.PUSH)
                    );
                    toolbar.add(pushButton);
                }
            }

        } catch (Exception e) {
            log.error("Failed to create Git toolbar buttons", e);
        }

        return toolbar;
    }

    /**
     * 创建 Git 操作按钮
     */
    private JButton createGitButton(String tooltip, String iconPath, ActionListener action) {
        JButton button = new JButton();
        FlatSVGIcon icon = new FlatSVGIcon(iconPath, 18, 18);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor(BUTTON_FOREGROUND_KEY)));
        button.setIcon(icon);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(24, 24));
        button.addActionListener(action);

        // 添加鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setContentAreaFilled(false);
            }
        });

        return button;
    }

    /**
     * 执行 Git 操作（Commit/Pull/Push）
     *
     * @param workspace 目标工作区
     * @param operation Git 操作类型
     */
    private void performGitOperation(Workspace workspace, GitOperation operation) {
        GitOperationDialog dialog = new GitOperationDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace,
                operation
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            // Pull 操作后需要刷新相关面板以显示最新数据
            if (operation == GitOperation.PULL) {
                SingletonFactory.getInstance(RequestCollectionsLeftPanel.class)
                        .switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace));
                SingletonFactory.getInstance(EnvironmentPanel.class)
                        .switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));
            }

            log.info("Git {} operation completed successfully", operation.getDisplayName());
        }
    }

    /**
     * 检查更新
     */
    private void checkUpdate() {
        BeanFactory.getBean(UpdateService.class).checkUpdateManually();
    }
}

