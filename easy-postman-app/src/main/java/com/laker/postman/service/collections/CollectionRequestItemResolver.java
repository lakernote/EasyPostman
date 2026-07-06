package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@UtilityClass
public class CollectionRequestItemResolver {

    private static final ActiveCollectionTreeNodeRepository REQUEST_NODE_REPOSITORY =
            new ActiveCollectionTreeNodeRepository();

    public Optional<HttpRequestItem> resolveCurrentRequest(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return REQUEST_NODE_REPOSITORY.findNodeByRequestId(requestId)
                    .flatMap(CollectionTreeNodes::request)
                    .map(CollectionRequestItemResolver::copyRequestItem);
        } catch (Exception e) {
            log.error("Failed to resolve collection request by ID {}: {}", requestId, e.getMessage());
            return Optional.empty();
        }
    }

    private HttpRequestItem copyRequestItem(HttpRequestItem original) {
        try {
            return JsonUtil.deepCopy(original, HttpRequestItem.class);
        } catch (Exception e) {
            log.error("Failed to deep copy collection request item: {}", e.getMessage());
            return original;
        }
    }
}
