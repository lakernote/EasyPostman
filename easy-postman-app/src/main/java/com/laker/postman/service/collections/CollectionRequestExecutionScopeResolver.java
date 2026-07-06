package com.laker.postman.service.collections;

import com.laker.postman.panel.collections.tree.adapter.SwingCollectionInheritanceAdapter;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.variable.RequestExecutionContext;
import com.laker.postman.service.variable.RequestExecutionScope;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class CollectionRequestExecutionScopeResolver {

    private static final ActiveCollectionTreeNodeRepository REQUEST_NODE_REPOSITORY =
            new ActiveCollectionTreeNodeRepository();

    public Optional<RequestExecutionScope> resolveCurrentScope(HttpRequestItem request) {
        return request == null ? Optional.empty() : resolveCurrentScope(request.getId());
    }

    public Optional<RequestExecutionScope> resolveCurrentScope(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }
        return REQUEST_NODE_REPOSITORY.findNodeByRequestId(requestId)
                .map(SwingCollectionInheritanceAdapter::getMergedGroupVariables)
                .map(RequestExecutionScope::fromVariables);
    }

    public boolean syncCurrentScope(HttpRequestItem request) {
        return resolveCurrentScope(request)
                .map(CollectionRequestExecutionScopeResolver::setCurrentScope)
                .orElse(false);
    }

    public boolean syncCurrentScope(String requestId) {
        return resolveCurrentScope(requestId)
                .map(CollectionRequestExecutionScopeResolver::setCurrentScope)
                .orElse(false);
    }

    public void syncCurrentScopeOrEmpty(String requestId) {
        RequestExecutionContext.setCurrentScope(resolveCurrentScope(requestId)
                .orElseGet(RequestExecutionScope::empty));
    }

    private boolean setCurrentScope(RequestExecutionScope scope) {
        RequestExecutionContext.setCurrentScope(scope);
        return true;
    }
}
