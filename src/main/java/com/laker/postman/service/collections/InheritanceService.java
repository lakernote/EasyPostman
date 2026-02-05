package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.variable.RequestContext;
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
     * 应用分组继承规则（可选是否使用缓存）
     * <p>
     * 用于处理未保存的请求（如 UI 中修改但未保存）
     *
     * @param item     原始请求项
     * @param useCache 是否使用缓存
     * @return 应用了继承后的请求项（新对象），如果不需要继承则返回原对象
     */
    public HttpRequestItem applyInheritance(HttpRequestItem item, boolean useCache) {
        if (item == null) {
            return null;
        }

        String requestId = item.getId();

        // 如果不使用缓存或 requestId 为空，直接计算
        if (!useCache || requestId == null) {
            return applyInheritanceInternal(item);
        }

        // 使用缓存：原子性地获取或计算并缓存结果（线程安全）
        // computeIfAbsent 保证只有一个线程执行计算，其他线程等待并获取结果
        return cache.computeIfAbsent(requestId, id -> applyInheritanceInternal(item));
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

            // 设置当前请求节点到全局上下文，供分组变量服务使用
            // 注意：不在此处清除，由调用方（PreparedRequestBuilder）负责清除
            RequestContext.setCurrentRequestNode(requestNode);

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

}
