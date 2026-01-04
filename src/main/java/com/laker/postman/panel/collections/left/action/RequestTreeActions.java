package com.laker.postman.panel.collections.left.action;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.*;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.left.dialog.AddRequestDialog;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.sidebar.SidebarTabPanel;
import com.laker.postman.service.collections.RequestsPersistence;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.postman.PostmanCollectionExporter;
import com.laker.postman.service.workspace.WorkspaceTransferHelper;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.*;

/**
 * 请求树的操作类
 * 集中管理所有的树操作逻辑
 */
@Slf4j
public class RequestTreeActions {
    private final JTree requestTree;
    private final RequestCollectionsLeftPanel leftPanel;
    private final List<HttpRequestItem> copiedRequests = new ArrayList<>();

    public RequestTreeActions(JTree requestTree, RequestCollectionsLeftPanel leftPanel) {
        this.requestTree = requestTree;
        this.leftPanel = leftPanel;
    }

    /**
     * 显示添加分组对话框
     */
    public void showAddGroupDialog(DefaultMutableTreeNode parentNode) {
        if (parentNode == null) return;

        String groupName = JOptionPane.showInputDialog(
                SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_GROUP_PROMPT)
        );

        if (groupName != null && !groupName.trim().isEmpty()) {
            addGroupToNode(parentNode, groupName);
        }
    }

    /**
     * 在指定节点下添加分组
     */
    private void addGroupToNode(DefaultMutableTreeNode parentNode, String groupName) {
        if (parentNode == null) return;
        RequestGroup group = new RequestGroup(groupName);
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new Object[]{GROUP, group});

        int insertIdx = getGroupInsertIndex(parentNode);
        if (insertIdx >= 0 && insertIdx <= parentNode.getChildCount()) {
            parentNode.insert(groupNode, insertIdx);
        } else {
            parentNode.add(groupNode);
        }

        leftPanel.getTreeModel().reload(parentNode);
        requestTree.expandPath(new TreePath(parentNode.getPath()));
        leftPanel.getPersistence().saveRequestGroups();
    }

    /**
     * 在选中节点下添加分组
     */
    public void addGroupUnderSelected() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;
        showAddGroupDialog(selectedNode);
    }

    /**
     * 显示添加请求对话框
     */
    public void showAddRequestDialog(DefaultMutableTreeNode groupNode) {
        AddRequestDialog dialog = new AddRequestDialog(groupNode, leftPanel);
        dialog.show();
    }

    /**
     * 重命名选中的项（分组、请求或保存的响应）
     */
    public void renameSelectedItem() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;

        Object userObj = selectedNode.getUserObject();
        if (!(userObj instanceof Object[] obj)) return;

        if (GROUP.equals(obj[0])) {
            renameGroup(selectedNode, obj);
        } else if (REQUEST.equals(obj[0])) {
            renameRequest(selectedNode, obj);
        } else if (SAVED_RESPONSE.equals(obj[0])) {
            renameSavedResponse(selectedNode, obj);
        }
    }

    /**
     * 重命名分组
     */
    private void renameGroup(DefaultMutableTreeNode selectedNode, Object[] obj) {
        Object groupData = obj[1];
        String oldName = groupData instanceof RequestGroup
                ? ((RequestGroup) groupData).getName()
                : String.valueOf(groupData);

        Object result = JOptionPane.showInputDialog(
                SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_GROUP_PROMPT),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_GROUP_TITLE),
                JOptionPane.PLAIN_MESSAGE,
                null, null, oldName
        );

        if (result != null) {
            String newName = result.toString().trim();
            if (!newName.isEmpty() && !newName.equals(oldName)) {
                updateGroupName(selectedNode, obj, groupData, newName);
            } else if (newName.isEmpty()) {
                NotificationUtil.showWarning(
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_GROUP_EMPTY)
                );
            }
        }
    }

    /**
     * 更新分组名称
     */
    private void updateGroupName(DefaultMutableTreeNode node, Object[] obj, Object groupData, String newName) {
        if (groupData instanceof RequestGroup group) {
            group.setName(newName);
        } else {
            obj[1] = new RequestGroup(newName);
        }
        leftPanel.getTreeModel().nodeChanged(node);
        leftPanel.getPersistence().saveRequestGroups();
    }

    /**
     * 重命名请求
     */
    private void renameRequest(DefaultMutableTreeNode selectedNode, Object[] obj) {
        HttpRequestItem item = (HttpRequestItem) obj[1];
        String oldName = item.getName();

        Object result = JOptionPane.showInputDialog(
                SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_REQUEST_PROMPT),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_REQUEST_TITLE),
                JOptionPane.PLAIN_MESSAGE,
                null, null, oldName
        );

        if (result != null) {
            String newName = result.toString().trim();
            if (!newName.isEmpty() && !newName.equals(oldName)) {
                updateRequestName(selectedNode, item, newName);
            } else if (newName.isEmpty()) {
                NotificationUtil.showWarning(
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_REQUEST_EMPTY)
                );
            }
        }
    }

    /**
     * 更新请求名称
     */
    private void updateRequestName(DefaultMutableTreeNode node, HttpRequestItem item, String newName) {
        item.setName(newName);
        leftPanel.getTreeModel().nodeChanged(node);
        leftPanel.getPersistence().saveRequestGroups();

        // 同步更新已打开Tab的标题
        updateOpenedTabsTitle(item, newName);
    }

    /**
     * 更新已打开Tab的标题
     */
    private void updateOpenedTabsTitle(HttpRequestItem item, String newName) {
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                HttpRequestItem tabItem = subPanel.getCurrentRequest();
                if (tabItem != null && item.getId().equals(tabItem.getId())) {
                    tabbedPane.setTitleAt(i, newName);
                    tabbedPane.setTabComponentAt(i,
                            new ClosableTabComponent(newName, item.getProtocol()));
                    subPanel.initPanelData(item);
                }
            }
        }
    }

    /**
     * 重命名保存的响应
     */
    private void renameSavedResponse(DefaultMutableTreeNode selectedNode, Object[] obj) {
        SavedResponse savedResponse = (SavedResponse) obj[1];
        String oldName = savedResponse.getName();

        Object result = JOptionPane.showInputDialog(
                SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_SAVED_RESPONSE_PROMPT),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_SAVED_RESPONSE_TITLE),
                JOptionPane.PLAIN_MESSAGE,
                null, null, oldName
        );

        if (result != null) {
            String newName = result.toString().trim();
            if (!newName.isEmpty() && !newName.equals(oldName)) {
                updateSavedResponseName(selectedNode, savedResponse, newName);
            } else if (newName.isEmpty()) {
                NotificationUtil.showWarning(
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_SAVED_RESPONSE_EMPTY)
                );
            }
        }
    }

    /**
     * 更新保存的响应名称
     */
    private void updateSavedResponseName(DefaultMutableTreeNode node, SavedResponse savedResponse, String newName) {
        savedResponse.setName(newName);
        leftPanel.getTreeModel().nodeChanged(node);
        leftPanel.getPersistence().saveRequestGroups();

        // 同步更新已打开Tab的标题
        updateOpenedSavedResponseTabsTitle(savedResponse, newName);
    }

    /**
     * 更新已打开的保存响应Tab的标题
     */
    private void updateOpenedSavedResponseTabsTitle(SavedResponse savedResponse, String newName) {
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                if (subPanel.isSavedResponseTab()) {
                    SavedResponse tabSavedResponse = subPanel.getSavedResponse();
                    if (tabSavedResponse != null && savedResponse.getId().equals(tabSavedResponse.getId())) {
                        tabbedPane.setTitleAt(i, newName);
                        tabbedPane.setTabComponentAt(i,
                                new ClosableTabComponent(newName, RequestItemProtocolEnum.SAVED_RESPONSE));
                    }
                }
            }
        }
    }

    /**
     * 删除选中的项（支持多选）
     */
    public void deleteSelectedItem() {
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length == 0) return;

        List<DefaultMutableTreeNode> nodesToDelete = filterDeletableNodes(selectedPaths);
        if (nodesToDelete.isEmpty()) return;

        if (!confirmDelete(nodesToDelete.size())) return;

        performDelete(nodesToDelete);
    }

    /**
     * 过滤可删除的节点
     */
    private List<DefaultMutableTreeNode> filterDeletableNodes(TreePath[] paths) {
        List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node != null && node != leftPanel.getRootTreeNode() && node.getParent() != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * 确认删除
     */
    private boolean confirmDelete(int count) {
        String message = count == 1
                ? I18nUtil.getMessage(MessageKeys.COLLECTIONS_DELETE_CONFIRM)
                : I18nUtil.getMessage(MessageKeys.COLLECTIONS_DELETE_BATCH_CONFIRM, count);

        int confirm = JOptionPane.showConfirmDialog(
                SingletonFactory.getInstance(MainFrame.class),
                message,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION
        );

        return confirm == JOptionPane.YES_OPTION;
    }

    /**
     * 执行删除操作
     */
    private void performDelete(List<DefaultMutableTreeNode> nodesToDelete) {
        List<TreePath> expandedPaths = TreeStateHelper.saveExpandedPaths(requestTree);

        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();

        // 关闭相关Tab
        for (DefaultMutableTreeNode node : nodesToDelete) {
            closeTabsForNode(node, tabbedPane);
        }

        // 删除节点
        for (DefaultMutableTreeNode node : nodesToDelete) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            if (parent != null) {
                parent.remove(node);
            }
        }

        leftPanel.getTreeModel().reload();
        TreeStateHelper.restoreExpandedPaths(requestTree, expandedPaths, leftPanel.getRootTreeNode());

        // 调整Tab选中状态
        if (tabbedPane.getTabCount() > 1) {
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
        }

        leftPanel.getPersistence().saveRequestGroups();
    }

    /**
     * 关闭节点相关的Tab
     */
    private void closeTabsForNode(DefaultMutableTreeNode node, JTabbedPane tabbedPane) {
        Object userObj = node.getUserObject();
        if (!(userObj instanceof Object[] obj)) return;

        if (REQUEST.equals(obj[0])) {
            HttpRequestItem item = (HttpRequestItem) obj[1];
            // 清空保存的响应列表，确保删除时不会保留已保存的响应
            if (item.getResponse() != null) {
                item.getResponse().clear();
            }
            closeRequestTabs(item, tabbedPane);
        } else if (GROUP.equals(obj[0])) {
            closeTabsForGroup(node, tabbedPane);
        } else if (SAVED_RESPONSE.equals(obj[0])) {
            // 处理删除保存的响应节点
            deleteSavedResponseNode(node);
        }
    }

    /**
     * 关闭请求的Tab
     */
    private void closeRequestTabs(HttpRequestItem item, JTabbedPane tabbedPane) {
        for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                HttpRequestItem tabItem = subPanel.getCurrentRequest();
                if (tabItem != null && item.getId().equals(tabItem.getId())) {
                    tabbedPane.remove(i);
                }
            }
        }
    }

    /**
     * 递归关闭分组下所有请求的Tab
     */
    private void closeTabsForGroup(DefaultMutableTreeNode groupNode, JTabbedPane tabbedPane) {
        for (int i = 0; i < groupNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) groupNode.getChildAt(i);
            closeTabsForNode(child, tabbedPane);
        }
    }

    /**
     * 复制选中的请求（创建副本）
     */
    public void duplicateSelectedRequests() {
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length == 0) return;

        List<RequestCopyInfo> copyInfos = collectCopyInfos(selectedPaths);
        if (copyInfos.isEmpty()) return;

        for (RequestCopyInfo info : copyInfos) {
            createDuplicateRequest(info);
        }

        leftPanel.getPersistence().saveRequestGroups();
        NotificationUtil.showSuccess(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_COPY_SUCCESS, copyInfos.size())
        );
    }

    /**
     * 收集复制信息
     */
    private List<RequestCopyInfo> collectCopyInfos(TreePath[] paths) {
        List<RequestCopyInfo> infos = new ArrayList<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObj = node.getUserObject();
            if (userObj instanceof Object[] obj && REQUEST.equals(obj[0])) {
                HttpRequestItem item = (HttpRequestItem) obj[1];
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                int idx = parent.getIndex(node);
                infos.add(new RequestCopyInfo(item, parent, idx));
            }
        }
        return infos;
    }

    /**
     * 创建复制的请求
     */
    private void createDuplicateRequest(RequestCopyInfo info) {
        HttpRequestItem copy = JsonUtil.deepCopy(info.item, HttpRequestItem.class);
        copy.setId(java.util.UUID.randomUUID().toString());
        copy.setName(info.item.getName() + " " +
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_SUFFIX));

        DefaultMutableTreeNode copyNode = new DefaultMutableTreeNode(new Object[]{REQUEST, copy});
        info.parent.insert(copyNode, info.index + 1);
        leftPanel.getTreeModel().reload(info.parent);
        requestTree.expandPath(new TreePath(info.parent.getPath()));
    }

    /**
     * 复制选中的请求到剪贴板
     */
    public void copySelectedRequests() {
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length == 0) return;

        copiedRequests.clear();
        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObj = node.getUserObject();
            if (userObj instanceof Object[] obj && REQUEST.equals(obj[0])) {
                HttpRequestItem item = (HttpRequestItem) obj[1];
                HttpRequestItem copy = JsonUtil.deepCopy(item, HttpRequestItem.class);
                copiedRequests.add(copy);
            }
        }

        if (!copiedRequests.isEmpty()) {
            NotificationUtil.showSuccess(
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_COPIED_TO_CLIPBOARD, copiedRequests.size())
            );
        }
    }

    /**
     * 粘贴请求
     */
    public void pasteRequests() {
        if (copiedRequests.isEmpty()) return;

        DefaultMutableTreeNode targetParent = determineTargetParent();
        if (targetParent == null) return;

        for (HttpRequestItem copiedItem : copiedRequests) {
            createPastedRequest(targetParent, copiedItem);
        }

        leftPanel.getTreeModel().reload(targetParent);
        requestTree.expandPath(new TreePath(targetParent.getPath()));
        leftPanel.getPersistence().saveRequestGroups();

        NotificationUtil.showSuccess(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_PASTE_SUCCESS, copiedRequests.size())
        );
    }

    /**
     * 确定粘贴的目标父节点
     */
    private DefaultMutableTreeNode determineTargetParent() {
        DefaultMutableTreeNode targetNode = getSelectedNode();
        if (targetNode == null) {
            return leftPanel.getRootTreeNode();
        }

        Object userObj = targetNode.getUserObject();
        if (targetNode == leftPanel.getRootTreeNode() || ROOT.equals(userObj)) {
            return leftPanel.getRootTreeNode();
        }

        if (userObj instanceof Object[] obj) {
            if (GROUP.equals(obj[0])) {
                return targetNode;
            } else if (REQUEST.equals(obj[0])) {
                return (DefaultMutableTreeNode) targetNode.getParent();
            }
        }

        return leftPanel.getRootTreeNode();
    }

    /**
     * 创建粘贴的请求
     */
    private void createPastedRequest(DefaultMutableTreeNode parent, HttpRequestItem copiedItem) {
        HttpRequestItem pasteItem = JsonUtil.deepCopy(copiedItem, HttpRequestItem.class);
        pasteItem.setId(java.util.UUID.randomUUID().toString());
        pasteItem.setName(copiedItem.getName() + " " +
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_SUFFIX));

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new Object[]{REQUEST, pasteItem});
        parent.add(newNode);
    }

    /**
     * 复制请求为cURL命令
     */
    public void copySelectedRequestAsCurl() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;

        Object userObj = selectedNode.getUserObject();
        if (!(userObj instanceof Object[] obj) || !REQUEST.equals(obj[0])) return;

        HttpRequestItem item = (HttpRequestItem) obj[1];
        try {
            PreparedRequest req = PreparedRequestBuilder.build(item);
            PreparedRequestBuilder.replaceVariablesAfterPreScript(req);
            String curl = CurlParser.toCurl(req);

            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(curl), null);

            NotificationUtil.showSuccess(
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_CURL_SUCCESS)
            );
        } catch (Exception ex) {
            NotificationUtil.showError(
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_CURL_FAIL, ex.getMessage())
            );
        }
    }

    /**
     * 复制选中的分组
     */
    public void duplicateSelectedGroup() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;

        Object userObj = selectedNode.getUserObject();
        if (!(userObj instanceof Object[] obj) || !GROUP.equals(obj[0])) return;

        DefaultMutableTreeNode copyNode = TreeNodeCloner.deepCopyGroupNode(selectedNode);
        Object[] copyObj = (Object[]) copyNode.getUserObject();

        // 正确处理 RequestGroup 对象的名称
        String originalName = getGroupName(copyObj[1]);
        String newName = originalName + I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_SUFFIX);

        if (copyObj[1] instanceof RequestGroup requestGroup) {
            requestGroup.setName(newName);
        } else {
            copyObj[1] = newName;
        }

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
        if (parent != null) {
            int idx = parent.getIndex(selectedNode) + 1;
            parent.insert(copyNode, idx);
            leftPanel.getTreeModel().reload(parent);
            requestTree.expandPath(new TreePath(parent.getPath()));
            leftPanel.getPersistence().saveRequestGroups();
        }
    }

    /**
     * 导出分组为Postman Collection
     */
    public void exportGroupAsPostman(DefaultMutableTreeNode groupNode) {
        if (!isValidGroupNode(groupNode)) {
            showInvalidGroupWarning();
            return;
        }

        Object[] obj = (Object[]) groupNode.getUserObject();
        String groupName = getGroupName(obj[1]);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_EXPORT_POSTMAN_DIALOG_TITLE)
        );
        fileChooser.setSelectedFile(new File(groupName + "-postman.json"));

        int userSelection = fileChooser.showSaveDialog(SingletonFactory.getInstance(MainFrame.class));
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            exportToFile(groupNode, groupName, fileChooser.getSelectedFile());
        }
    }

    /**
     * 导出到文件
     */
    private void exportToFile(DefaultMutableTreeNode groupNode, String groupName, File file) {
        try {
            JSONObject postmanCollection =
                    PostmanCollectionExporter.buildPostmanCollectionFromTreeNode(groupNode, groupName);
            FileUtil.writeUtf8String(postmanCollection.toStringPretty(), file);
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_SUCCESS));
        } catch (Exception ex) {
            log.error("Export Postman error", ex);
            NotificationUtil.showError(
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_FAIL, ex.getMessage())
            );
        }
    }

    /**
     * 转移集合到其他工作区
     */
    public void moveCollectionToWorkspace(DefaultMutableTreeNode selectedNode) {
        if (!isValidGroupNode(selectedNode)) return;

        Object[] obj = (Object[]) selectedNode.getUserObject();
        String collectionName = getGroupName(obj[1]);

        WorkspaceTransferHelper.transferToWorkspaceQuiet(
                collectionName,
                (targetWorkspace, itemName) -> performCollectionMove(selectedNode, targetWorkspace)
        );
    }

    /**
     * 执行集合移动到目标工作区
     * 将集合从当前工作区移动到目标工作区
     *
     * @param collectionNode  要移动的集合节点
     * @param targetWorkspace 目标工作区
     */
    private void performCollectionMove(DefaultMutableTreeNode collectionNode, Workspace targetWorkspace) {
        // 1. 深拷贝集合节点（包含所有子节点）
        DefaultMutableTreeNode copiedNode = TreeNodeCloner.deepCopyGroupNode(collectionNode);

        // 2. 获取目标工作区的集合文件路径
        String targetCollectionPath = SystemUtil.getCollectionPathForWorkspace(targetWorkspace);

        // 3. 创建目标工作区的持久化工具
        DefaultMutableTreeNode targetRootNode = new DefaultMutableTreeNode(ROOT);
        DefaultTreeModel targetTreeModel = new DefaultTreeModel(targetRootNode);
        RequestsPersistence targetPersistence = new RequestsPersistence(
                targetCollectionPath, targetRootNode, targetTreeModel);

        // 4. 加载目标工作区的现有集合
        targetPersistence.initRequestGroupsFromFile();

        // 5. 将集合添加到目标工作区
        targetRootNode.add(copiedNode);

        // 6. 保存到目标工作区
        targetPersistence.saveRequestGroups();

        // 7. 从当前工作区删除原集合
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) collectionNode.getParent();
        if (parent != null) {
            parent.remove(collectionNode);
            leftPanel.getTreeModel().reload();
            leftPanel.getPersistence().saveRequestGroups();
        }

        log.info("Successfully moved collection '{}' to workspace '{}'",
                ((Object[]) collectionNode.getUserObject())[1], targetWorkspace.getName());
    }

    /**
     * 添加选中的请求到功能测试
     */
    public void addSelectedRequestsToFunctionalTest() {
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length == 0) return;

        List<HttpRequestItem> requestsToAdd = new ArrayList<>();
        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            collectRequestsRecursively(node, requestsToAdd);
        }

        if (requestsToAdd.isEmpty()) return;

        try {
            FunctionalPanel functionalPanel = SingletonFactory.getInstance(FunctionalPanel.class);
            functionalPanel.loadRequests(requestsToAdd);

            // 切换到功能测试Tab
            SidebarTabPanel sidebarPanel = SingletonFactory.getInstance(SidebarTabPanel.class);
            JTabbedPane tabbedPane = sidebarPanel.getTabbedPane();
            tabbedPane.setSelectedIndex(3); // 功能测试Tab索引
        } catch (Exception e) {
            log.error("添加请求到功能测试失败", e);
        }
    }

    // ==================== 辅助方法 ====================

    private DefaultMutableTreeNode getSelectedNode() {
        return (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
    }

    private int getGroupInsertIndex(DefaultMutableTreeNode parent) {
        if (parent == null) return -1;
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof Object[] obj && REQUEST.equals(obj[0])) {
                return i;
            }
        }
        return parent.getChildCount();
    }

    private boolean isValidGroupNode(DefaultMutableTreeNode node) {
        return node != null
                && node.getUserObject() instanceof Object[] obj
                && GROUP.equals(obj[0]);
    }

    private void showInvalidGroupWarning() {
        JOptionPane.showMessageDialog(
                leftPanel,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_EXPORT_POSTMAN_SELECT_GROUP),
                I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                JOptionPane.WARNING_MESSAGE
        );
    }

    private String getGroupName(Object groupData) {
        return groupData instanceof RequestGroup requestGroup
                ? requestGroup.getName()
                : String.valueOf(groupData);
    }

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

    public boolean isCopiedRequestsEmpty() {
        return copiedRequests.isEmpty();
    }

    /**
     * 从父请求中删除保存的响应
     */
    private void deleteSavedResponseNode(DefaultMutableTreeNode savedResponseNode) {
        // 获取父节点（应该是 REQUEST 节点）
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) savedResponseNode.getParent();
        if (parentNode == null) return;

        Object parentUserObj = parentNode.getUserObject();
        if (!(parentUserObj instanceof Object[] parentObj)) return;
        if (!REQUEST.equals(parentObj[0])) return;

        // 获取父请求和要删除的响应
        HttpRequestItem parentRequest = (HttpRequestItem) parentObj[1];
        Object savedRespUserObj = savedResponseNode.getUserObject();
        if (!(savedRespUserObj instanceof Object[] respObj)) return;
        if (!SAVED_RESPONSE.equals(respObj[0])) return;

        SavedResponse toDelete = (SavedResponse) respObj[1];

        // 从父请求的 response 列表中删除
        if (parentRequest.getResponse() != null) {
            parentRequest.getResponse().removeIf(resp ->
                    resp.getId() != null && resp.getId().equals(toDelete.getId())
            );
        }
    }

    /**
     * 请求复制信息内部类
     */
    private static class RequestCopyInfo {
        final HttpRequestItem item;
        final DefaultMutableTreeNode parent;
        final int index;

        RequestCopyInfo(HttpRequestItem item, DefaultMutableTreeNode parent, int index) {
            this.item = item;
            this.parent = parent;
            this.index = index;
        }
    }
}
