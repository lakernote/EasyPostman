package com.laker.postman.service.collections;

import com.laker.postman.model.RequestGroup;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.Optional;

/**
 * 树节点仓库接口
 * <p>
 * 职责：提供树节点的查找和访问功能，隔离UI层的树结构
 * <p>
 * 设计模式：Repository Pattern（仓库模式）
 * <p>
 * 优势：
 * - 解耦业务逻辑与树结构
 * - 便于单元测试（可Mock）
 * - 统一数据访问接口
 *
 * @author laker
 * @since 4.3.22
 */
public interface TreeNodeRepository {

    /**
     * 通过请求ID查找节点
     *
     * @param requestId 请求ID
     * @return 节点（如果存在）
     */
    Optional<DefaultMutableTreeNode> findNodeByRequestId(String requestId);

    /**
     * 获取节点的所有祖先分组（从外到内排序）
     *
     * @param requestNode 请求节点
     * @return 分组链表（外层到内层）
     */
    List<RequestGroup> getAncestorGroups(DefaultMutableTreeNode requestNode);

    /**
     * 获取根节点
     *
     * @return 根节点
     */
    Optional<DefaultMutableTreeNode> getRootNode();

    /**
     * 注册树结构变更监听器
     *
     * @param listener 监听器
     */
    void registerChangeListener(TreeChangeListener listener);

    /**
     * 移除树结构变更监听器
     *
     * @param listener 监听器
     */
    void removeChangeListener(TreeChangeListener listener);

    /**
     * 树结构变更监听器
     */
    interface TreeChangeListener {
        /**
         * 树结构发生变更时调用
         */
        void onTreeChanged();
    }
}
