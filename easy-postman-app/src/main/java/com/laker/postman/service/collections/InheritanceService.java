package com.laker.postman.service.collections;

import com.laker.postman.collection.CollectionInheritance;
import com.laker.postman.collection.model.CollectionRequestContext;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.variable.RequestExecutionContext;
import com.laker.postman.service.variable.RequestExecutionScope;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class InheritanceService {

    private final CollectionRequestRepository requestRepository;

    /**
     * 创建继承服务（使用默认实现）
     */
    public InheritanceService() {
        this(new ActiveCollectionRequestRepository());
    }

    /**
     * 应用分组继承规则
     *
     * @param item 原始请求项（最新的，包含最新 url/params）
     * @return 应用了继承后的请求项
     */
    public HttpRequestItem applyInheritance(HttpRequestItem item) {
        if (item == null) {
            return null;
        }

        try {
            List<RequestGroup> groupChain = findGroupChain(item.getId());
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

    private List<RequestGroup> findGroupChain(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return List.of();
        }
        return requestRepository.findRequestContextById(requestId)
                .map(CollectionRequestContext::getGroupChain)
                .orElseGet(List::of);
    }

    private void refreshExecutionScope(List<RequestGroup> groupChain) {
        RequestExecutionContext.setCurrentScope(RequestExecutionScope.fromVariables(
                CollectionInheritance.mergeGroupVariables(groupChain)
        ));
    }
}
