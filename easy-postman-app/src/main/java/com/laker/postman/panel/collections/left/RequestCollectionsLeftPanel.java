package com.laker.postman.panel.collections.left;

import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.async.EasyTaskExecutor;
import com.laker.postman.common.component.tree.RequestTreeCellRenderer;
import com.laker.postman.common.component.tree.TreeTransferHandler;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.collections.left.action.TreeNodeCloner;
import com.laker.postman.panel.collections.left.handler.RequestTreeKeyboardHandler;
import com.laker.postman.panel.collections.left.handler.RequestTreeMouseHandler;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.service.collections.OpenedRequestsService;
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.service.collections.RequestsPersistence;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.startup.StartupDiagnostics;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 请求集合面板，展示所有请求分组和请求项
 * 支持请求的增删改查、分组管理、拖拽排序等功能
 */
@Slf4j
public class RequestCollectionsLeftPanel extends SingletonBasePanel {
    public static final String REQUEST = "request";
    public static final String GROUP = "group";
    public static final String ROOT = "root";
    public static final String SAVED_RESPONSE = "response";
    public static final String EXPORT_FILE_NAME = "EasyPostman-Collections.json";
    // 请求集合的根节点
    @Getter
    private DefaultMutableTreeNode rootTreeNode;
    // 请求树组件
    @Getter
    private JTree requestTree;
    // 树模型，用于管理树节点
    @Getter
    private DefaultTreeModel treeModel;
    @Getter
    private transient RequestsPersistence persistence;

    private record StartupLoadSnapshot(List<HttpRequestItem> openedRequests, HttpRequestItem lastNonNewRequest) {
    }


    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 200));

        // 顶部面板，导入导出按钮在最上方，环境信息在下方
        JPanel topPanel = getTopPanel();
        add(topPanel, BorderLayout.NORTH);

        JScrollPane treeScrollPane = getTreeScrollPane();
        add(treeScrollPane, BorderLayout.CENTER);
    }

    private JScrollPane getTreeScrollPane() {
        // 初始化请求树
        rootTreeNode = new DefaultMutableTreeNode(ROOT);
        treeModel = new DefaultTreeModel(rootTreeNode);
        Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
        String filePath = SystemUtil.getCollectionPathForWorkspace(currentWorkspace);
        // 初始化持久化工具
        persistence = new RequestsPersistence(filePath, rootTreeNode, treeModel);
        // 创建树组件，重写 getScrollableTracksViewportWidth 确保树宽度始终铺满 viewport，
        // 这样鼠标在行的右侧空白区域仍在 JTree 上，mouseMoved 事件能正常触发
        requestTree = new JTree(treeModel) {
            @Override
            public boolean isPathEditable(TreePath path) {
                Object node = path.getLastPathComponent();
                if (node instanceof DefaultMutableTreeNode treeNode) {
                    return treeNode.getParent() != null;
                }
                return false;
            }

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                // 在树正常绘制之后，对 hover 行叠加整行背景高亮
                if (getCellRenderer() instanceof RequestTreeCellRenderer r) {
                    int row = r.getHoveredRow();
                    if (row >= 0 && row < getRowCount()) {
                        Rectangle bounds = getRowBounds(row);
                        if (bounds != null) {
                            TreePath path = getPathForRow(row);
                            if (path != null && !isPathSelected(path)) {
                                Graphics2D g2 = (Graphics2D) g.create();
                                boolean dark = com.formdev.flatlaf.FlatLaf.isLafDark();
                                g2.setColor(dark ? new Color(255, 255, 255, 20) : new Color(0, 0, 0, 18));
                                g2.fillRect(0, bounds.y, getWidth(), bounds.height);
                                g2.dispose();
                            }
                        }
                    }
                }
            }
        };
        // 不显示根节点
        requestTree.setRootVisible(false);
        // 让 JTree 组件显示根节点的"展开/收起"小三角（即树形结构的手柄）
        requestTree.setShowsRootHandles(true);
        // 禁用双击展开/收起：设置 toggleClickCount 为 0
        // 这样双击只会触发我们自定义的打开 tab 行为，不会展开/收起
        requestTree.setToggleClickCount(0);
        // 设置树支持多选（支持批量删除）
        requestTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        // FlatLaf wideCellRenderer：让 CellRenderer 宽度铺满整行，使 "+" 号精确贴在右侧
        requestTree.putClientProperty("FlatLaf.style", "wideCellRenderer: true");
        // 注册 ToolTipManager，允许动态设置 tooltip（hover "+" 时显示 Add Request）
        ToolTipManager.sharedInstance().registerComponent(requestTree);
        // 设置树的字体和行高
        requestTree.setCellRenderer(new RequestTreeCellRenderer());
        requestTree.setRowHeight(28);
        JScrollPane treeScrollPane = new JScrollPane(requestTree);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(16); // 设置滚动条增量
        // 启用拖拽排序
        requestTree.setDragEnabled(true);
        requestTree.setDropMode(DropMode.ON_OR_INSERT);
        requestTree.setTransferHandler(new TreeTransferHandler(requestTree, treeModel, this::saveRequestGroups));
        return treeScrollPane;
    }

    private JPanel getTopPanel() {
        return SingletonFactory.getInstance(LeftTopPanel.class);
    }


    @Override
    protected void registerListeners() {
        requestTree.addKeyListener(new RequestTreeKeyboardHandler(requestTree, this));
        RequestTreeMouseHandler mouseHandler = new RequestTreeMouseHandler(requestTree, this);
        requestTree.addMouseListener(mouseHandler);
        requestTree.addMouseMotionListener(mouseHandler);

        EasyTaskExecutor.execute(
                this::loadStartupSnapshot,
                this::applyStartupSnapshot,
                this::handleStartupLoadError,
                "RequestCollections-Loader"
        );
    }

    private StartupLoadSnapshot loadStartupSnapshot() {
        long loadStartNanos = System.nanoTime();
        StartupDiagnostics.mark("Request collections load started");
        persistence.initRequestGroupsFromFile();
        List<HttpRequestItem> openedRequests = OpenedRequestsService.getAll();
        HttpRequestItem lastNonNewRequest = RequestCollectionsService.getLastNonNewRequest(openedRequests);
        StartupDiagnostics.mark("Request collections load finished in "
                + StartupDiagnostics.formatSince(loadStartNanos)
                + ", openedRequests=" + openedRequests.size());
        return new StartupLoadSnapshot(openedRequests, lastNonNewRequest);
    }

    private void applyStartupSnapshot(StartupLoadSnapshot snapshot) {
        RequestEditPanel requestEditPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        StartupDiagnostics.mark("Collections UI ready; openedRequests=" + snapshot.openedRequests().size());
        requestEditPanel.setAutoInitializeSelectedTabOnTabAdd(true);
        restoreOpenedRequestTabs(snapshot, requestEditPanel);
        restoreTreeSelection(snapshot);
    }

    private void restoreOpenedRequestTabs(StartupLoadSnapshot snapshot, RequestEditPanel requestEditPanel) {
        if (snapshot.openedRequests().isEmpty()) {
            requestEditPanel.addPlusTab();
            return;
        }
        StartupDiagnostics.mark("Restoring opened request tabs");
        requestEditPanel.setStartupRestoreSelectingLastTab(true);
        requestEditPanel.setAutoInitializeSelectedTabOnTabAdd(false);
        requestEditPanel.addPlusTab();
        RequestCollectionsService.restoreOpenedRequests(
                snapshot.openedRequests(),
                () -> {
                    requestEditPanel.setStartupRestoreSelectingLastTab(false);
                    requestEditPanel.setAutoInitializeSelectedTabOnTabAdd(true);
                    requestEditPanel.initializeSelectedStartupRestoreTab();
                    requestEditPanel.warmUpDeferredRequestTabsAfterStartup();
                }
        );
    }

    private void restoreTreeSelection(StartupLoadSnapshot snapshot) {
        if (snapshot.lastNonNewRequest() != null) {
            locateAndSelectRequest(snapshot.lastNonNewRequest().getId());
            return;
        }
        expandAndSelectFirstGroupIfPresent();
    }

    private void expandAndSelectFirstGroupIfPresent() {
        if (rootTreeNode.getChildCount() <= 0) {
            return;
        }
        DefaultMutableTreeNode firstGroup = (DefaultMutableTreeNode) rootTreeNode.getChildAt(0);
        TreePath path = new TreePath(firstGroup.getPath());
        requestTree.setSelectionPath(path);
        requestTree.expandPath(path);
    }

    private void handleStartupLoadError(Throwable error) {
        log.error("Error loading request collections", error);
        SingletonFactory.getInstance(RequestEditPanel.class).addPlusTab();
    }


    private void saveRequestGroups() {
        persistence.saveRequestGroups();
        // 保存后使预计算缓存失效（可能有修改）
        PreparedRequestBuilder.invalidateCache();
    }


    /**
     * 将请求保存到指定分组
     *
     * @param group 分组信息，[type, name/RequestGroup] 形式的数组
     * @param item  请求项
     */
    public void saveRequestToGroup(Object[] group, HttpRequestItem item) {
        if (group == null || !GROUP.equals(group[0])) {
            return;
        }
        Object groupData = group[1];
        String groupName = groupData instanceof RequestGroup ? ((RequestGroup) groupData).getName() : String.valueOf(groupData);
        DefaultMutableTreeNode groupNode = findGroupNode(rootTreeNode, groupName);
        if (groupNode == null) {
            return;
        }
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new Object[]{REQUEST, item});
        groupNode.add(requestNode);
        treeModel.reload(groupNode);
        requestTree.expandPath(new TreePath(groupNode.getPath()));
        persistence.saveRequestGroups();
    }

    /**
     * 根据名称查找分组节点
     */
    public DefaultMutableTreeNode findGroupNode(DefaultMutableTreeNode node, String groupName) {
        if (node == null) return null;

        Object userObj = node.getUserObject();
        if (userObj instanceof Object[] obj && GROUP.equals(obj[0])) {
            Object groupData = obj[1];
            String nodeName = groupData instanceof RequestGroup ? ((RequestGroup) groupData).getName() : String.valueOf(groupData);
            if (groupName.equals(nodeName)) {
                return node;
            }
        }


        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode result = findGroupNode(child, groupName);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * 更新已存在的请求
     *
     * @param item 请求项
     * @return 是否更新成功
     */
    public boolean updateExistingRequest(HttpRequestItem item) {
        if (item == null || item.getId() == null || item.getId().isEmpty()) {
            return false;
        }
        DefaultMutableTreeNode requestNode = RequestCollectionsService.findRequestNodeById(rootTreeNode, item.getId());
        if (requestNode == null) {
            return false;
        }
        Object[] userObj = (Object[]) requestNode.getUserObject();
        HttpRequestItem originalItem = (HttpRequestItem) userObj[1];
        String originalName = originalItem.getName();
        item.setName(originalName);

        // 保留原对象的 response，避免在保存请求时丢失已保存的响应
        if (originalItem.getResponse() != null && !originalItem.getResponse().isEmpty()) {
            item.setResponse(originalItem.getResponse());
        }

        userObj[1] = item;
        treeModel.nodeChanged(requestNode);

        PreparedRequestBuilder.invalidateCacheForRequest(item.getId());

        persistence.saveRequestGroups();
        // 保存后去除Tab红点，同时通知 FunctionalPanel 和 PerformancePanel 同步最新数据
        SwingUtilities.invokeLater(() -> {
            // 1. 去除 Collections Tab 红点
            RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
            JTabbedPane tabbedPane = editPanel.getTabbedPane();
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component comp = tabbedPane.getComponentAt(i);
                if (comp instanceof RequestEditSubPanel subPanel) {
                    HttpRequestItem tabItem = subPanel.getCurrentRequest();
                    if (tabItem != null && item.getId().equals(tabItem.getId())) {
                        editPanel.updateTabDirty(subPanel, false);
                        subPanel.setOriginalRequestItem(item);
                    }
                }
            }
            // 2. 同步 FunctionalPanel
            SingletonFactory.getInstance(com.laker.postman.panel.functional.FunctionalPanel.class)
                    .syncRequestItem(item);
            // 3. 同步 PerformancePanel
            SingletonFactory.getInstance(com.laker.postman.panel.performance.PerformancePanel.class)
                    .syncRequestItem(item);
        });
        return true;
    }


    /**
     * 获取分组树的 TreeModel（用于分组选择树）
     */
    public DefaultTreeModel getGroupTreeModel() {
        return treeModel;
    }


    // 创建一个可多选的请求/分组选择树（用于Runner面板弹窗）
    public JTree createRequestSelectionTree() {
        DefaultTreeModel model = new DefaultTreeModel(TreeNodeCloner.cloneTreeNode(rootTreeNode));
        JTree tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new RequestTreeCellRenderer());
        tree.setRowHeight(28);
        // 支持多选
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        return tree;
    }

    // 获取树中选中的所有请求（包含分组下所有请求）
    public List<HttpRequestItem> getSelectedRequestsFromTree(JTree tree) {
        List<HttpRequestItem> result = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return result;
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            collectRequestsRecursively(node, result);
        }
        // 去重（按id）
        Map<String, HttpRequestItem> map = new LinkedHashMap<>();
        for (HttpRequestItem item : result) {
            map.put(item.getId(), item);
        }
        return new ArrayList<>(map.values());
    }

    // 递归收集请求
    private void collectRequestsRecursively(DefaultMutableTreeNode node, List<HttpRequestItem> list) {
        Object userObj = node.getUserObject();
        if (userObj instanceof Object[] obj) {
            if (REQUEST.equals(obj[0])) {
                list.add((HttpRequestItem) obj[1]);
            } else if (GROUP.equals(obj[0])) {
                for (int i = 0; i < node.getChildCount(); i++) {
                    collectRequestsRecursively((DefaultMutableTreeNode) node.getChildAt(i), list);
                }
            }
        }
    }

    /**
     * 根据请求ID定位并选中树中的对应节点
     *
     * @param requestId 请求ID
     */
    public void locateAndSelectRequest(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return;
        }

        DefaultMutableTreeNode targetNode = RequestCollectionsService.findRequestNodeById(rootTreeNode, requestId);
        if (targetNode == null) {
            return;
        }

        TreePath treePath = new TreePath(targetNode.getPath());
        requestTree.expandPath(treePath.getParentPath());
        requestTree.setSelectionPath(treePath);
        requestTree.requestFocusInWindow();
    }

    /**
     * 切换到指定工作区的请求集合文件，并在集合树加载完成后执行回调。
     */
    public void switchWorkspaceAndRefreshUI(String collectionFilePath, Runnable onSuccessCallback) {
        // 使用 AsyncTaskExecutor 异步切换工作区
        EasyTaskExecutor.builder()
                .threadName("SwitchWorkspace-Loader")
                .backgroundTask(() -> {
                    // 后台线程：执行文件加载操作
                    if (persistence != null) {
                        persistence.setDataFilePath(collectionFilePath);
                    }
                })
                .onSuccess(() -> {
                    // EDT线程：更新UI
                    SingletonFactory.getInstance(RequestEditPanel.class).getTabbedPane().removeAll();
                    SingletonFactory.getInstance(RequestEditPanel.class).addPlusTab();
                    expandFirstGroup();
                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }
                })
                .onError(error -> {
                    // EDT线程：处理错误
                    log.error("Error switching workspace and loading collections", error);
                    SingletonFactory.getInstance(RequestEditPanel.class).getTabbedPane().removeAll();
                    SingletonFactory.getInstance(RequestEditPanel.class).addPlusTab();
                })
                .execute();
    }

    /**
     * 展开第一个分组
     */
    public void expandFirstGroup() {
        if (rootTreeNode != null && rootTreeNode.getChildCount() > 0) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) rootTreeNode.getChildAt(0);
            TreePath path = new TreePath(firstChild.getPath());
            requestTree.expandPath(path);
        }
    }

}
