package com.laker.postman.panel.workspace;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSidebarToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.common.component.button.PlusButton;
import com.laker.postman.common.component.dialog.TextInputDialog;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.GitOperationResult;
import com.laker.postman.model.GitRepoSource;
import com.laker.postman.model.RemoteStatus;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import com.laker.postman.panel.topmenu.TopMenuBar;
import com.laker.postman.panel.workspace.components.*;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 工作区面板
 * 显示工作区列表，支持创建、切换、管理工作区
 */
@Slf4j
public class WorkspacePanel extends UiSingletonPanel {

    private static final String HTML_START = "<html>";
    private static final String HTML_END = "</html>";
    private static final String WORKSPACE_DETAIL_DIVIDER_APPLIED_PROPERTY =
            "EasyPostman.workspace.detailDividerApplied";
    private static final int WORKSPACE_DETAIL_DEFAULT_HEIGHT = 360;
    private static final int WORKSPACE_DETAIL_MIN_HEIGHT = 320;
    private static final int WORKSPACE_DETAIL_MAX_HEIGHT = 400;
    private static final int WORKSPACE_LOG_MIN_HEIGHT = 180;
    private static final double WORKSPACE_DETAIL_RESIZE_WEIGHT = 0.34;
    public static final String HH_MM_SS = "HH:mm:ss";

    private JList<Workspace> workspaceList;
    private DefaultListModel<Workspace> listModel;
    private SearchTextField workspaceSearchField;
    private List<Workspace> allWorkspaces = new ArrayList<>();
    private JPanel infoPanel;
    private JTextArea logArea;
    private transient WorkspaceService workspaceService;

    @Override
    protected void initUI() {
        workspaceService = WorkspaceService.getInstance();
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyBackground(this);

        JPanel leftPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(leftPanel);
        leftPanel.add(createToolbar(), BorderLayout.NORTH);
        leftPanel.add(createWorkspaceListPanel(), BorderLayout.CENTER);

        JSplitPane rightSplitPane = AppToolWindowChrome.createVerticalInnerSplitPane(
                createInfoPanel(),
                createLogPanel(),
                WORKSPACE_DETAIL_DEFAULT_HEIGHT
        );
        rightSplitPane.setResizeWeight(WORKSPACE_DETAIL_RESIZE_WEIGHT);
        installInitialWorkspaceDetailDivider(rightSplitPane);

        JSplitPane mainSplitPane = AppToolWindowChrome.createHorizontalCardSplitPane(
                leftPanel,
                rightSplitPane,
                AppToolWindowChrome.DEFAULT_SIDE_WIDTH
        );
        mainSplitPane.setResizeWeight(0.0);

        add(mainSplitPane, BorderLayout.CENTER);

        // 刷新工作区列表
        refreshWorkspaceList();
    }

    /**
     * 创建工具栏
     */
    private JPanel createToolbar() {
        // 新建工作区按钮
        JButton newButton = new PlusButton();
        newButton.addActionListener(e -> showCreateWorkspaceDialog());

        workspaceSearchField = new SearchTextField();
        workspaceSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyWorkspaceFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyWorkspaceFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyWorkspaceFilter();
            }
        });
        return new ToolWindowSidebarToolbar(newButton, workspaceSearchField);
    }

    /**
     * 创建工作区列表面板
     */
    private JScrollPane createWorkspaceListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(panel);
        // 创建列表模型和列表
        listModel = new DefaultListModel<>();
        workspaceList = new JList<>(listModel);

        // 设置列表样式
        workspaceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        workspaceList.setCellRenderer(new WorkspaceListCellRenderer());
        // 动态计算单元格高度，基于字体大小
        workspaceList.setFixedCellHeight(WorkspaceListCellRenderer.calculateCellHeight());

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

        workspaceList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleListKeyPressed(e);
            }
        });

        // 拖拽排序支持
        setupDragAndDrop();

        JScrollPane scrollPane = new JScrollPane(workspaceList);
        ToolWindowSurfaceStyle.applyListScrollPaneCard(scrollPane, workspaceList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return scrollPane;
    }

    /**
     * 创建信息面板
     */
    private JPanel createInfoPanel() {
        infoPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(infoPanel);
        infoPanel.setPreferredSize(new Dimension(400, 0));

        JLabel welcomeLabel = new JLabel("<html><center>" +
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_WELCOME_MESSAGE) +
                "</center></html>");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomeLabel.setFont(FontsUtil.getDefaultFont(Font.ITALIC));
        welcomeLabel.setForeground(ModernColors.getTextHint());

        infoPanel.add(welcomeLabel, BorderLayout.CENTER);

        return infoPanel;
    }

    /**
     * 创建日志面板
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        panel.setMinimumSize(new Dimension(0, WORKSPACE_LOG_MIN_HEIGHT));
        panel.setPreferredSize(new Dimension(0, WORKSPACE_LOG_MIN_HEIGHT));

        // 创建日志文本区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        ToolWindowSurfaceStyle.applyTextComponentCard(logArea);

        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        ToolWindowSurfaceStyle.applyScrollPaneCard(logScrollPane);
        panel.add(logScrollPane, BorderLayout.CENTER);

        // 添加清空日志按钮
        ClearButton clearLogButton = new ClearButton();
        clearLogButton.addActionListener(e -> logArea.setText(""));
        JPanel logToolbar = ToolWindowActionToolbar.right(clearLogButton);
        panel.add(logToolbar, BorderLayout.SOUTH);

        return panel;
    }

    private static void installInitialWorkspaceDetailDivider(JSplitPane splitPane) {
        splitPane.putClientProperty(WORKSPACE_DETAIL_DIVIDER_APPLIED_PROPERTY, Boolean.FALSE);
        ComponentAdapter listener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (applyInitialWorkspaceDetailDivider(splitPane)) {
                    splitPane.removeComponentListener(this);
                }
            }
        };
        splitPane.addComponentListener(listener);
        SwingUtilities.invokeLater(() -> {
            if (applyInitialWorkspaceDetailDivider(splitPane)) {
                splitPane.removeComponentListener(listener);
            }
        });
    }

    private static boolean applyInitialWorkspaceDetailDivider(JSplitPane splitPane) {
        if (Boolean.TRUE.equals(splitPane.getClientProperty(WORKSPACE_DETAIL_DIVIDER_APPLIED_PROPERTY))) {
            return true;
        }
        int splitHeight = splitPane.getHeight();
        if (splitHeight <= 0) {
            return false;
        }
        splitPane.setDividerLocation(defaultWorkspaceDetailDividerLocation(splitHeight, splitPane.getDividerSize()));
        splitPane.putClientProperty(WORKSPACE_DETAIL_DIVIDER_APPLIED_PROPERTY, Boolean.TRUE);
        return true;
    }

    static int defaultWorkspaceDetailDividerLocation(int splitHeight, int dividerSize) {
        int usableHeight = Math.max(0, splitHeight - Math.max(0, dividerSize));
        if (usableHeight <= 0) {
            return WORKSPACE_DETAIL_DEFAULT_HEIGHT;
        }
        int ratioLocation = (int) Math.round(usableHeight * WORKSPACE_DETAIL_RESIZE_WEIGHT);
        int desiredLocation = Math.max(WORKSPACE_DETAIL_DEFAULT_HEIGHT,
                Math.max(WORKSPACE_DETAIL_MIN_HEIGHT, ratioLocation));
        desiredLocation = Math.min(desiredLocation, WORKSPACE_DETAIL_MAX_HEIGHT);
        int maxLocation = usableHeight - WORKSPACE_LOG_MIN_HEIGHT;
        if (maxLocation < WORKSPACE_DETAIL_MIN_HEIGHT) {
            return Math.max(0, maxLocation);
        }
        return Math.min(desiredLocation, maxLocation);
    }

    /**
     * 设置拖拽排序功能
     */
    private void setupDragAndDrop() {
        workspaceList.setDragEnabled(true);
        workspaceList.setDropMode(DropMode.INSERT);
        workspaceList.setTransferHandler(new TransferHandler() {
            private int fromIndex = -1;

            @Override
            protected Transferable createTransferable(JComponent c) {
                fromIndex = workspaceList.getSelectedIndex();
                Workspace selected = workspaceList.getSelectedValue();
                return new StringSelection(selected != null ? selected.getName() : "");
            }

            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDrop() && !isWorkspaceSearchActive();
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                int toIndex = dl.getIndex();
                if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return false;

                Workspace moved = listModel.getElementAt(fromIndex);
                listModel.remove(fromIndex);
                if (toIndex > fromIndex) toIndex--;
                listModel.add(toIndex, moved);
                workspaceList.setSelectedIndex(toIndex);

                // 持久化顺序
                persistWorkspaceOrder();
                return true;
            }
        });
    }

    /**
     * 持久化工作区顺序
     */
    private void persistWorkspaceOrder() {
        List<String> idOrder = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            idOrder.add(listModel.get(i).getId());
        }
        workspaceService.saveWorkspaceOrder(idOrder);
    }

    private boolean isWorkspaceSearchActive() {
        return workspaceSearchField != null && !workspaceSearchField.getText().trim().isEmpty();
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
            // 更新顶部菜单栏的工作区下拉框（不需要重新加载整个菜单栏）
            UiSingletonFactory.getInstance(TopMenuBar.class).updateWorkspaceComboBox();
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
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

        addSwitchMenuItem(menu, workspace);
        addGitMenuItems(menu, workspace);
        addManagementMenuItems(menu, workspace);

        return menu;
    }

    private void addSwitchMenuItem(JPopupMenu menu, Workspace workspace) {
        Workspace current = workspaceService.getCurrentWorkspace();
        if (current == null || !current.getId().equals(workspace.getId())) {
            JMenuItem switchItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_SWITCH));
            switchItem.setIcon(IconUtil.createThemed("icons/switch.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
            switchItem.addActionListener(e -> switchToWorkspace(workspace));
            menu.add(switchItem);
            if (!WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
                menu.addSeparator();
            }
        }
    }

    private void addGitMenuItems(JPopupMenu menu, Workspace workspace) {
        if (workspace.getType() != WorkspaceType.GIT) {
            // 所有本地工作区都可以转换为Git工作区（包括默认工作区）
            // 默认工作区位于 workspaces/default/ 子目录，与其他工作区平级，根目录不参与 git
            JMenuItem convertToGitItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_TO_GIT));
            convertToGitItem.setIcon(IconUtil.create("icons/git.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
            convertToGitItem.addActionListener(e -> convertToGitWorkspace(workspace));
            menu.add(convertToGitItem);
            // 只有非默认工作区才添加分隔符（因为后面还有重命名和删除选项）
            if (!WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
                menu.addSeparator();
            }
            return;
        }

        // 根据工作区类型显示不同的Git操作
        if (workspace.getGitRepoSource() == GitRepoSource.INITIALIZED) {
            addInitializedGitMenuItems(menu, workspace);
        }

        addStandardGitMenuItems(menu, workspace);
        // 只有非默认工作区才添加分隔符（因为后面还有重命名和删除选项）
        if (!WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            menu.addSeparator();
        }
    }

    private void addInitializedGitMenuItems(JPopupMenu menu, Workspace workspace) {
        try {
            RemoteStatus remoteStatus = workspaceService.getRemoteStatus(workspace.getId());
            if (!remoteStatus.hasRemote) {
                // 还未配置远程仓库
                JMenuItem configRemoteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_REMOTE_CONFIG_TITLE));
                configRemoteItem.setIcon(IconUtil.create("icons/git.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
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
        commitItem.setIcon(IconUtil.createThemed(
                GitOperationPresentation.getIconName(GitOperation.COMMIT),
                IconUtil.SIZE_SMALL,
                IconUtil.SIZE_SMALL));
        commitItem.addActionListener(e -> performGitCommit(workspace));
        menu.add(commitItem);

        // 2.查看历史 始终显示
        JMenuItem historyItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_HISTORY));
        historyItem.setIcon(IconUtil.createThemed("icons/history.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        historyItem.addActionListener(e -> showGitHistory(workspace));
        menu.add(historyItem);

        JMenuItem diffItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_DIFF));
        diffItem.setIcon(IconUtil.createThemed("icons/detail.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        diffItem.addActionListener(e -> showGitDiff(workspace));
        menu.add(diffItem);

        JMenuItem branchesItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_BRANCHES));
        branchesItem.setIcon(IconUtil.createThemed("icons/git.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        branchesItem.addActionListener(e -> showGitBranches(workspace));
        menu.add(branchesItem);

        try {
            RemoteStatus remoteStatus = workspaceService.getRemoteStatus(workspace.getId());
            if (remoteStatus.hasRemote) { // 3.只有已配置远程仓库的工作区才显示拉取操作
                JMenuItem pullItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_PULL));
                pullItem.setIcon(IconUtil.createThemed(
                        GitOperationPresentation.getIconName(GitOperation.PULL),
                        IconUtil.SIZE_SMALL,
                        IconUtil.SIZE_SMALL));
                pullItem.addActionListener(e -> performGitPull(workspace));
                menu.add(pullItem);

                if (remoteStatus.hasUpstream) { // 4.只有有上游分支的工作区才显示推送操作
                    JMenuItem pushItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_PUSH));
                    pushItem.setIcon(IconUtil.createThemed(
                            GitOperationPresentation.getIconName(GitOperation.PUSH),
                            IconUtil.SIZE_SMALL,
                            IconUtil.SIZE_SMALL));
                    pushItem.addActionListener(e -> performGitPush(workspace));
                    menu.add(pushItem);
                }

                // 添加更新认证菜单项
                JMenuItem updateAuthItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_UPDATE));
                updateAuthItem.setIcon(IconUtil.createThemed("icons/security.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
                updateAuthItem.addActionListener(e -> updateGitAuthentication(workspace));
                menu.add(updateAuthItem);
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
            renameItem.setIcon(IconUtil.createThemed("icons/refresh.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
            renameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
            renameItem.addActionListener(e -> renameWorkspace(workspace));
            menu.add(renameItem);

            // 删除
            JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_DELETE));
            deleteItem.setIcon(IconUtil.createThemed("icons/close.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
            deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
            deleteItem.addActionListener(e -> deleteWorkspace(workspace));
            menu.add(deleteItem);
        }
    }

    private void handleListKeyPressed(KeyEvent e) {
        Workspace workspace = workspaceList.getSelectedValue();
        if (workspace == null || WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_F2) {
            renameWorkspace(workspace);
            e.consume();
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            deleteWorkspace(workspace);
            e.consume();
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
            saveCurrentWorkspaceScopedPanels();
            workspaceService.switchWorkspace(workspace.getId());
            // 切换环境变量文件
            UiSingletonFactory.getInstance(EnvironmentPanel.class).switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));
            // 切换请求集合文件
            UiSingletonFactory.getInstance(CollectionTreePanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace), () -> {
                        refreshExistingWorkspaceScopedPanels();
                        // 更新顶部菜单栏工作区显示
                        UiSingletonFactory.getInstance(TopMenuBar.class).updateWorkspaceDisplay();
                        refreshWorkspaceList();
                    });
        } catch (Exception e) {
            log.error("Failed to switch workspace", e);
        }
    }

    private void saveCurrentWorkspaceScopedPanels() {
        UiSingletonFactory.getExistingInstance(FunctionalPanel.class).ifPresent(FunctionalPanel::save);
        UiSingletonFactory.getExistingInstance(PerformancePanel.class).ifPresent(PerformancePanel::save);
    }

    private void refreshExistingWorkspaceScopedPanels() {
        UiSingletonFactory.getExistingInstance(FunctionalPanel.class)
                .ifPresent(FunctionalPanel::switchWorkspaceAndRefreshUI);
        UiSingletonFactory.getExistingInstance(PerformancePanel.class)
                .ifPresent(PerformancePanel::switchWorkspaceAndRefreshUI);
    }

    /**
     * Git拉取操作
     */
    private void performGitPull(Workspace workspace) {
        saveCurrentWorkspaceScopedPanels();
        GitOperationDialog dialog = new GitOperationDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace,
                GitOperation.PULL
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            // 刷新 requests 和 env 面板
            UiSingletonFactory.getInstance(CollectionTreePanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace), () -> {
                        refreshExistingWorkspaceScopedPanels();
                        refreshWorkspaceList();
                    });
            UiSingletonFactory.getInstance(EnvironmentPanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));
        }
    }

    /**
     * Git提交操作
     */
    private void performGitCommit(Workspace workspace) {
        saveCurrentWorkspaceScopedPanels();
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
        saveCurrentWorkspaceScopedPanels();
        GitOperationDialog dialog = new GitOperationDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace,
                GitOperation.PUSH
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            // 刷新 requests 和 env 面板
            UiSingletonFactory.getInstance(CollectionTreePanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace), () -> {
                        refreshExistingWorkspaceScopedPanels();
                        refreshWorkspaceList();
                    });
            UiSingletonFactory.getInstance(EnvironmentPanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));
        }
    }

    /**
     * 显示 Git 历史记录
     */
    private void showGitHistory(Workspace workspace) {
        GitHistoryDialog dialog = new GitHistoryDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace
        );
        dialog.setVisible(true);

        // 如果恢复了版本，需要刷新请求集合和环境变量面板
        if (dialog.isNeedRefresh()) {
            UiSingletonFactory.getInstance(CollectionTreePanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace), () -> {
                        refreshExistingWorkspaceScopedPanels();
                        refreshWorkspaceList();
                    });
            UiSingletonFactory.getInstance(EnvironmentPanel.class)
                    .switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));
        }
    }

    private void showGitBranches(Workspace workspace) {
        Workspace current = workspaceService.getCurrentWorkspace();
        boolean isCurrentWorkspace = current != null && current.getId().equals(workspace.getId());
        if (isCurrentWorkspace) {
            saveCurrentWorkspaceScopedPanels();
        }

        GitBranchDialog dialog = new GitBranchDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace
        );
        dialog.setVisible(true);

        if (dialog.isNeedRefresh()) {
            if (isCurrentWorkspace) {
                UiSingletonFactory.getInstance(CollectionTreePanel.class)
                        .switchWorkspaceAndRefreshUI(SystemUtil.getCollectionPathForWorkspace(workspace), () -> {
                            refreshExistingWorkspaceScopedPanels();
                            refreshWorkspaceList();
                        });
                UiSingletonFactory.getInstance(EnvironmentPanel.class)
                        .switchWorkspaceAndRefreshUI(SystemUtil.getEnvPathForWorkspace(workspace));
            } else {
                refreshWorkspaceList();
            }
        }
    }

    private void showGitDiff(Workspace workspace) {
        Workspace current = workspaceService.getCurrentWorkspace();
        boolean isCurrentWorkspace = current != null && current.getId().equals(workspace.getId());
        if (isCurrentWorkspace) {
            saveCurrentWorkspaceScopedPanels();
        }

        GitDiffDialog dialog = new GitDiffDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace
        );
        dialog.setVisible(true);
    }

    /**
     * 重命名工作区
     */
    private void renameWorkspace(Workspace workspace) {
        TextInputDialog.showRequiredName(
                this,
                I18nUtil.getMessage(MessageKeys.WORKSPACE_RENAME),
                workspace.getName(),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_NAME_REQUIRED)
        ).ifPresent(newName -> {
            if (newName.equals(workspace.getName())) {
                return;
            }
            try {
                workspaceService.renameWorkspace(workspace.getId(), newName);
                refreshWorkspaceList();
                // 如果重命名的是当前工作区，更新顶部菜单栏的工作区下拉框
                Workspace current = workspaceService.getCurrentWorkspace();
                if (current != null && current.getId().equals(workspace.getId())) {
                    // 只更新下拉框，不需要重新加载整个菜单栏（工作区类型未变）
                    UiSingletonFactory.getInstance(TopMenuBar.class).updateWorkspaceComboBox();
                }
            } catch (Exception e) {
                log.error("Failed to rename workspace", e);
            }
        });
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
                        UiSingletonFactory.getInstance(EnvironmentPanel.class).switchWorkspaceAndRefreshUI(
                                SystemUtil.getEnvPathForWorkspace(newCurrentWorkspace));
                        // 切换请求集合文件
                        UiSingletonFactory.getInstance(CollectionTreePanel.class).switchWorkspaceAndRefreshUI(
                                SystemUtil.getCollectionPathForWorkspace(newCurrentWorkspace), this::refreshExistingWorkspaceScopedPanels);

                    }
                }

                refreshWorkspaceList();
                UiSingletonFactory.getInstance(TopMenuBar.class).updateWorkspaceDisplay();
            } catch (Exception e) {
                log.error("Failed to delete workspace", e);
            }
        }
    }

    /**
     * 转换本地工作区为Git工作区
     */
    private void convertToGitWorkspace(Workspace workspace) {
        // 使用自定义对话框，合并分支名输入和确认步骤
        ConvertToGitDialog dialog = new ConvertToGitDialog(
                SwingUtilities.getWindowAncestor(this),
                workspace
        );
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        // 执行转换
        try {
            workspaceService.convertLocalToGit(workspace, dialog.getBranchName());
            refreshWorkspaceList();

            // 如果转换的是当前工作区，需要更新顶部菜单栏
            Workspace current = workspaceService.getCurrentWorkspace();
            if (current != null && current.getId().equals(workspace.getId())) {
                UiSingletonFactory.getInstance(TopMenuBar.class).updateWorkspaceDisplay();
            }

            logSuccess("Successfully converted workspace '" + workspace.getName() + "' to Git workspace (branch: " + dialog.getBranchName() + ")");
        } catch (Exception e) {
            log.error("Failed to convert workspace to Git", e);
            logError("Failed to convert workspace: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_FAILED) + ": " + e.getMessage(),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.ERROR_MESSAGE
            );
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
            refreshWorkspaceList();
        }
    }

    /**
     * 更新 Git 认证信息
     */
    private void updateGitAuthentication(Workspace workspace) {
        UpdateAuthDialog dialog = new UpdateAuthDialog(
                SwingUtilities.getWindowAncestor(this), workspace);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            refreshWorkspaceList();
            String logMessage = "Git authentication updated successfully for workspace: " + workspace.getName();

            // 如果更新了SSH密钥，添加额外提示
            if (dialog.getAuthType() == GitAuthType.SSH_KEY) {
                logMessage += " (SSH session cache cleared)";
            }

            logSuccess(logMessage);
        }
    }

    /**
     * 刷新工作区列表
     */
    private void refreshWorkspaceList() {
        try {
            allWorkspaces = new ArrayList<>(workspaceService.getAllWorkspaces());
            applyWorkspaceFilter();
        } catch (Exception e) {
            log.error("Failed to refresh workspace list", e);
        }
    }

    private void applyWorkspaceFilter() {
        if (listModel == null) {
            return;
        }

        String query = workspaceSearchField == null ? "" : workspaceSearchField.getText().trim();
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        listModel.clear();
        for (Workspace workspace : allWorkspaces) {
            if (normalizedQuery.isEmpty() || matchesWorkspaceSearch(workspace, normalizedQuery)) {
                listModel.addElement(workspace);
            }
        }

        if (workspaceSearchField != null) {
            workspaceSearchField.setNoResult(!normalizedQuery.isEmpty() && listModel.isEmpty());
        }
        if (workspaceList != null) {
            workspaceList.setDragEnabled(normalizedQuery.isEmpty());
        }
        selectCurrentWorkspaceInVisibleList();
        updateInfoPanel();
    }

    private boolean matchesWorkspaceSearch(Workspace workspace, String normalizedQuery) {
        return containsIgnoreCase(workspace.getName(), normalizedQuery)
                || containsIgnoreCase(workspace.getDescription(), normalizedQuery)
                || containsIgnoreCase(workspace.getPath(), normalizedQuery)
                || containsIgnoreCase(workspace.getGitRemoteUrl(), normalizedQuery)
                || containsIgnoreCase(workspace.getCurrentBranch(), normalizedQuery)
                || (workspace.getType() != null
                && workspace.getType().name().toLowerCase(Locale.ROOT).contains(normalizedQuery));
    }

    private static boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private void selectCurrentWorkspaceInVisibleList() {
        if (workspaceList == null) {
            return;
        }
        workspaceList.clearSelection();
        Workspace current = workspaceService.getCurrentWorkspace();
        if (current == null) {
            return;
        }
        for (int i = 0; i < listModel.getSize(); i++) {
            if (listModel.getElementAt(i).getId().equals(current.getId())) {
                workspaceList.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * 更新信息面板
     */
    private void updateInfoPanel() {
        Workspace selected = workspaceList.getSelectedValue();
        infoPanel.removeAll();

        if (selected != null) {
            infoPanel.add(new WorkspaceDetailPanel(
                    selected,
                    () -> showGitBranches(selected),
                    () -> showGitDiff(selected)
            ), BorderLayout.CENTER);
        } else {
            JLabel welcomeLabel = new JLabel(HTML_START + "<center>" +
                    I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_WELCOME_MESSAGE) +
                    "</center>" + HTML_END);
            welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            welcomeLabel.setFont(FontsUtil.getDefaultFont(Font.ITALIC));
            welcomeLabel.setForeground(ModernColors.getTextSecondary());
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
     * 记录成功日志
     */
    private void logSuccess(String message) {
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat(HH_MM_SS);
                String timestamp = sdf.format(new Date());
                logArea.append("[" + timestamp + "] SUCCESS: " + message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    /**
     * 记录Git操作结果到日志区域
     */
    public void logGitOperationResult(GitOperationResult result) {
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat(HH_MM_SS);
                String timestamp = sdf.format(new Date());

                // 操作标题
                logArea.append("[" + timestamp + "] " + result.message + "\n");

                // 详细信息
                if (!result.details.isEmpty()) {
                    logArea.append(result.details);
                }

                // 操作状态
                if (result.success) {
                    logArea.append("[" + timestamp + "] " + result.operationType + " ✅ \n");
                } else {
                    logArea.append("[" + timestamp + "] " + result.operationType + " ❌ \n");
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
