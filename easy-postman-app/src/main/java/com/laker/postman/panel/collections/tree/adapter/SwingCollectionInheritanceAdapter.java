package com.laker.postman.panel.collections.tree.adapter;

import com.laker.postman.collection.CollectionInheritance;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.service.collections.CollectionTreeNodes;


import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;


/**
 * Swing collection tree 继承上下文适配器。
 * <p>
 * 这里只负责从 Swing tree node 提取 groupChain 和分组变量；认证、脚本、请求头等合并规则由
 * collection-core 的 {@link CollectionInheritance} 统一处理。
 */
@UtilityClass
public class SwingCollectionInheritanceAdapter {

    /**
     * 收集分组链（从外到内）
     * <p>
     * 优化点：
     * - 使用迭代代替递归，避免栈溢出
     * - 直接向上遍历，性能为 O(h)，h 为树的高度
     */
    public static List<RequestGroup> collectGroupChain(DefaultMutableTreeNode startNode) {
        List<RequestGroup> groupChain = new ArrayList<>();

        if (startNode == null) {
            return groupChain;
        }

        TreeNode currentNode = startNode.getParent(); // 从父节点开始

        while (currentNode != null) {
            if (!(currentNode instanceof DefaultMutableTreeNode treeNode)) {
                break;
            }

            // 检查是否是根节点
            if (treeNode.getUserObject() == null || "root".equals(String.valueOf(treeNode.getUserObject()))) {
                break;
            }

            CollectionTreeNodes.group(treeNode)
                    .ifPresent(group -> groupChain.add(0, group)); // 插入到列表头部，保持外层到内层的顺序

            currentNode = currentNode.getParent();
        }

        return groupChain;
    }

    /**
     * 获取合并后的分组级别变量
     * <p>
     * 用于变量解析器获取当前请求所继承的所有分组变量
     * 变量优先级：内层分组覆盖外层分组
     *
     * @param requestNode 请求在树中的节点
     * @return 合并后的变量列表，按优先级排序（内层优先）
     */
    public static List<Variable> getMergedGroupVariables(DefaultMutableTreeNode requestNode) {
        if (requestNode == null) {
            return new ArrayList<>();
        }

        List<RequestGroup> groupChain = collectGroupChain(requestNode);
        if (groupChain.isEmpty()) {
            return new ArrayList<>();
        }

        return CollectionInheritance.mergeGroupVariables(groupChain);
    }

}
