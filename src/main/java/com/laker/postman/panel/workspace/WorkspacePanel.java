package com.laker.postman.panel.workspace;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.panel.SingletonBasePanel;
import com.laker.postman.model.*;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.topmenu.TopMenuBarPanel;
import com.laker.postman.panel.workspace.components.*;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 工作区面板
 * 显示工作区列表，支持创建、切换、管理工作区
 */
@Slf4j
public class WorkspacePanel extends SingletonBasePanel {

    private static final String HTML_START = "<html>";
    private static final String HTML_END = "</html>";
    public static final String HH_MM_SS = "HH:mm:ss";

    private JList<Workspace> workspaceList;
    private DefaultListModel<Workspace> listModel;
    private JPanel infoPanel;
    private JTextArea logArea;
    private transient WorkspaceService workspaceService;

    @Override
    protected void initUI() {
        workspaceService = WorkspaceService.getInstance();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));

        // 创建顶部工具栏
        add(createToolbar(), BorderLayout.NORTH);

        // 创建主要内容区域 - 垂直分割面板
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 上半部分 - 水平分割面板
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topSplitPane.setLeftComponent(createWorkspaceListPanel());
        topSplitPane.setRightComponent(createInfoPanel());
        topSplitPane.setDividerLocation(300);
        topSplitPane.setResizeWeight(0.4);

        // 下半部分 - 日志区域
        JPanel logPanel = createLogPanel();

        mainSplitPane.setTopComponent(topSplitPane);
        mainSplitPane.setBottomComponent(logPanel);
        mainSplitPane.setDividerLocation(400);
        mainSplitPane.setResizeWeight(0.7);

        add(mainSplitPane, BorderLayout.CENTER);

        // 刷新工作区列表
        refreshWorkspaceList();
    }

    /**
     * 创建工具栏
     */
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 新建工作区按钮
        JButton newButton = new JButton(I18nUtil.getMessage(MessageKeys.WORKSPACE_NEW));
        newButton.setIcon(new FlatSVGIcon("icons/plus.svg", 16, 16));
        newButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        newButton.addActionListener(e -> showCreateWorkspaceDialog());
        toolbar.add(newButton);

        // 刷新按钮
        JButton refreshButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_REFRESH));
        refreshButton.setIcon(new FlatSVGIcon("icons/refresh.svg", 16, 16));
        refreshButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        refreshButton.addActionListener(e -> refreshWorkspaceList());
        toolbar.add(refreshButton);

        return toolbar;
    }

    /**
     * 创建工作区列表面板
     */
    private JScrollPane createWorkspaceListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // 创建列表模型和列表
        listModel = new DefaultListModel<>();
        workspaceList = new JList<>(listModel);

        // 设置列表样式
        workspaceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        workspaceList.setCellRenderer(new WorkspaceListCellRenderer());
        workspaceList.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        workspaceList.setFixedCellHeight(50);

        // 添加右键菜单和双击事件
        workspaceList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = workspaceList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    workspaceList.setSelectedIndex(index);
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showContextMenu(e);
                    } else if (e.getClickCount() == 2) {
                        handleDoubleClick();
                    }
                }
            }
        });

        // 添加选择监听器
        workspaceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateInfoPanel();
            }
        });

        JScrollPane scrollPane = new JScrollPane(workspaceList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return scrollPane;
    }

    /**
     * 创建信息面板
     */
    private JPanel createInfoPanel() {
        infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.WORKSPACE_INFO)));
        infoPanel.setPreferredSize(new Dimension(400, 0));

        JLabel welcomeLabel = new JLabel("<html><center>" +
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_WELCOME_MESSAGE) +
                "</center></html>");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomeLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.ITALIC, 12));
        welcomeLabel.setForeground(Color.GRAY);

        infoPanel.add(welcomeLabel, BorderLayout.CENTER);

        return infoPanel;
    }

    /**
     * 创建日志面板
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG));
        panel.setBorder(border);
        panel.setPreferredSize(new Dimension(0, 150));

        // 创建日志文本区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        logArea.setBackground(new Color(248, 248, 248));

        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(logScrollPane, BorderLayout.CENTER);

        // 添加清空日志按钮
        JPanel logToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        JButton clearLogButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        clearLogButton.setIcon(new FlatSVGIcon("icons/clear.svg", 20, 20));
        clearLogButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        clearLogButton.addActionListener(e -> logArea.setText(""));
        logToolbar.add(clearLogButton);
        panel.add(logToolbar, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 显示创建工作区对话框
     */
    private void showCreateWorkspaceDialog() {
        WorkspaceCreateDialog dialog = new WorkspaceCreateDialog(
                SwingUtilities.getWindowAncestor(this)
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            refreshWorkspaceList();
        }
    }

    /**
     * 显示右键菜单
     */
    private void showContextMenu(MouseEvent e) {
        Workspace workspace = workspaceList.getSelectedValue();
        if (workspace != null) {
            JPopupMenu menu = createWorkspaceContextMenu(workspace);
            menu.show(workspaceList, e.getX(), e.getY());
        }
    }

    /**
     * 创建工作区右键菜单
     */
    private JPopupMenu createWorkspaceContextMenu(Workspace workspace) {
        JPopupMenu menu = new JPopupMenu();

        addSwitchMenuItem(menu, workspace);
        addGitMenuItems(menu, workspace);
        addManagementMenuItems(menu, workspace);

        return menu;
    }

    private void addSwitchMenuItem(JPopupMenu menu, Workspace workspace) {
        Workspace current = workspaceService.getCurrentWorkspace();
        if (current == null || !current.getId().equals(workspace.getId())) {
            JMenuItem switchItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_SWITCH));
            switchItem.setIcon(new FlatSVGIcon("icons/switch.svg", 16, 16));
            switchItem.addActionListener(e -> switchToWorkspace(workspace));
            menu.add(switchItem);
            if (!WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
                menu.addSeparator();
            }
        }
    }

    private void addGitMenuItems(JPopupMenu menu, Workspace workspace) {
        if (workspace.getType() != WorkspaceType.GIT) {
            return;
        }

        // 根据工作区类型显示不同的Git操作
        if (workspace.getGitRepoSource() == GitRepoSource.INITIALIZED) {
            addInitializedGitMenuItems(menu, workspace);
        }

        addStandardGitMenuItems(menu, workspace);
        menu.addSeparator();
    }

    private void addInitializedGitMenuItems(JPopupMenu menu, Workspace workspace) {
        try {
            RemoteStatus remoteStatus = workspaceService.getRemoteStatus(workspace.getId());
            if (!remoteStatus.hasRemote) {
                // 还未配置远程仓库
                JMenuItem configRemoteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_REMOTE_CONFIG_TITLE));
                configRemoteItem.setIcon(new FlatSVGIcon("icons/git.svg", 16, 16));
                configRemoteItem.addActionListener(e -> configureRemoteRepository(workspace));
                menu.add(configRemoteItem);
            }
        } catch (Exception ex) {
            log.warn("Failed to check remote status for workspace: {}", workspace.getId(), ex);
        }
    }

    private void addStandardGitMenuItems(JPopupMenu menu, Workspace workspace) {

        // 1.提交操作 始终显示
        JMenuItem commitItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_COMMIT));
        commitItem.setIcon(new FlatSVGIcon("icons/save.svg", 16, 16));
        commitItem.addActionListener(e -> performGitCommit(workspace));
        menu.add(commitItem);

        try {
            RemoteStatus remoteStatus = workspaceService.getRemoteStatus(workspace.getId());
            if (remoteStatus.hasRemote) { // 2.只有已配置远程仓库的工作区才显示拉取操作
                JMenuItem pullItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_PULL));
                pullItem.setIcon(new FlatSVGIcon("icons/download.svg", 16, 16));
                pullItem.addActionListener(e -> performGitPull(workspace));
                menu.add(pullItem);

                if (remoteStatus.hasUpstream) { // 3.只有有上游分支的工作区才显示推送操作
                    JMenuItem pushItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_PUSH));
                    pushItem.setIcon(new FlatSVGIcon("icons/upload.svg", 16, 16));
                    pushItem.addActionListener(e -> performGitPush(workspace));
                    menu.add(pushItem);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to check remote repository for workspace: {}", workspace.getId(), ex);
        }
    }

    private void addManagementMenuItems(JPopupMenu menu, Workspace workspace) {
        // 默认工作区不可重命名和删除
        if (!WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            // 重命名
            JMenuItem renameItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_RENAME));
            renameItem.setIcon(new FlatSVGIcon("icons/refresh.svg", 16, 16));
            renameItem.addActionListener(e -> renameWorkspace(workspace));
            menu.add(renameItem);

            // 删除
            JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_DELETE));
            deleteItem.setIcon(new FlatSVGIcon("icons/close.svg", 16, 16));
            deleteItem.addActionListener(e -> deleteWorkspace(workspace));
            menu.add(deleteItem);
        }
    }

    /**
     * 处理双击事件
     */
    private void handleDoubleClick() {
        Workspace workspace = workspaceList.getSelectedValue();
        if (workspace != null) {
            switchToWorkspace(workspace);
        }
    }

    /**
     * 切换到指定工作区
     */
    private void switchToWorkspace(Workspace workspace) {
        try {
            workspaceService.switchWorkspace(workspace.getId());
            // 切换环境变量文件
            SingletonFactory.getInstance(EnvironmentPanel.class).switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));
            // 切换请求集合文件
            SingletonFactory.getInstance(RequestCollectionsLeftPanel.class).switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace));
            // 更新顶部菜单栏工作区显示
            SingletonFactory.getInstance(TopMenuBarPanel.class).updateWorkspaceDisplay();
            refreshWorkspaceList();
        } catch (Exception e) {
            log.error("Failed to switch workspace", e);
        }
    }

    /**
     * Git拉取操作
     */
    private void performGitPull(Workspace workspace) {
        GitOperationDialog dialog = new GitOperationDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace,
                GitOperation.PULL
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            // 刷新 requests 和 env 面板
            SingletonFactory.getInstance(RequestCollectionsLeftPanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace));
            SingletonFactory.getInstance(EnvironmentPanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));
            refreshWorkspaceList();
        }
    }

    /**
     * Git提交操作
     */
    private void performGitCommit(Workspace workspace) {
        GitOperationDialog dialog = new GitOperationDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace,
                GitOperation.COMMIT
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            refreshWorkspaceList();
        }
    }

    /**
     * Git推送操作
     */
    private void performGitPush(Workspace workspace) {
        GitOperationDialog dialog = new GitOperationDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace,
                GitOperation.PUSH
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            // 刷新 requests 和 env 面板
            SingletonFactory.getInstance(RequestCollectionsLeftPanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace));
            SingletonFactory.getInstance(EnvironmentPanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));
            refreshWorkspaceList();
        }
    }

    /**
     * 重命名工作区
     */
    private void renameWorkspace(Workspace workspace) {
        String newName = JOptionPane.showInputDialog(
                this,
                I18nUtil.getMessage(MessageKeys.WORKSPACE_NAME) + ":",
                workspace.getName()
        );

        if (newName != null && !newName.trim().isEmpty() && !newName.equals(workspace.getName())) {
            try {
                workspaceService.renameWorkspace(workspace.getId(), newName.trim());
                refreshWorkspaceList();
                // 如果重命名的是当前工作区，更新顶部菜单栏显示
                Workspace current = workspaceService.getCurrentWorkspace();
                if (current != null && current.getId().equals(workspace.getId())) {
                    SingletonFactory.getInstance(TopMenuBarPanel.class).updateWorkspaceDisplay();
                }
            } catch (Exception e) {
                log.error("Failed to rename workspace", e);
            }
        }
    }

    /**
     * 删除工作区
     */
    private void deleteWorkspace(Workspace workspace) {
        String[] options = {
                I18nUtil.getMessage(MessageKeys.WORKSPACE_DELETE),
                I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL)
        };

        int choice = JOptionPane.showOptionDialog(
                this,
                I18nUtil.getMessage(MessageKeys.WORKSPACE_DELETE_CONFIRM, workspace.getName()),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_DELETE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0] // default option
        );

        if (choice == 0) { // 删除
            try {
                // 检查是否删除的是当前工作区
                boolean isDeletingCurrentWorkspace = workspaceService.getCurrentWorkspace() != null &&
                        workspaceService.getCurrentWorkspace().getId().equals(workspace.getId());

                workspaceService.deleteWorkspace(workspace.getId());

                // 如果删除的是当前工作区，需要切换到新的当前工作区并刷新相关UI
                if (isDeletingCurrentWorkspace) {
                    Workspace newCurrentWorkspace = workspaceService.getCurrentWorkspace();
                    if (newCurrentWorkspace != null) {
                        // 切换环境变量文件
                        SingletonFactory.getInstance(EnvironmentPanel.class).switchWorkspaceAndRefreshUI(
                                SystemUtil.getEnvPathForWorkspace(newCurrentWorkspace));
                        // 切换请求集合文件
                        SingletonFactory.getInstance(RequestCollectionsLeftPanel.class).switchWorkspaceAndRefreshUI(
                                SystemUtil.getCollectionPathForWorkspace(newCurrentWorkspace));

                    }
                }

                refreshWorkspaceList();
                SingletonFactory.getInstance(TopMenuBarPanel.class).updateWorkspaceDisplay();
            } catch (Exception e) {
                log.error("Failed to delete workspace", e);
            }
        }
    }

    /**
     * 配置远程仓库
     */
    private void configureRemoteRepository(Workspace workspace) {
        RemoteConfigDialog dialog = new RemoteConfigDialog(
                SwingUtilities.getWindowAncestor(this), workspace);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            try {
                workspaceService.addRemoteRepository(
                        workspace.getId(),
                        dialog.getRemoteUrl(),
                        dialog.getRemoteBranch(),
                        dialog.getAuthType(),
                        dialog.getUsername(),
                        dialog.getPassword(),
                        dialog.getToken()
                );
                refreshWorkspaceList();
            } catch (Exception e) {
                log.error("Failed to configure remote repository", e);
                logError("Error: " + e.getMessage());
                showError(e.getMessage());
            }
        }
    }

    /**
     * 刷新工作区列表
     */
    private void refreshWorkspaceList() {
        try {
            List<Workspace> workspaces = workspaceService.getAllWorkspaces();
            listModel.clear();
            for (Workspace workspace : workspaces) {
                listModel.addElement(workspace);
            }

            // 选中当前工作区
            Workspace current = workspaceService.getCurrentWorkspace();
            if (current != null) {
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (listModel.getElementAt(i).getId().equals(current.getId())) {
                        workspaceList.setSelectedIndex(i);
                        break;
                    }
                }
            }

            updateInfoPanel();
        } catch (Exception e) {
            log.error("Failed to refresh workspace list", e);
        }
    }

    /**
     * 更新信息面板
     */
    private void updateInfoPanel() {
        Workspace selected = workspaceList.getSelectedValue();
        infoPanel.removeAll();

        if (selected != null) {
            infoPanel.add(new WorkspaceDetailPanel(selected), BorderLayout.CENTER);
        } else {
            JLabel welcomeLabel = new JLabel(HTML_START + "<center>" +
                    I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_WELCOME_MESSAGE) +
                    "</center>" + HTML_END);
            welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            welcomeLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.ITALIC, 12));
            welcomeLabel.setForeground(Color.GRAY);
            infoPanel.add(welcomeLabel, BorderLayout.CENTER);
        }

        infoPanel.revalidate();
        infoPanel.repaint();
    }

    /**
     * 记录错误日志
     */
    private void logError(String message) {
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat(HH_MM_SS);
                String timestamp = sdf.format(new Date());
                logArea.append("[" + timestamp + "] ERROR: " + message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    /**
     * 记录Git操作结果到日志区域
     */
    public void logGitOperationResult(WorkspaceService.GitOperationResult result) {
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat(HH_MM_SS);
                String timestamp = sdf.format(new Date());

                // 操作标题
                logArea.append("=== " + result.operationType + "操作结果 ===\n");
                logArea.append("[" + timestamp + "] " + result.message + "\n");

                // 详细信息
                if (!result.details.isEmpty()) {
                    logArea.append(result.details);
                }

                // 受影响的文件总结
                if (!result.affectedFiles.isEmpty()) {
                    logArea.append("受影响的文件总数: " + result.affectedFiles.size() + "\n");
                }

                // 操作状态
                if (result.success) {
                    logArea.append("[" + timestamp + "] ✅ " + result.operationType + "操作成功完成\n");
                } else {
                    logArea.append("[" + timestamp + "] ❌ " + result.operationType + "操作失败\n");
                }

                logArea.append("\n"); // 添加空行分隔
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                I18nUtil.getMessage(MessageKeys.ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    protected void registerListeners() {
        // 监听器已在initUI中注册
    }
}
