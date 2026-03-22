package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.model.ApiMetadata;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.service.PerformancePersistenceService;
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
    private final JTree jmeterTree;
    private final PerformancePersistenceService persistenceService;
    private final BiConsumer<DefaultMutableTreeNode, JMeterTreeNode> syncRequestStructureAction;

    DefaultMutableTreeNode refreshCurrentRequest(DefaultMutableTreeNode currentRequestNode,
                                                 RequestEditSubPanel requestEditSubPanel,
                                                 Consumer<HttpRequestItem> switchRequestEditorAction,
                                                 Runnable saveConfigAction) {
        log.debug("[refreshCurrentRequest] 开始执行单节点刷新, currentRequestNode={}", currentRequestNode);
        if (currentRequestNode == null) {
            log.debug("[refreshCurrentRequest] currentRequestNode 为 null，退出");
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_SELECTED));
            return null;
        }

        Object userObj = currentRequestNode.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jmNode) || jmNode.type != NodeType.REQUEST) {
            log.debug("[refreshCurrentRequest] 当前节点类型不是 REQUEST，退出");
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_SELECTED));
            return currentRequestNode;
        }

        String editSubId = requestEditSubPanel.getId();
        String nodeId = jmNode.httpRequestItem != null ? jmNode.httpRequestItem.getId() : null;
        log.debug("[refreshCurrentRequest] editSub.id={}, node.httpRequestItem.id={}", editSubId, nodeId);

        String requestId = editSubId;
        if (requestId == null || requestId.trim().isEmpty()) {
            log.debug("[refreshCurrentRequest] editSub.id 为空，降级使用 node.id");
            if (jmNode.httpRequestItem == null) {
                log.debug("[refreshCurrentRequest] node.httpRequestItem 也为 null，退出");
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_SELECTED));
                return currentRequestNode;
            }
            requestId = nodeId;
        }

        if (requestId == null || requestId.trim().isEmpty()) {
            log.debug("[refreshCurrentRequest] requestId 最终为空，退出");
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_NOT_FOUND_IN_COLLECTIONS));
            return currentRequestNode;
        }

        log.debug("[refreshCurrentRequest] 准备从集合中查找请求, requestId={}", requestId);
        HttpRequestItem latestRequestItem = persistenceService.findRequestItemById(requestId);
        log.debug("[refreshCurrentRequest] 集合中找到的请求: name={}, url={}, id={}",
                latestRequestItem != null ? latestRequestItem.getName() : "null",
                latestRequestItem != null ? latestRequestItem.getUrl() : "null",
                latestRequestItem != null ? latestRequestItem.getId() : "null");

        if (latestRequestItem == null) {
            log.debug("[refreshCurrentRequest] 集合中未找到 requestId={} 对应的请求，退出", requestId);
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_NOT_FOUND_IN_COLLECTIONS));
            return currentRequestNode;
        }

        jmNode.httpRequestItem = latestRequestItem;
        syncRequestStructureAction.accept(currentRequestNode, jmNode);
        jmNode.name = latestRequestItem.getName();
        treeModel.nodeChanged(currentRequestNode);
        PreparedRequestBuilder.invalidateCacheForRequest(requestId);
        log.debug("[refreshCurrentRequest] 已将节点数据更新并清除 InheritanceCache, name={}, requestId={}",
                latestRequestItem.getName(), requestId);

        log.debug("[refreshCurrentRequest] 调用 initPanelData, url={}", latestRequestItem.getUrl());
        switchRequestEditorAction.accept(latestRequestItem);
        log.debug("[refreshCurrentRequest] initPanelData 执行完毕，editSub.id={}", requestEditSubPanel.getId());

        saveConfigAction.run();
        NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_REFRESHED));
        log.debug("[refreshCurrentRequest] 单节点刷新完成");
        return currentRequestNode;
    }

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
                                                          RequestEditSubPanel requestEditSubPanel,
                                                          Consumer<HttpRequestItem> switchRequestEditorAction,
                                                          Runnable saveAllPropertyPanelDataAction,
                                                          Runnable clearCachedResultAction,
                                                          Runnable saveConfigAction) {
        log.debug("[refreshFromCollections] 开始执行全局刷新");
        saveAllPropertyPanelDataAction.run();
        log.debug("[refreshFromCollections] saveAllPropertyPanelData 执行完毕");

        clearCachedResultAction.run();
        ApiMetadata.clear();
        System.gc();

        List<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        int updatedCount = refreshTreeNode(root, nodesToRemove);
        int removedCount = nodesToRemove.size();
        log.debug("[refreshFromCollections] refreshTreeNode 完毕: updatedCount={}, removedCount={}", updatedCount, removedCount);

        for (int i = nodesToRemove.size() - 1; i >= 0; i--) {
            DefaultMutableTreeNode nodeToRemove = nodesToRemove.get(i);
            if (nodeToRemove == currentRequestNode) {
                currentRequestNode = null;
            }
            treeModel.removeNodeFromParent(nodeToRemove);
        }

        HttpRequestItem refreshedCurrentItem = extractCurrentItemBeforeReload(currentRequestNode);
        log.debug("[refreshFromCollections] reload 前 editSub.id={}, editSub.url={}",
                requestEditSubPanel.getId(),
                requestEditSubPanel.getRequestLinePanel() != null ? "有requestLinePanel" : "null");

        TreePath currentPath = currentRequestNode != null ? new TreePath(currentRequestNode.getPath()) : null;
        saveConfigAction.run();

        log.debug("[refreshFromCollections] 即将调用 treeModel.reload()");
        treeModel.reload();
        log.debug("[refreshFromCollections] treeModel.reload() 执行完毕，当前节点数据可能已被 listener 覆盖");
        logCurrentNodeAfterReload(currentRequestNode);

        for (int i = 0; i < jmeterTree.getRowCount(); i++) {
            jmeterTree.expandRow(i);
        }

        DefaultMutableTreeNode refreshedNode = relocateCurrentRequestNode(
                currentPath,
                currentRequestNode,
                refreshedCurrentItem,
                requestEditSubPanel,
                switchRequestEditorAction
        );

        if (removedCount > 0) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REFRESH_WARNING, removedCount));
        } else if (updatedCount > 0) {
            NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REFRESH_SUCCESS, updatedCount));
        } else {
            NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_TO_REFRESH));
        }
        log.debug("[refreshFromCollections] 全局刷新完成");
        return refreshedNode;
    }

    private void syncRequestItemInTree(DefaultMutableTreeNode node,
                                       HttpRequestItem item,
                                       DefaultMutableTreeNode currentRequestNode,
                                       Consumer<HttpRequestItem> switchRequestEditorAction) {
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode
                && jtNode.type == NodeType.REQUEST
                && jtNode.httpRequestItem != null
                && item.getId().equals(jtNode.httpRequestItem.getId())) {
            jtNode.httpRequestItem = item;
            syncRequestStructureAction.accept(node, jtNode);
            jtNode.name = item.getName();
            treeModel.nodeChanged(node);
            if (node == currentRequestNode) {
                switchRequestEditorAction.accept(item);
            }
            log.debug("PerformancePanel syncRequestItem: id={}, name={}", item.getId(), item.getName());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            syncRequestItemInTree((DefaultMutableTreeNode) node.getChildAt(i), item, currentRequestNode, switchRequestEditorAction);
        }
    }

    private HttpRequestItem extractCurrentItemBeforeReload(DefaultMutableTreeNode currentRequestNode) {
        if (currentRequestNode == null) {
            log.debug("[refreshFromCollections] currentRequestNode 为 null，无需保存当前节点数据");
            return null;
        }
        Object curUserObj = currentRequestNode.getUserObject();
        if (curUserObj instanceof JMeterTreeNode curJmNode
                && curJmNode.type == NodeType.REQUEST
                && curJmNode.httpRequestItem != null) {
            HttpRequestItem refreshedCurrentItem = curJmNode.httpRequestItem;
            log.debug("[refreshFromCollections] reload 前保存当前节点最新数据: name={}, url={}, id={}",
                    refreshedCurrentItem.getName(), refreshedCurrentItem.getUrl(), refreshedCurrentItem.getId());
            return refreshedCurrentItem;
        }
        return null;
    }

    private void logCurrentNodeAfterReload(DefaultMutableTreeNode currentRequestNode) {
        if (currentRequestNode == null) {
            return;
        }
        Object curObj = currentRequestNode.getUserObject();
        if (curObj instanceof JMeterTreeNode curJmNode) {
            log.debug("[refreshFromCollections] reload 后 currentRequestNode.httpRequestItem: name={}, url={}",
                    curJmNode.httpRequestItem != null ? curJmNode.httpRequestItem.getName() : "null",
                    curJmNode.httpRequestItem != null ? curJmNode.httpRequestItem.getUrl() : "null");
        }
    }

    private DefaultMutableTreeNode relocateCurrentRequestNode(TreePath currentPath,
                                                              DefaultMutableTreeNode currentRequestNode,
                                                              HttpRequestItem refreshedCurrentItem,
                                                              RequestEditSubPanel requestEditSubPanel,
                                                              Consumer<HttpRequestItem> switchRequestEditorAction) {
        if (currentPath == null) {
            log.debug("[refreshFromCollections] currentPath 为 null，跳过重新定位");
            return currentRequestNode;
        }

        TreePath newPath = findTreePathByPath(currentPath);
        log.debug("[refreshFromCollections] findTreePathByPath 结果: {}", newPath != null ? "找到" : "未找到");
        if (newPath == null) {
            log.debug("[refreshFromCollections] 未找到匹配路径，currentRequestNode 已清空");
            return null;
        }

        jmeterTree.setSelectionPath(newPath);
        DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) newPath.getLastPathComponent();
        Object userObj = newNode.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jtNode) || jtNode.type != NodeType.REQUEST) {
            log.debug("[refreshFromCollections] newPath 最后节点不是 REQUEST 类型");
            return null;
        }

        HttpRequestItem itemToLoad = refreshedCurrentItem != null ? refreshedCurrentItem : jtNode.httpRequestItem;
        log.debug("[refreshFromCollections] 准备 initPanelData: 使用{}，name={}, url={}",
                refreshedCurrentItem != null ? "reload前保存的数据" : "节点数据",
                itemToLoad != null ? itemToLoad.getName() : "null",
                itemToLoad != null ? itemToLoad.getUrl() : "null");
        if (itemToLoad != null) {
            jtNode.httpRequestItem = itemToLoad;
            switchRequestEditorAction.accept(itemToLoad);
            log.debug("[refreshFromCollections] initPanelData 执行完毕，editSub.id={}", requestEditSubPanel.getId());
        }
        return newNode;
    }

    private int refreshTreeNode(DefaultMutableTreeNode treeNode, List<DefaultMutableTreeNode> nodesToRemove) {
        int updatedCount = 0;

        Object userObj = treeNode.getUserObject();
        if (userObj instanceof JMeterTreeNode jmNode
                && jmNode.type == NodeType.REQUEST
                && jmNode.httpRequestItem != null) {
            String requestId = jmNode.httpRequestItem.getId();
            log.debug("[refreshTreeNode] 处理请求节点: name={}, requestId={}, url={}",
                    jmNode.name, requestId, jmNode.httpRequestItem.getUrl());
            if (requestId == null || requestId.trim().isEmpty()) {
                log.warn("[refreshTreeNode] 请求节点 id 为空，标记删除: name={}", jmNode.name);
                nodesToRemove.add(treeNode);
            } else {
                HttpRequestItem latestRequestItem = persistenceService.findRequestItemById(requestId);
                if (latestRequestItem == null) {
                    log.warn("[refreshTreeNode] 集合中找不到 requestId={}，标记删除: name={}", requestId, jmNode.name);
                    nodesToRemove.add(treeNode);
                } else {
                    log.debug("[refreshTreeNode] 集合中找到最新数据: name={}, url={} -> name={}, url={}",
                            jmNode.name, jmNode.httpRequestItem.getUrl(),
                            latestRequestItem.getName(), latestRequestItem.getUrl());
                    jmNode.httpRequestItem = latestRequestItem;
                    syncRequestStructureAction.accept(treeNode, jmNode);
                    jmNode.name = latestRequestItem.getName();
                    treeModel.nodeChanged(treeNode);
                    PreparedRequestBuilder.invalidateCacheForRequest(requestId);
                    log.debug("[refreshTreeNode] 已清除 requestId={} 的 InheritanceCache", requestId);
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
            if (!(oldNodeUserObj instanceof JMeterTreeNode oldJmNode)) {
                return null;
            }

            DefaultMutableTreeNode matchedChild = null;
            for (int j = 0; j < currentNode.getChildCount(); j++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentNode.getChildAt(j);
                Object childUserObj = child.getUserObject();
                if (childUserObj instanceof JMeterTreeNode childJmNode
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
