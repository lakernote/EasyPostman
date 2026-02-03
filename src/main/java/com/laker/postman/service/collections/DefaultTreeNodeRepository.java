package com.laker.postman.service.collections;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.GROUP;
import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

/**
 * 树节点仓库默认实现
 * <p>
 * 基于 RequestCollectionsLeftPanel 的适配器实现
 * <p>
 * 新增特性：
 * - 内置索引缓存（O(1) 查找）
 * - 自动索引维护
 * - 与 UI 层解耦
 *
 * @author laker
 * @since 4.3.22
 */
@Slf4j
public class DefaultTreeNodeRepository implements TreeNodeRepository {

    private final List<TreeChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 请求节点索引（性能优化：O(1) 查找）
     * requestId -> TreeNode
     */
    private final ConcurrentHashMap<String, DefaultMutableTreeNode> requestNodeIndex = new ConcurrentHashMap<>();

    /**
     * 索引是否已初始化
     */
    private volatile boolean indexInitialized = false;

    @Override
    public Optional<DefaultMutableTreeNode> findNodeByRequestId(String requestId) {
        if (requestId == null) {
            return Optional.empty();
        }

        // 确保索引已初始化
        ensureIndexInitialized();

        // 使用索引查找（O(1)）
        DefaultMutableTreeNode indexed = requestNodeIndex.get(requestId);
        if (indexed != null) {
            return Optional.of(indexed);
        }

        // 索引未命中，降级到树遍历（O(n)）
        return getRootNode().flatMap(root -> findByTraversal(root, requestId));
    }

    @Override
    public List<RequestGroup> getAncestorGroups(DefaultMutableTreeNode requestNode) {
        List<RequestGroup> groupChain = new ArrayList<>();

        if (requestNode == null) {
            return groupChain;
        }

        TreeNode currentNode = requestNode.getParent();

        while (currentNode != null) {
            if (!(currentNode instanceof DefaultMutableTreeNode treeNode)) {
                break;
            }

            Object userObj = treeNode.getUserObject();

            // 检查是否是根节点
            if ("root".equals(String.valueOf(userObj))) {
                break;
            }

            // 检查是否是分组节点
            if (userObj instanceof Object[] obj && GROUP.equals(obj[0])) {
                Object groupData = obj[1];
                if (groupData instanceof RequestGroup group) {
                    groupChain.add(0, group); // 插入到列表头部，保持外层到内层的顺序
                }
            }

            currentNode = currentNode.getParent();
        }

        return groupChain;
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

    @Override
    public void registerChangeListener(TreeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeChangeListener(TreeChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器树结构已变更
     */
    public void notifyTreeChanged() {
        // 清空索引，强制重建
        invalidateIndex();

        // 通知监听器
        for (TreeChangeListener listener : listeners) {
            try {
                listener.onTreeChanged();
            } catch (Exception e) {
                log.error("通知监听器失败", e);
            }
        }
    }

    /**
     * 使索引失效（树结构变更时调用）
     */
    public void invalidateIndex() {
        requestNodeIndex.clear();
        indexInitialized = false;
        log.debug("请求节点索引已失效");
    }

    /**
     * 确保索引已初始化
     */
    private void ensureIndexInitialized() {
        if (!indexInitialized) {
            synchronized (this) {
                if (!indexInitialized) {
                    rebuildIndex();
                    indexInitialized = true;
                }
            }
        }
    }

    /**
     * 重建索引
     */
    private void rebuildIndex() {
        requestNodeIndex.clear();

        Optional<DefaultMutableTreeNode> rootOpt = getRootNode();
        if (rootOpt.isEmpty()) {
            log.debug("根节点为空，跳过索引重建");
            return;
        }

        DefaultMutableTreeNode root = rootOpt.get();
        buildIndexRecursively(root);

        log.debug("请求节点索引重建完成，共 {} 个请求", requestNodeIndex.size());
    }

    /**
     * 递归构建索引
     */
    private void buildIndexRecursively(DefaultMutableTreeNode node) {
        if (node == null) {
            return;
        }

        Object userObj = node.getUserObject();

        // 如果是请求节点，加入索引
        if (userObj instanceof Object[] obj && REQUEST.equals(obj[0])) {
            HttpRequestItem req = (HttpRequestItem) obj[1];
            if (req.getId() != null) {
                requestNodeIndex.put(req.getId(), node);
            }
        }

        // 递归处理子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            buildIndexRecursively(child);
        }
    }

    /**
     * 添加节点到索引（外部调用，用于增量更新）
     *
     * @param requestId 请求ID
     * @param node 节点
     */
    public void addToIndex(String requestId, DefaultMutableTreeNode node) {
        if (requestId != null && node != null) {
            requestNodeIndex.put(requestId, node);
            log.trace("节点已添加到索引: {}", requestId);
        }
    }

    /**
     * 从索引中移除节点（外部调用，用于增量更新）
     *
     * @param requestId 请求ID
     */
    public void removeFromIndex(String requestId) {
        if (requestId != null) {
            DefaultMutableTreeNode removed = requestNodeIndex.remove(requestId);
            if (removed != null) {
                log.trace("节点已从索引移除: {}", requestId);
            }
        }
    }

    /**
     * 获取索引大小（用于监控）
     *
     * @return 索引中的节点数量
     */
    public int getIndexSize() {
        return requestNodeIndex.size();
    }

    /**
     * 通过树遍历查找节点（降级方案）
     */
    private Optional<DefaultMutableTreeNode> findByTraversal(
            DefaultMutableTreeNode root,
            String requestId) {

        if (root == null || requestId == null) {
            return Optional.empty();
        }

        // 检查当前节点
        Object userObj = root.getUserObject();
        if (userObj instanceof Object[] obj && REQUEST.equals(obj[0])) {
            HttpRequestItem req = (HttpRequestItem) obj[1];
            if (requestId.equals(req.getId())) {
                // 找到了，加入索引
                requestNodeIndex.put(requestId, root);
                return Optional.of(root);
            }
        }

        // 递归搜索子节点
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            Optional<DefaultMutableTreeNode> result = findByTraversal(child, requestId);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }
}
