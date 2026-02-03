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
 * 设计原则：简单、直接、易于理解
 * <p>
 * 核心职责：
 * - 根据请求ID查找树节点（O(1) 性能）
 * - 获取请求的父分组链
 * - 监听树变更事件
 * <p>
 * 实现策略：
 * - 使用索引缓存提升查找性能（requestId -> TreeNode）
 * - 索引按需构建，树变更时失效
 * - 索引未命中时自动重建（自愈机制）
 *
 * @author laker
 * @since 4.3.22
 */
@Slf4j
public class DefaultTreeNodeRepository implements TreeNodeRepository {

    /**
     * 树变更监听器列表
     * 使用 CopyOnWriteArrayList 保证线程安全
     */
    private final List<TreeChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 请求节点索引（性能优化：O(1) 查找）
     * <p>
     * Key: requestId
     * Value: 该请求在树中的节点
     * <p>
     * 为什么需要索引？
     * - 不使用索引：需要遍历整棵树查找，O(n) 时间复杂度
     * - 使用索引：直接查 Map，O(1) 时间复杂度
     */
    private final ConcurrentHashMap<String, DefaultMutableTreeNode> requestNodeIndex = new ConcurrentHashMap<>();

    /**
     * 索引状态标记
     * <p>
     * true: 索引有效，可以使用
     * false: 索引失效，需要重建
     */
    private volatile boolean indexValid = false;

    // ==================== 公共 API ====================

    @Override
    public Optional<DefaultMutableTreeNode> findNodeByRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }

        // 确保索引有效
        ensureIndexValid();

        // 从索引中查找
        DefaultMutableTreeNode node = requestNodeIndex.get(requestId);

        if (node != null) {
            log.trace("索引命中: {}", requestId);
            return Optional.of(node);
        }

        // 索引未命中 -> 可能是新增的节点或索引损坏
        // 尝试重建索引后再查一次
        log.debug("索引未命中: {}, 尝试重建索引", requestId);
        rebuildIndex();

        node = requestNodeIndex.get(requestId);
        if (node != null) {
            log.debug("重建索引后找到节点: {}", requestId);
            return Optional.of(node);
        }

        log.trace("节点不存在: {}", requestId);
        return Optional.empty();
    }

    @Override
    public List<RequestGroup> getAncestorGroups(DefaultMutableTreeNode requestNode) {
        List<RequestGroup> groupChain = new ArrayList<>();

        if (requestNode == null) {
            return groupChain;
        }

        // 从请求节点向上遍历，收集所有父分组
        TreeNode currentNode = requestNode.getParent();

        while (currentNode != null) {
            // 检查节点类型
            if (!(currentNode instanceof DefaultMutableTreeNode treeNode)) {
                break;
            }

            Object userObj = treeNode.getUserObject();

            // 到达根节点，停止
            if (userObj == null || "root".equals(String.valueOf(userObj))) {
                break;
            }

            // 检查是否是分组节点
            if (userObj instanceof Object[] obj && obj.length >= 2 && GROUP.equals(obj[0])) {
                if (obj[1] instanceof RequestGroup group) {
                    // 插入到列表头部，保持"外层到内层"的顺序
                    groupChain.add(0, group);
                }
            }

            // 继续向上
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
            log.debug("已注册树变更监听器");
        }
    }

    @Override
    public void removeChangeListener(TreeChangeListener listener) {
        listeners.remove(listener);
        log.debug("已移除树变更监听器");
    }

    // ==================== 索引管理 ====================

    /**
     * 使索引失效
     * <p>
     * 调用时机：
     * - 添加/删除/移动节点
     * - 导入 Collection
     * - 任何会改变树结构的操作
     */
    public void invalidateIndex() {
        requestNodeIndex.clear();
        indexValid = false;
        log.debug("请求节点索引已失效");
    }

    /**
     * 通知所有监听器：树结构已变更
     * <p>
     * 这个方法应该在所有修改树结构的地方被调用
     */
    public void notifyTreeChanged() {
        // 1. 失效索引
        invalidateIndex();

        // 2. 通知监听器
        for (TreeChangeListener listener : listeners) {
            try {
                listener.onTreeChanged();
            } catch (Exception e) {
                log.error("通知监听器失败", e);
            }
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 确保索引有效
     * <p>
     * 如果索引无效，则重建索引
     * 使用双重检查锁定（DCL）避免重复构建
     */
    private void ensureIndexValid() {
        // 第一次检查（无锁）
        if (indexValid) {
            return;
        }

        // 第二次检查（有锁）
        synchronized (this) {
            if (!indexValid) {
                rebuildIndex();
                indexValid = true;
            }
        }
    }

    /**
     * 重建索引
     * <p>
     * 策略：
     * 1. 清空现有索引
     * 2. 遍历整棵树
     * 3. 将所有请求节点加入索引
     */
    private void rebuildIndex() {
        requestNodeIndex.clear();

        // 获取根节点
        Optional<DefaultMutableTreeNode> rootOpt = getRootNode();
        if (rootOpt.isEmpty()) {
            log.debug("根节点为空，无法构建索引");
            return;
        }

        // 递归遍历树，构建索引
        DefaultMutableTreeNode root = rootOpt.get();
        int count = buildIndexRecursively(root);

        log.debug("请求节点索引重建完成，共 {} 个请求", count);
    }

    /**
     * 递归构建索引
     * <p>
     * 遍历树的每个节点，如果是请求节点就加入索引
     *
     * @param node 当前节点
     * @return 已索引的请求数量
     */
    private int buildIndexRecursively(DefaultMutableTreeNode node) {
        if (node == null) {
            return 0;
        }

        int count = 0;
        Object userObj = node.getUserObject();

        // 如果是请求节点，加入索引
        if (userObj instanceof Object[] obj &&
            obj.length >= 2 &&
            REQUEST.equals(obj[0]) &&
            obj[1] instanceof HttpRequestItem req &&
            req.getId() != null) {

            requestNodeIndex.put(req.getId(), node);
            count++;
        }

        // 递归处理子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            TreeNode child = node.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode childNode) {
                count += buildIndexRecursively(childNode);
            }
        }

        return count;
    }
}
