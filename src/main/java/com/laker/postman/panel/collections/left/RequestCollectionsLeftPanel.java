package com.laker.postman.panel.collections.left;

import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.async.AsyncTaskExecutor;
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
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.service.collections.RequestsPersistence;
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
        // 创建树组件
        requestTree = new JTree(treeModel) {
            @Override
            public boolean isPathEditable(TreePath path) {
                // 禁止根节点重命名
                Object node = path.getLastPathComponent();
                if (node instanceof DefaultMutableTreeNode treeNode) {
                    return treeNode.getParent() != null;
                }
                return false;
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
        // 设置树的字体和行高
        requestTree.setCellRenderer(new RequestTreeCellRenderer());
        requestTree.setRowHeight(28);
        JScrollPane treeScrollPane = new JScrollPane(requestTree);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(16); // 设置滚动条增量
        treeScrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        // 启用拖拽排序
        requestTree.setDragEnabled(true); // 启用拖拽
        requestTree.setDropMode(DropMode.ON_OR_INSERT); // 设置拖拽模式为插入
        requestTree.setTransferHandler(new TreeTransferHandler(requestTree, treeModel, this::saveRequestGroups));
        return treeScrollPane;
    }

    private JPanel getTopPanel() {
        return SingletonFactory.getInstance(LeftTopPanel.class);
    }


    @Override
    protected void registerListeners() {
        requestTree.addKeyListener(new RequestTreeKeyboardHandler(requestTree, this));
        requestTree.addMouseListener(new RequestTreeMouseHandler(requestTree, this));

        // 使用 AsyncTaskExecutor 异步加载请求集合
        AsyncTaskExecutor.execute(
                // 后台线程：执行耗时的IO操作
                () -> {
                    persistence.initRequestGroupsFromFile();
                    return RequestCollectionsService.getLastNonNewRequest();
                },
                // EDT线程：更新UI
                lastNonNewRequest -> {
                    // 恢复之前已打开请求
                    RequestCollectionsService.restoreOpenedRequests();
                    // 增加一个plusTab
                    SingletonFactory.getInstance(RequestEditPanel.class).addPlusTab();
                    // 反向定位到最后一个请求
                    if (lastNonNewRequest != null) {
                        locateAndSelectRequest(lastNonNewRequest.getId());
                    } else { // 没有请求时默认展开第一个组
                        if (rootTreeNode.getChildCount() > 0) {
                            DefaultMutableTreeNode firstGroup = (DefaultMutableTreeNode) rootTreeNode.getChildAt(0);
                            TreePath path = new TreePath(firstGroup.getPath());
                            requestTree.setSelectionPath(path);
                            requestTree.expandPath(path);
                        }
                    }
                },
                // EDT线程：处理错误
                error -> {
                    log.error("Error loading request collections", error);
                    // 即使加载失败也要添加 plusTab
                    SingletonFactory.getInstance(RequestEditPanel.class).addPlusTab();
                },
                "RequestCollections-Loader"
        );
    }


    private void saveRequestGroups() {
        persistence.saveRequestGroups();
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
        userObj[1] = item;
        treeModel.nodeChanged(requestNode);
        persistence.saveRequestGroups();
        // 保存后去除Tab红点
        SwingUtilities.invokeLater(() -> {
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
        tree.setBackground(new Color(245, 247, 250));
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
        if (targetNode == null) { // 如果没有找到对应的请求节点
            return;
        }

        // 构建完整路径
        TreePath treePath = new TreePath(targetNode.getPath());

        // 展开父节点路径，确保目标节点可见
        requestTree.expandPath(treePath.getParentPath());

        // 选中目标节点
        requestTree.setSelectionPath(treePath);

        // 确保焦点在树上（用于突出显示）
        requestTree.requestFocusInWindow();
    }

    /**
     * 切换到指定工作区的请求集合文件，并刷新树UI
     */
    public void switchWorkspaceAndRefreshUI(String collectionFilePath) {
        // 使用 AsyncTaskExecutor 异步切换工作区
        AsyncTaskExecutor.builder()
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
