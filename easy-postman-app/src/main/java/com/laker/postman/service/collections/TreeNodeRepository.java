package com.laker.postman.service.collections;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Optional;

/**
 * 树节点仓库接口
 * <p>
 * 职责：提供树节点的查找和访问功能，隔离UI层的树结构
 * <p>
 * 设计模式：Repository Pattern（仓库模式）
 * <p>
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
     * 获取根节点
     *
     * @return 根节点
     */
    Optional<DefaultMutableTreeNode> getRootNode();

}
