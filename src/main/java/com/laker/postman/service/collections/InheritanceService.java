package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Optional;

/**
 * 继承服务
 * <p>
 * 设计模式：Facade Pattern（门面模式）
 * <p>
 * 职责：
 * - 统一的继承计算入口
 * - 协调各个组件（Repository, Cache, Helper）
 * - 简化客户端调用
 * <p>
 *
 * @author laker
 * @since 4.3.22
 */
@Slf4j
public class InheritanceService {

    private final TreeNodeRepository treeRepository;
    private final InheritanceCache cache;

    /**
     * 创建继承服务（使用默认实现）
     */
    public InheritanceService() {
        this(new DefaultTreeNodeRepository(), new InheritanceCache());

        // 注册树变更监听器，自动失效缓存
        treeRepository.registerChangeListener(() -> {
            cache.clear(); // 简化：直接清空
            // 同时失效 Repository 的索引
            if (treeRepository instanceof DefaultTreeNodeRepository repo) {
                repo.invalidateIndex();
            }
        });
    }

    /**
     * 创建继承服务（依赖注入，便于测试）
     *
     * @param treeRepository 树节点仓库
     * @param cache          缓存管理器
     */
    public InheritanceService(TreeNodeRepository treeRepository, InheritanceCache cache) {
        this.treeRepository = treeRepository;
        this.cache = cache;
    }

    /**
     * 应用分组继承规则
     * <p>
     * 核心入口方法，统一处理所有继承逻辑
     *
     * @param item 原始请求项
     * @return 应用了继承后的请求项（新对象），如果不需要继承则返回原对象
     */
    public HttpRequestItem applyInheritance(HttpRequestItem item) {
        return applyInheritance(item, true);
    }

    /**
     * 应用分组继承规则（可选是否使用缓存）
     * <p>
     * 用于处理未保存的请求（如 UI 中修改但未保存）
     *
     * @param item 原始请求项
     * @param useCache 是否使用缓存
     * @return 应用了继承后的请求项（新对象），如果不需要继承则返回原对象
     */
    public HttpRequestItem applyInheritance(HttpRequestItem item, boolean useCache) {
        if (item == null) {
            return null;
        }

        // 1. 如果使用缓存，先检查缓存
        String requestId = item.getId();
        if (useCache && requestId != null) {
            Optional<HttpRequestItem> cached = cache.get(requestId);
            if (cached.isPresent()) {
                log.trace("缓存命中: {}", item.getName());
                return cached.get();
            }
        }

        // 2. 缓存未命中或不使用缓存，执行继承计算
        HttpRequestItem result = applyInheritanceInternal(item);

        // 3. 如果使用缓存，缓存结果
        if (useCache && requestId != null && result != null) {
            cache.put(requestId, result);
        }

        return result;
    }

    /**
     * 内部方法：执行实际的继承计算
     */
    private HttpRequestItem applyInheritanceInternal(HttpRequestItem item) {
        try {
            // 1. 查找请求节点
            Optional<DefaultMutableTreeNode> nodeOpt =
                    treeRepository.findNodeByRequestId(item.getId());

            if (nodeOpt.isEmpty()) {
                log.trace("请求 [{}] 不在 Collections 树中，使用原始配置", item.getName());
                return item;
            }

            // 2. 应用继承（mergeGroupSettings 内部会收集分组链）
            DefaultMutableTreeNode requestNode = nodeOpt.get();
            HttpRequestItem result = GroupInheritanceHelper.mergeGroupSettings(item, requestNode);

            if (result == item) {
                log.trace("请求 [{}] 没有父分组，使用原始配置", item.getName());
            } else {
                log.debug("为请求 [{}] 应用分组继承", item.getName());
            }

            return result;

        } catch (Exception e) {
            log.debug("应用继承时发生异常（将使用原始配置）: {}", e.getMessage());
            return item;
        }
    }

    /**
     * 使所有缓存失效
     * <p>
     * 调用时机：
     * - 修改分组的 auth/headers/scripts
     * - 添加/删除/移动节点
     * - 导入 Collection
     */
    public void invalidateCache() {
        cache.clear();
        log.debug("继承缓存已全局失效");
    }

    /**
     * 使特定请求的缓存失效
     *
     * @param requestId 请求ID
     */
    public void invalidateCache(String requestId) {
        cache.remove(requestId);
        log.debug("请求缓存已失效: {}", requestId);
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public String getCacheStats() {
        return cache.getStats();
    }

    /**
     * 通知树结构已变更
     * <p>
     * 应该在修改树结构后调用，会自动触发缓存失效
     */
    public void notifyTreeChanged() {
        if (treeRepository instanceof DefaultTreeNodeRepository repo) {
            repo.notifyTreeChanged();
        }
    }
}
