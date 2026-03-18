package com.laker.postman.service.collections;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Optional;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

/**
 * 树节点仓库默认实现
 * <p>
 * 核心职责：
 * - 根据请求ID查找树节点（通过遍历树）
 * <p>
 *
 * @author laker
 * @since 4.3.22
 */
@Slf4j
public class DefaultTreeNodeRepository implements TreeNodeRepository {

    @Override
    public Optional<DefaultMutableTreeNode> findNodeByRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }

        // 获取根节点
        Optional<DefaultMutableTreeNode> rootOpt = getRootNode();
        if (rootOpt.isEmpty()) {
            log.trace("根节点为空，无法查找节点: {}", requestId);
            return Optional.empty();
        }

        // 遍历树查找节点
        return findByTraversal(rootOpt.get(), requestId);
    }

    @Override
    public Optional<DefaultMutableTreeNode> getRootNode() {
        try {
            RequestCollectionsLeftPanel leftPanel =
                    SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);

            DefaultMutableTreeNode root = leftPanel.getRootTreeNode();
            return Optional.ofNullable(root);

        } catch (Exception e) {
            log.debug("获取根节点失败: {}", e.getMessage());
            return Optional.empty();
        }
    }


    /**
     * 通过遍历树查找节点
     * <p>
     * 递归遍历整棵树，找到匹配 requestId 的请求节点
     *
     * @param root      当前节点
     * @param requestId 请求ID
     * @return 找到的节点（Optional）
     */
    private Optional<DefaultMutableTreeNode> findByTraversal(
            DefaultMutableTreeNode root,
            String requestId) {

        if (root == null || requestId == null) {
            return Optional.empty();
        }

        // 检查当前节点
        Object userObj = root.getUserObject();
        if (userObj instanceof Object[] obj &&
                obj.length >= 2 &&
                REQUEST.equals(obj[0]) &&
                obj[1] instanceof HttpRequestItem req &&
                requestId.equals(req.getId())) {

            log.trace("找到节点: {}", requestId);
            return Optional.of(root);
        }

        // 递归搜索子节点
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
