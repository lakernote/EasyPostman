package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.model.PerformanceTreeNode;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
final class PerformanceRequestSyncSupport {

    private final DefaultTreeModel treeModel;
    private final JTree performanceTree;
    private final PerformanceCollectionRequestResolver requestResolver;
    private final BiConsumer<DefaultMutableTreeNode, PerformanceTreeNode> syncRequestStructureAction;

    void syncRequestItem(DefaultMutableTreeNode root,
                         HttpRequestItem item,
                         DefaultMutableTreeNode currentRequestNode,
                         Consumer<HttpRequestItem> switchRequestEditorAction) {
        if (item == null || item.getId() == null) {
            return;
        }
        syncRequestItemInTree(root, item, currentRequestNode, switchRequestEditorAction);
    }

    DefaultMutableTreeNode refreshRequestsFromCollections(DefaultMutableTreeNode currentRequestNode,
                                                          Consumer<HttpRequestItem> switchRequestEditorAction,
                                                          Runnable saveAllPropertyPanelDataAction,
                                                          Runnable clearCachedResultAction,
                                                          Runnable saveConfigAction) {
        saveAllPropertyPanelDataAction.run();

        clearCachedResultAction.run();
        // 这里只释放压测结果引用；不要主动 System.gc()，避免刷新集合时触发全局停顿。

        List<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        int updatedCount = refreshTreeNode(root, nodesToRemove);
        int removedCount = nodesToRemove.size();

        for (int i = nodesToRemove.size() - 1; i >= 0; i--) {
            DefaultMutableTreeNode nodeToRemove = nodesToRemove.get(i);
            if (nodeToRemove == currentRequestNode) {
                currentRequestNode = null;
            }
            treeModel.removeNodeFromParent(nodeToRemove);
        }

        HttpRequestItem refreshedCurrentItem = extractCurrentItemBeforeReload(currentRequestNode);
        TreePath currentPath = currentRequestNode != null ? new TreePath(currentRequestNode.getPath()) : null;
        saveConfigAction.run();

        treeModel.reload();

        for (int i = 0; i < performanceTree.getRowCount(); i++) {
            performanceTree.expandRow(i);
        }

        DefaultMutableTreeNode refreshedNode = relocateCurrentRequestNode(
                currentPath,
                currentRequestNode,
                refreshedCurrentItem,
                switchRequestEditorAction
        );

        if (removedCount > 0) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REFRESH_WARNING, removedCount));
        } else if (updatedCount > 0) {
            NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REFRESH_SUCCESS, updatedCount));
        } else {
            NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_TO_REFRESH));
        }
        return refreshedNode;
    }

    private void syncRequestItemInTree(DefaultMutableTreeNode node,
                                       HttpRequestItem item,
                                       DefaultMutableTreeNode currentRequestNode,
                                       Consumer<HttpRequestItem> switchRequestEditorAction) {
        Object userObj = node.getUserObject();
        if (userObj instanceof PerformanceTreeNode nodeData
                && nodeData.type == NodeType.REQUEST
                && nodeData.httpRequestItem != null
                && item.getId().equals(nodeData.httpRequestItem.getId())) {
            nodeData.httpRequestItem = item;
            syncRequestStructureAction.accept(node, nodeData);
            nodeData.name = item.getName();
            treeModel.nodeChanged(node);
            if (node == currentRequestNode) {
                switchRequestEditorAction.accept(item);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            syncRequestItemInTree((DefaultMutableTreeNode) node.getChildAt(i), item, currentRequestNode, switchRequestEditorAction);
        }
    }

    private HttpRequestItem extractCurrentItemBeforeReload(DefaultMutableTreeNode currentRequestNode) {
        if (currentRequestNode == null) {
            return null;
        }
        Object curUserObj = currentRequestNode.getUserObject();
        if (curUserObj instanceof PerformanceTreeNode curJmNode
                && curJmNode.type == NodeType.REQUEST
                && curJmNode.httpRequestItem != null) {
            return curJmNode.httpRequestItem;
        }
        return null;
    }

    private DefaultMutableTreeNode relocateCurrentRequestNode(TreePath currentPath,
                                                              DefaultMutableTreeNode currentRequestNode,
                                                              HttpRequestItem refreshedCurrentItem,
                                                              Consumer<HttpRequestItem> switchRequestEditorAction) {
        if (currentPath == null) {
            return currentRequestNode;
        }

        TreePath newPath = findTreePathByPath(currentPath);
        if (newPath == null) {
            return null;
        }

        performanceTree.setSelectionPath(newPath);
        DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) newPath.getLastPathComponent();
        Object userObj = newNode.getUserObject();
        if (!(userObj instanceof PerformanceTreeNode nodeData) || nodeData.type != NodeType.REQUEST) {
            return null;
        }

        HttpRequestItem itemToLoad = refreshedCurrentItem != null ? refreshedCurrentItem : nodeData.httpRequestItem;
        if (itemToLoad != null) {
            nodeData.httpRequestItem = itemToLoad;
            switchRequestEditorAction.accept(itemToLoad);
        }
        return newNode;
    }

    private int refreshTreeNode(DefaultMutableTreeNode treeNode, List<DefaultMutableTreeNode> nodesToRemove) {
        int updatedCount = 0;

        Object userObj = treeNode.getUserObject();
        if (userObj instanceof PerformanceTreeNode jmNode
                && jmNode.type == NodeType.REQUEST
                && jmNode.httpRequestItem != null) {
            String requestId = jmNode.httpRequestItem.getId();
            if (requestId == null || requestId.trim().isEmpty()) {
                log.warn("刷新集合请求时发现请求节点 id 为空，已移除: {}", jmNode.name);
                nodesToRemove.add(treeNode);
            } else {
                HttpRequestItem latestRequestItem = requestResolver.findRequestItemById(requestId);
                if (latestRequestItem == null) {
                    log.warn("刷新集合请求时找不到 requestId={}，已移除: {}", requestId, jmNode.name);
                    nodesToRemove.add(treeNode);
                } else {
                    jmNode.httpRequestItem = latestRequestItem;
                    syncRequestStructureAction.accept(treeNode, jmNode);
                    jmNode.name = latestRequestItem.getName();
                    treeModel.nodeChanged(treeNode);
                    PreparedRequestBuilder.invalidateCacheForRequest(requestId);
                    updatedCount++;
                }
            }
        }

        for (int i = 0; i < treeNode.getChildCount(); i++) {
            updatedCount += refreshTreeNode((DefaultMutableTreeNode) treeNode.getChildAt(i), nodesToRemove);
        }
        return updatedCount;
    }

    private TreePath findTreePathByPath(TreePath oldPath) {
        if (oldPath == null) {
            return null;
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        Object[] oldPathObjects = oldPath.getPath();
        DefaultMutableTreeNode currentNode = root;
        List<DefaultMutableTreeNode> newPath = new ArrayList<>();
        newPath.add(root);

        for (int i = 1; i < oldPathObjects.length; i++) {
            Object oldNodeUserObj = ((DefaultMutableTreeNode) oldPathObjects[i]).getUserObject();
            if (!(oldNodeUserObj instanceof PerformanceTreeNode oldJmNode)) {
                return null;
            }

            DefaultMutableTreeNode matchedChild = null;
            for (int j = 0; j < currentNode.getChildCount(); j++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentNode.getChildAt(j);
                Object childUserObj = child.getUserObject();
                if (childUserObj instanceof PerformanceTreeNode childJmNode
                        && childJmNode.type == oldJmNode.type
                        && childJmNode.name.equals(oldJmNode.name)) {
                    matchedChild = child;
                    break;
                }
            }

            if (matchedChild == null) {
                return null;
            }
            newPath.add(matchedChild);
            currentNode = matchedChild;
        }

        return new TreePath(newPath.toArray());
    }
}
