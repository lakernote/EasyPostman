package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.service.variable.RequestContext;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
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
     * 应用分组继承规则
     * <p>
     * 缓存策略：只缓存 Group 链（auth/headers/scripts 来源），不缓存整个 item。
     * 这样每次调用都会把最新 item（含最新 url/params）与缓存的 group 链重新合并，
     * 确保用户在 editSubPanel 修改请求后，发送/执行时始终用最新数据。
     *
     * @param item     原始请求项（最新的，包含最新 url/params）
     * @param useCache 是否缓存 Group 链
     * @return 应用了继承后的请求项
     */
    public HttpRequestItem applyInheritance(HttpRequestItem item, boolean useCache) {
        if (item == null) {
            return null;
        }

        String requestId = item.getId();

        // requestId 为空：不在 Collections 里，直接返回
        if (requestId == null || requestId.trim().isEmpty()) {
            return applyInheritanceInternal(item);
        }

        if (!useCache) {
            return applyInheritanceInternal(item);
        }

        // 获取（或计算并缓存）该请求对应的 Group 链
        List<RequestGroup> groupChain = cache.computeIfAbsent(requestId, id -> {
            Optional<DefaultMutableTreeNode> nodeOpt = treeRepository.findNodeByRequestId(id);
            if (nodeOpt.isEmpty()) {
                return List.of(); // 不在 Collections 树中，空 group 链
            }
            DefaultMutableTreeNode requestNode = nodeOpt.get();
            // 设置全局上下文（供分组变量服务使用）
            RequestContext.setCurrentRequestNode(requestNode);
            return GroupInheritanceHelper.collectGroupChain(requestNode);
        });

        // 用最新的 item + 缓存的 group 链合并（每次都重新合并，保证 url/params 最新）
        if (groupChain.isEmpty()) {
            log.trace("请求 [{}] 不在 Collections 树中或无父分组，使用原始配置", item.getName());
            return item;
        }

        // 需要设置 RequestContext（后续变量解析依赖）
        Optional<DefaultMutableTreeNode> nodeOpt = treeRepository.findNodeByRequestId(requestId);
        nodeOpt.ifPresent(RequestContext::setCurrentRequestNode);

        return GroupInheritanceHelper.mergeGroupSettingsWithChain(item, groupChain);
    }

    /**
     * 内部方法：不使用缓存，直接从树中查找并合并
     */
    private HttpRequestItem applyInheritanceInternal(HttpRequestItem item) {
        try {
            Optional<DefaultMutableTreeNode> nodeOpt =
                    treeRepository.findNodeByRequestId(item.getId());

            if (nodeOpt.isEmpty()) {
                log.trace("请求 [{}] 不在 Collections 树中，使用原始配置", item.getName());
                return item;
            }

            DefaultMutableTreeNode requestNode = nodeOpt.get();
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
