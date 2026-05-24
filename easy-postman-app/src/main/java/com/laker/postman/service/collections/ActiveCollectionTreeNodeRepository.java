package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Optional;

/**
 * 树节点仓库默认实现。
 * <p>
 * 通过 CollectionTreeRootRegistry 读取当前 UI 已注册的 collection 树根节点，
 * service 层不直接创建或查找 Swing panel。
 */
@Slf4j
public class ActiveCollectionTreeNodeRepository implements TreeNodeRepository {

    @Override
    public Optional<DefaultMutableTreeNode> findNodeByRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<DefaultMutableTreeNode> rootOpt = getRootNode();
        if (rootOpt.isEmpty()) {
            log.trace("根节点为空，无法查找节点: {}", requestId);
            return Optional.empty();
        }

        return findByTraversal(rootOpt.get(), requestId);
    }

    @Override
    public Optional<DefaultMutableTreeNode> getRootNode() {
        return CollectionTreeRootRegistry.getRootNode();
    }

    private Optional<DefaultMutableTreeNode> findByTraversal(
            DefaultMutableTreeNode root,
            String requestId) {

        if (root == null || requestId == null) {
            return Optional.empty();
        }

        Optional<HttpRequestItem> request = CollectionTreeNodes.request(root);
        if (request.isPresent() && requestId.equals(request.get().getId())) {

            log.trace("找到节点: {}", requestId);
            return Optional.of(root);
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode child = root.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode childNode) {
                Optional<DefaultMutableTreeNode> result = findByTraversal(childNode, requestId);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return Optional.empty();
    }
}
