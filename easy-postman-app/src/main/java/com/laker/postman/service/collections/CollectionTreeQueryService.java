package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class CollectionTreeQueryService {
    public static HttpRequestItem getLastNonNewRequest() {
        return getLastNonNewRequest(OpenedRequestTabsStore.loadAll());
    }

    public static HttpRequestItem getLastNonNewRequest(List<HttpRequestItem> requestItems) {
        if (requestItems == null || requestItems.isEmpty()) {
            return null;
        }
        for (int i = requestItems.size() - 1; i >= 0; i--) {
            HttpRequestItem item = requestItems.get(i);
            if (!item.isNewRequest()) {
                return item;
            }
        }
        return null;
    }

    public static List<HttpRequestItem> buildRestorableOpenedRequests(List<HttpRequestItem> requestItems,
                                                                      DefaultMutableTreeNode rootNode) {
        if (requestItems == null || requestItems.isEmpty()) {
            return List.of();
        }

        List<HttpRequestItem> restorableItems = new ArrayList<>();
        for (HttpRequestItem item : requestItems) {
            // 验证请求对象和ID是否有效
            if (item == null || item.getId() == null || item.getId().isEmpty()) {
                log.warn("Skip restoring request as it is null or has null/empty ID: {}",
                        item != null ? item.getName() : "null");
                continue;
            }

            HttpRequestItem resolvedItem = item;
            // 如果不是新请求，从tree中找到完整数据
            if (!item.isNewRequest()) {
                DefaultMutableTreeNode node = findRequestNodeById(rootNode, item.getId());
                if (node == null) {
                    // 请求在左侧树中已不存在，跳过恢复
                    log.warn("Skip restoring request {} (id={}) as it no longer exists in the tree",
                            item.getName(), item.getId());
                    continue;
                }
                resolvedItem = CollectionTreeNodes.request(node).orElse(null);
                if (resolvedItem == null) {
                    log.warn("Skip restoring request {} (id={}) as the tree node payload is invalid",
                            item.getName(), item.getId());
                    continue;
                }
            }
            restorableItems.add(resolvedItem);
        }
        return restorableItems;
    }

    /**
     * 根据ID查找请求节点
     */
    public static DefaultMutableTreeNode findRequestNodeById(DefaultMutableTreeNode node, String id) {
        if (node == null) return null;

        var request = CollectionTreeNodes.request(node);
        if (request.isPresent()) {
            HttpRequestItem item = request.get();
            if (id.equals(item.getId())) {
                return node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode result = findRequestNodeById(child, id);
            if (result != null) {
                return result;
            }
        }

        return null;
    }
}
