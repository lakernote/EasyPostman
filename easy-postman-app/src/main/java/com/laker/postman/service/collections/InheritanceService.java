package com.laker.postman.service.collections;

import com.laker.postman.collection.CollectionInheritance;
import com.laker.postman.collection.model.CollectionRequestContext;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.variable.RequestExecutionContext;
import com.laker.postman.service.variable.RequestExecutionScope;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Collection 继承应用服务。
 *
 * <p>负责按请求 ID 找到父分组链，刷新本次请求的执行变量作用域，并把 collection-core
 * 的继承规则应用到最新的请求草稿上。</p>
 *
 * @author laker
 * @since 4.3.22
 */
@Slf4j
public class InheritanceService {

    private final CollectionRequestRepository requestRepository;
    private final InheritanceCache cache;

    /**
     * 创建继承服务（使用默认实现）
     */
    public InheritanceService() {
        this(new ActiveCollectionRequestRepository(), new InheritanceCache());
    }

    /**
     * 创建继承服务（依赖注入，便于测试）
     *
     * @param requestRepository 请求仓库
     * @param cache             缓存管理器
     */
    public InheritanceService(CollectionRequestRepository requestRepository, InheritanceCache cache) {
        this.requestRepository = requestRepository;
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

        try {
            List<RequestGroup> groupChain = resolveGroupChain(item.getId(), useCache);
            refreshExecutionScope(groupChain);
            if (groupChain.isEmpty()) {
                log.trace("请求 [{}] 不在 Collections 树中或无父分组，使用原始配置", item.getName());
                return item;
            }

            log.debug("为请求 [{}] 应用分组继承", item.getName());
            return CollectionInheritance.apply(item, groupChain);
        } catch (Exception e) {
            log.debug("应用继承时发生异常（将使用原始配置）: {}", e.getMessage());
            return item;
        }
    }

    private List<RequestGroup> resolveGroupChain(String requestId, boolean useCache) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return List.of();
        }
        if (!useCache) {
            return findGroupChain(requestId);
        }
        return cache.computeIfAbsent(requestId, this::findGroupChain);
    }

    private List<RequestGroup> findGroupChain(String requestId) {
        return requestRepository.findRequestContextById(requestId)
                .map(CollectionRequestContext::getGroupChain)
                .orElseGet(List::of);
    }

    private void refreshExecutionScope(List<RequestGroup> groupChain) {
        RequestExecutionContext.setCurrentScope(RequestExecutionScope.fromVariables(
                CollectionInheritance.mergeGroupVariables(groupChain)
        ));
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
